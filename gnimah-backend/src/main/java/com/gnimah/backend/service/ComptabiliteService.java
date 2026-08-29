package com.gnimah.backend.service;

import com.gnimah.backend.dto.comptabilite.LigneRepartitionResponse;
import com.gnimah.backend.dto.comptabilite.SyntheseComptableResponse;
import com.gnimah.backend.entity.enums.FamilleDepense;
import com.gnimah.backend.entity.enums.SensMouvement;
import com.gnimah.backend.entity.enums.TypeMouvementCaisse;
import com.gnimah.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compte de résultat simplifié et position de trésorerie de la résidence.
 *
 * Les recettes sont ventilées par origine — hébergement, restauration,
 * événements — et volontairement séparées des encaissements : une nuitée
 * facturée est une recette même si le client n'a pas encore payé, alors que la
 * trésorerie ne bouge qu'au moment de l'encaissement. Les deux lectures sont
 * fournies côte à côte, car l'exploitant a besoin des deux : la première dit si
 * l'affaire est rentable, la seconde s'il peut payer ses fournisseurs demain.
 */
@Service
@RequiredArgsConstructor
public class ComptabiliteService {

    private final SejourRepository sejourRepository;
    private final DepenseRepository depenseRepository;
    private final CommandeRestaurantRepository commandeRepository;
    private final EvenementRepository evenementRepository;
    private final MouvementCaisseRepository mouvementRepository;
    private final CaisseRepository caisseRepository;
    private final VersementRepository versementRepository;
    private final ArticleStockRepository articleStockRepository;
    private final IncidentChambreRepository incidentRepository;

    @Transactional(readOnly = true)
    public SyntheseComptableResponse synthese(LocalDate debut, LocalDate fin) {
        LocalDateTime debutDT = debut.atStartOfDay();
        LocalDateTime finDT = fin.plusDays(1).atStartOfDay();

        // ---- Recettes ----
        BigDecimal hebergement = sejourRepository.sumRecetteHebergement(debutDT, finDT);
        BigDecimal restauration = commandeRepository.sumChiffreAffairesPeriode(debutDT, finDT);
        BigDecimal evenements = evenementRepository.sumMontantPeriode(debutDT, finDT);

        // Les autres recettes se lisent sur leur type propre, et non par
        // soustraction du total des entrées de caisse : ce total inclut le fond
        // de caisse et les remboursements annulés, qui ne sont pas des recettes
        // et gonfleraient artificiellement le chiffre d'affaires.
        BigDecimal autres = mouvementRepository.sumParTypePeriode(
                SensMouvement.ENTREE, TypeMouvementCaisse.AUTRE_RECETTE, debutDT, finDT);

        BigDecimal totalRecettes = hebergement.add(restauration).add(evenements).add(autres);

        // ---- Charges ----
        BigDecimal totalDepenses = depenseRepository.sumDepensesPeriode(debut, fin);
        BigDecimal depensesPayees = depenseRepository.sumDepensesPayeesPeriode(debut, fin);

        BigDecimal resultat = totalRecettes.subtract(totalDepenses);
        double tauxMarge = totalRecettes.compareTo(BigDecimal.ZERO) > 0
                ? resultat.multiply(BigDecimal.valueOf(100))
                    .divide(totalRecettes, 1, RoundingMode.HALF_UP).doubleValue()
                : 0d;

        // ---- Trésorerie ----
        BigDecimal entrees = mouvementRepository.sumBySensPeriode(SensMouvement.ENTREE, debutDT, finDT);
        BigDecimal sorties = mouvementRepository.sumBySensPeriode(SensMouvement.SORTIE, debutDT, finDT);
        BigDecimal totalVerse = versementRepository.sumPeriode(debut, fin);

        return SyntheseComptableResponse.builder()
                .debut(debut)
                .fin(fin)
                .recetteHebergement(hebergement)
                .recetteRestauration(restauration)
                .recetteEvenements(evenements)
                .autresRecettes(autres)
                .totalRecettes(totalRecettes)
                .totalDepenses(totalDepenses)
                .depensesPayees(depensesPayees)
                .depensesEnAttente(totalDepenses.subtract(depensesPayees).max(BigDecimal.ZERO))
                .resultat(resultat)
                .tauxMarge(tauxMarge)
                .soldeCaisses(caisseRepository.sumSoldes())
                .entreesCaisse(entrees)
                .sortiesCaisse(sorties)
                .totalVerse(totalVerse)
                .resteAVerser(caisseRepository.sumSoldes())
                .creancesClients(sejourRepository.sumCreancesEnCours())
                .valeurStock(articleStockRepository.valeurTotaleStock())
                .coutIncidents(incidentRepository.sumCoutPeriode(debutDT, finDT))
                .depensesParFamille(depensesParFamille(debut, fin, totalDepenses))
                .recettesParSource(recettesParSource(hebergement, restauration, evenements, autres, totalRecettes))
                .topFournisseurs(topFournisseurs(debut, fin, totalDepenses))
                .evolutionJournaliere(evolutionJournaliere(debut, fin))
                .build();
    }

    // ------------------------------------------------------------------
    // Ventilations
    // ------------------------------------------------------------------

    private List<LigneRepartitionResponse> depensesParFamille(LocalDate debut, LocalDate fin, BigDecimal total) {
        List<LigneRepartitionResponse> lignes = new ArrayList<>();
        for (Object[] row : depenseRepository.repartitionParFamille(debut, fin)) {
            FamilleDepense famille = (FamilleDepense) row[0];
            BigDecimal montant = (BigDecimal) row[2];
            lignes.add(LigneRepartitionResponse.builder()
                    .cle(famille.name())
                    .libelle(DepenseService.libelleFamille(famille))
                    .nombre((Long) row[1])
                    .montant(montant)
                    .part(part(montant, total))
                    .build());
        }
        lignes.sort((a, b) -> b.getMontant().compareTo(a.getMontant()));
        return lignes;
    }

    private List<LigneRepartitionResponse> recettesParSource(BigDecimal hebergement, BigDecimal restauration,
                                                             BigDecimal evenements, BigDecimal autres,
                                                             BigDecimal total) {
        List<LigneRepartitionResponse> lignes = new ArrayList<>();
        lignes.add(ligne("HEBERGEMENT", "Hébergement", hebergement, total));
        lignes.add(ligne("RESTAURATION", "Restauration", restauration, total));
        lignes.add(ligne("EVENEMENTS", "Événements", evenements, total));
        lignes.add(ligne("AUTRES", "Autres recettes", autres, total));
        lignes.removeIf(l -> l.getMontant().compareTo(BigDecimal.ZERO) == 0);
        lignes.sort((a, b) -> b.getMontant().compareTo(a.getMontant()));
        return lignes;
    }

    private List<LigneRepartitionResponse> topFournisseurs(LocalDate debut, LocalDate fin, BigDecimal total) {
        List<LigneRepartitionResponse> lignes = new ArrayList<>();
        for (Object[] row : depenseRepository.repartitionParFournisseur(debut, fin)) {
            BigDecimal montant = (BigDecimal) row[2];
            lignes.add(LigneRepartitionResponse.builder()
                    .cle((String) row[0])
                    .libelle((String) row[0])
                    .nombre((Long) row[1])
                    .montant(montant)
                    .part(part(montant, total))
                    .build());
            if (lignes.size() >= 8) break;
        }
        return lignes;
    }

    /**
     * Courbe recettes / dépenses jour par jour. La période est bornée à 92 jours
     * : au-delà, la requête produirait des centaines d'allers-retours en base
     * pour un graphique devenu illisible.
     */
    private List<Map<String, Object>> evolutionJournaliere(LocalDate debut, LocalDate fin) {
        List<Map<String, Object>> resultat = new ArrayList<>();
        LocalDate borne = debut.plusDays(91);
        LocalDate finEffective = fin.isAfter(borne) ? borne : fin;

        for (LocalDate jour = debut; !jour.isAfter(finEffective); jour = jour.plusDays(1)) {
            LocalDateTime d = jour.atStartOfDay();
            LocalDateTime f = jour.plusDays(1).atStartOfDay();

            BigDecimal recettes = sejourRepository.sumRecetteHebergement(d, f)
                    .add(commandeRepository.sumChiffreAffairesPeriode(d, f));
            BigDecimal depenses = depenseRepository.sumDepensesPeriode(jour, jour);

            Map<String, Object> entree = new HashMap<>();
            entree.put("date", jour.toString());
            entree.put("recettes", recettes);
            entree.put("depenses", depenses);
            entree.put("resultat", recettes.subtract(depenses));
            resultat.add(entree);
        }
        return resultat;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private LigneRepartitionResponse ligne(String cle, String libelle, BigDecimal montant, BigDecimal total) {
        return LigneRepartitionResponse.builder()
                .cle(cle).libelle(libelle).nombre(0)
                .montant(montant).part(part(montant, total))
                .build();
    }

    private double part(BigDecimal montant, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0 || montant == null) return 0d;
        return montant.multiply(BigDecimal.valueOf(100))
                .divide(total, 1, RoundingMode.HALF_UP).doubleValue();
    }
}
