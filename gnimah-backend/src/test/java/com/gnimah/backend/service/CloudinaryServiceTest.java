package com.gnimah.backend.service;

import com.gnimah.backend.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validation des ressources téléversées par le navigateur.
 *
 * Quand le serveur n'a pas de clé d'API, le navigateur dépose lui-même le
 * fichier sur Cloudinary et transmet ensuite son identifiant. Sans contrôle, un
 * client pourrait annoncer l'identifiant de n'importe quel objet du compte —
 * puis le faire effacer en retirant la ligne de la galerie.
 */
class CloudinaryServiceTest {

    private CloudinaryService service;

    @BeforeEach
    void init() {
        service = new CloudinaryService();
        ReflectionTestUtils.setField(service, "cloudName", "moncompte");
        ReflectionTestUtils.setField(service, "dossier", "gnimah/galerie");
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "apiSecret", "");
    }

    private static final String OK_URL =
            "https://res.cloudinary.com/moncompte/image/upload/f_auto,q_auto/gnimah/galerie/1-photo";

    @Test
    void accepteUneRessourceDuBonCompteEtDuBonDossier() {
        assertThatCode(() -> service.validerRessourceDistante(OK_URL, "gnimah/galerie/1-photo"))
                .doesNotThrowAnyException();
    }

    @Test
    void refuseUnDossierEtranger() {
        // « gnimah/photos » héberge les visuels de la vitrine : les laisser
        // adopter ici reviendrait à permettre leur suppression.
        assertThatThrownBy(() -> service.validerRessourceDistante(
                "https://res.cloudinary.com/moncompte/image/upload/gnimah/photos/facade-patio",
                "gnimah/photos/facade-patio"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dossier");
    }

    @Test
    void refuseUnDossierQuiSeContentDuMemePrefixe() {
        assertThatThrownBy(() -> service.validerRessourceDistante(
                "https://res.cloudinary.com/moncompte/image/upload/gnimah/galerie-bis/x",
                "gnimah/galerie-bis/x"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void refuseUneRemonteeDeChemin() {
        assertThatThrownBy(() -> service.validerRessourceDistante(
                "https://res.cloudinary.com/moncompte/image/upload/gnimah/galerie/../photos/facade",
                "gnimah/galerie/../photos/facade"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalide");
    }

    @Test
    void refuseUnAutreCompteCloudinary() {
        assertThatThrownBy(() -> service.validerRessourceDistante(
                "https://res.cloudinary.com/quelquun-dautre/image/upload/gnimah/galerie/1-photo",
                "gnimah/galerie/1-photo"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("compte");
    }

    @Test
    void refuseUnHebergeurQuelconque() {
        assertThatThrownBy(() -> service.validerRessourceDistante(
                "https://exemple.test/gnimah/galerie/1-photo", "gnimah/galerie/1-photo"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void refuseUneAdresseQuiNeCorrespondPasAuFichierAnnonce() {
        // Sinon on enregistrerait une image et en supprimerait une autre.
        assertThatThrownBy(() -> service.validerRessourceDistante(
                "https://res.cloudinary.com/moncompte/image/upload/gnimah/galerie/9-autre",
                "gnimah/galerie/1-photo"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("correspond pas");
    }

    @Test
    void sansIdentifiants_leServeurNePretendPasPouvoirSigner() {
        assertThatThrownBy(() -> service.televerserImage(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("configuré");
    }

    @Test
    void supprimer_sansIdentifiants_neJettePas() {
        // Le retrait de la galerie doit aboutir même si le fichier distant ne
        // peut pas être effacé : sinon l'administrateur ne pourrait plus retirer
        // un visuel de sa galerie.
        assertThatCode(() -> service.supprimer("gnimah/galerie/1-photo")).doesNotThrowAnyException();
    }
}
