package com.gnimah.backend.service;

import com.gnimah.backend.dto.media.MediaResidenceRequest;
import com.gnimah.backend.dto.media.MediaResidenceResponse;
import com.gnimah.backend.entity.MediaResidence;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.MediaResidenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Galerie « La Résidence », administrable depuis l'application.
 *
 * Le catalogue était auparavant figé dans le frontend : ajouter une photo
 * demandait une modification de code et un redéploiement. Il vit désormais en
 * base, et l'administrateur le pilote lui-même.
 */
@Service
@RequiredArgsConstructor
public class MediaResidenceService {

    private final MediaResidenceRepository repository;
    private final CloudinaryService cloudinary;

    @Transactional(readOnly = true)
    public List<MediaResidenceResponse> findVisibles() {
        return repository.findByActifTrueOrderByPositionAscIdAsc().stream().map(this::versDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MediaResidenceResponse> findAll() {
        return repository.findAllByOrderByPositionAscIdAsc().stream().map(this::versDto).toList();
    }

    /** Vrai si le serveur peut signer l'envoi lui-même (voie préférée). */
    public boolean hebergementDisponible() {
        return cloudinary.estConfigure();
    }

    /** Valeurs publiques dont le navigateur a besoin pour téléverser lui-même. */
    public String cloudName() {
        return cloudinary.cloudName();
    }

    public String dossier() {
        return cloudinary.dossier();
    }

    public String uploadPreset() {
        return cloudinary.uploadPreset();
    }

    /** Envoi d'un fichier : il part sur Cloudinary, seules les métadonnées restent ici. */
    @Transactional
    public MediaResidenceResponse televerser(MultipartFile fichier, String titre, String categorie) {
        CloudinaryService.FichierDistant distant = cloudinary.televerserImage(fichier);

        MediaResidence media = MediaResidence.builder()
                .titre(titreOuDefaut(titre, fichier))
                .categorie(categorieOuDefaut(categorie))
                .url(distant.url())
                .urlVignette(distant.urlVignette())
                .publicId(distant.publicId())
                .position(repository.prochainePosition())
                .actif(true)
                .misEnAvant(false)
                .build();
        return versDto(repository.save(media));
    }

    /**
     * Enregistre une image que le navigateur vient de téléverser directement sur
     * Cloudinary (préréglage non signé), quand le serveur n'a pas d'identifiants
     * pour le faire lui-même.
     *
     * Le {@code publicId} est conservé pour que la suppression efface aussi le
     * fichier distant. Il est validé : sans cela, un client pourrait faire
     * enregistrer — puis supprimer — n'importe quel objet du compte Cloudinary,
     * y compris les photos de la vitrine.
     */
    @Transactional
    public MediaResidenceResponse enregistrerTeleversement(MediaResidenceRequest request) {
        if (vide(request.getUrl()) || vide(request.getPublicId())) {
            throw new BusinessException("Envoi incomplet : l'image n'a pas pu être enregistrée.");
        }
        cloudinary.validerRessourceDistante(request.getUrl(), request.getPublicId());

        if (repository.existsByPublicId(request.getPublicId())) {
            throw new BusinessException("Cette image figure déjà dans la galerie.");
        }

        MediaResidence media = MediaResidence.builder()
                .titre(request.getTitre())
                .categorie(categorieOuDefaut(request.getCategorie()))
                .url(request.getUrl().trim())
                .urlVignette(vide(request.getUrlVignette()) ? null : request.getUrlVignette().trim())
                .publicId(request.getPublicId().trim())
                .position(repository.prochainePosition())
                .actif(true)
                .misEnAvant(false)
                .build();
        return versDto(repository.save(media));
    }

    /** Référencement d'une image déjà en ligne, sans passer par notre hébergement. */
    @Transactional
    public MediaResidenceResponse creerDepuisUrl(MediaResidenceRequest request) {
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            throw new BusinessException("Indiquez l'adresse de l'image, ou envoyez un fichier.");
        }
        MediaResidence media = MediaResidence.builder()
                .titre(request.getTitre())
                .categorie(categorieOuDefaut(request.getCategorie()))
                .url(request.getUrl().trim())
                .urlVignette(vide(request.getUrlVignette()) ? null : request.getUrlVignette().trim())
                .position(request.getPosition() != null ? request.getPosition() : repository.prochainePosition())
                .actif(request.getActif() == null || request.getActif())
                .misEnAvant(false)
                .build();
        MediaResidence sauve = repository.save(media);
        if (Boolean.TRUE.equals(request.getMisEnAvant())) appliquerMiseEnAvant(sauve);
        return versDto(sauve);
    }

    @Transactional
    public MediaResidenceResponse update(Long id, MediaResidenceRequest request) {
        MediaResidence media = charger(id);
        media.setTitre(request.getTitre());
        media.setCategorie(categorieOuDefaut(request.getCategorie()));
        if (!vide(request.getUrl())) media.setUrl(request.getUrl().trim());
        if (request.getUrlVignette() != null) {
            media.setUrlVignette(vide(request.getUrlVignette()) ? null : request.getUrlVignette().trim());
        }
        if (request.getPosition() != null) media.setPosition(request.getPosition());
        if (request.getActif() != null) media.setActif(request.getActif());

        MediaResidence sauve = repository.save(media);
        if (Boolean.TRUE.equals(request.getMisEnAvant())) {
            appliquerMiseEnAvant(sauve);
        } else if (Boolean.FALSE.equals(request.getMisEnAvant())) {
            sauve.setMisEnAvant(false);
        }
        return versDto(sauve);
    }

    /**
     * Déplace un visuel d'un cran. On réécrit les positions de toute la liste :
     * les valeurs héritées peuvent comporter des trous ou des doublons, et un
     * simple échange laisserait alors l'ordre inchangé à l'écran.
     */
    @Transactional
    public List<MediaResidenceResponse> deplacer(Long id, int sens) {
        List<MediaResidence> liste = repository.findAllByOrderByPositionAscIdAsc();
        int index = -1;
        for (int i = 0; i < liste.size(); i++) {
            if (liste.get(i).getId().equals(id)) { index = i; break; }
        }
        if (index < 0) throw new ResourceNotFoundException("Média introuvable : " + id);

        int cible = index + (sens < 0 ? -1 : 1);
        if (cible >= 0 && cible < liste.size()) {
            MediaResidence tmp = liste.get(index);
            liste.set(index, liste.get(cible));
            liste.set(cible, tmp);
        }
        for (int i = 0; i < liste.size(); i++) liste.get(i).setPosition(i);
        repository.saveAll(liste);
        return liste.stream().map(this::versDto).toList();
    }

    /**
     * Supprime la ligne et, si le fichier était hébergé par nous, l'objet distant :
     * sans cela le quota Cloudinary se remplirait de visuels que plus rien
     * n'affiche.
     */
    @Transactional
    public void delete(Long id) {
        MediaResidence media = charger(id);
        repository.delete(media);
        cloudinary.supprimer(media.getPublicId());
    }

    private void appliquerMiseEnAvant(MediaResidence media) {
        repository.retirerMiseEnAvantSauf(media.getId());
        media.setMisEnAvant(true);
        repository.save(media);
    }

    private MediaResidence charger(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Média introuvable : " + id));
    }

    private String titreOuDefaut(String titre, MultipartFile fichier) {
        if (!vide(titre)) return titre.trim();
        String nom = fichier.getOriginalFilename();
        if (vide(nom)) return "Photo de la résidence";
        int point = nom.lastIndexOf('.');
        return point > 0 ? nom.substring(0, point) : nom;
    }

    private String categorieOuDefaut(String categorie) {
        return vide(categorie) ? "Chambres" : categorie.trim();
    }

    private boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private MediaResidenceResponse versDto(MediaResidence m) {
        return MediaResidenceResponse.builder()
                .id(m.getId())
                .titre(m.getTitre())
                .categorie(m.getCategorie())
                .url(m.getUrl())
                .urlVignette(m.getUrlVignette() != null ? m.getUrlVignette() : m.getUrl())
                .publicId(m.getPublicId())
                .position(m.getPosition())
                .actif(m.isActif())
                .misEnAvant(m.isMisEnAvant())
                .heberge(m.getPublicId() != null)
                .createdAt(m.getCreatedAt())
                .build();
    }
}
