package com.gnimah.backend.service;

import com.gnimah.backend.dto.media.MediaResidenceRequest;
import com.gnimah.backend.dto.media.MediaResidenceResponse;
import com.gnimah.backend.entity.MediaResidence;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.MediaResidenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaResidenceServiceTest {

    @Mock private MediaResidenceRepository repository;
    @Mock private CloudinaryService cloudinary;

    @InjectMocks private MediaResidenceService service;

    private MediaResidence media(long id, String titre, int position) {
        return MediaResidence.builder()
                .id(id).titre(titre).categorie("Chambres")
                .url("https://cdn.test/" + id + ".jpg")
                .position(position).actif(true).misEnAvant(false)
                .build();
    }

    @Test
    void deplacer_renumerotesansTrou_memeAvecDesPositionsHeritees() {
        // Positions volontairement trouées : un simple échange de valeurs
        // laisserait l'ordre affiché inchangé.
        List<MediaResidence> liste = new ArrayList<>(List.of(
                media(1, "A", 0), media(2, "B", 5), media(3, "C", 5), media(4, "D", 42)));
        when(repository.findAllByOrderByPositionAscIdAsc()).thenReturn(liste);
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<MediaResidenceResponse> apres = service.deplacer(3L, -1);

        assertThat(apres).extracting(MediaResidenceResponse::getTitre)
                .containsExactly("A", "C", "B", "D");
        assertThat(apres).extracting(MediaResidenceResponse::getPosition)
                .containsExactly(0, 1, 2, 3);
    }

    @Test
    void deplacer_auxExtremites_neSortPasDeLaListe() {
        List<MediaResidence> liste = new ArrayList<>(List.of(media(1, "A", 0), media(2, "B", 1)));
        when(repository.findAllByOrderByPositionAscIdAsc()).thenReturn(liste);
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.deplacer(1L, -1)).extracting(MediaResidenceResponse::getTitre)
                .containsExactly("A", "B");
        assertThat(service.deplacer(2L, 1)).extracting(MediaResidenceResponse::getTitre)
                .containsExactly("A", "B");
    }

    @Test
    void deplacer_idInconnu_estSignale() {
        when(repository.findAllByOrderByPositionAscIdAsc()).thenReturn(new ArrayList<>());
        assertThatThrownBy(() -> service.deplacer(99L, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void miseEnAvant_retireLaPrecedente() {
        MediaResidence m = media(2, "Vedette", 1);
        when(repository.findById(2L)).thenReturn(Optional.of(m));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MediaResidenceRequest req = new MediaResidenceRequest();
        req.setTitre("Vedette");
        req.setCategorie("Chambres");
        req.setMisEnAvant(true);

        MediaResidenceResponse res = service.update(2L, req);

        assertThat(res.isMisEnAvant()).isTrue();
        // Un seul visuel peut porter les bandeaux : les autres sont dégradés.
        verify(repository).retirerMiseEnAvantSauf(2L);
    }

    @Test
    void suppression_effaceAussiLeFichierDistant() {
        MediaResidence m = media(1, "A", 0);
        m.setPublicId("gnimah/galerie/photo-1");
        when(repository.findById(1L)).thenReturn(Optional.of(m));

        service.delete(1L);

        verify(repository).delete(m);
        // Sans cela, le quota Cloudinary se remplirait de visuels que plus rien
        // n'affiche.
        verify(cloudinary).supprimer("gnimah/galerie/photo-1");
    }

    @Test
    void suppression_dUneUrlExterne_ne_toucheRienADistance() {
        MediaResidence m = media(1, "Image d'un tiers", 0);
        m.setPublicId(null);
        when(repository.findById(1L)).thenReturn(Optional.of(m));

        service.delete(1L);

        verify(repository).delete(m);
        verify(cloudinary).supprimer(null);
    }

    @Test
    void creerDepuisUrl_sansAdresse_estRefuse() {
        MediaResidenceRequest req = new MediaResidenceRequest();
        req.setTitre("Sans adresse");
        req.setCategorie("Chambres");

        assertThatThrownBy(() -> service.creerDepuisUrl(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("adresse");
    }

    @Test
    void creerDepuisUrl_poseLeVisuelEnFinDeGalerie() {
        when(repository.prochainePosition()).thenReturn(7);
        when(repository.save(any())).thenAnswer(inv -> {
            MediaResidence m = inv.getArgument(0);
            m.setId(9L);
            return m;
        });

        MediaResidenceRequest req = new MediaResidenceRequest();
        req.setTitre("Terrasse");
        req.setCategorie("Extérieur");
        req.setUrl("  https://cdn.test/terrasse.jpg  ");

        MediaResidenceResponse res = service.creerDepuisUrl(req);

        assertThat(res.getPosition()).isEqualTo(7);
        // L'adresse est nettoyée : un espace résiduel casserait le chargement.
        assertThat(res.getUrl()).isEqualTo("https://cdn.test/terrasse.jpg");
        assertThat(res.isHeberge()).isFalse();
        // Sans vignette dédiée, l'affichage retombe sur l'image principale.
        assertThat(res.getUrlVignette()).isEqualTo("https://cdn.test/terrasse.jpg");
    }

    @Test
    void televerser_sansHebergement_remonteLErreurDuService() {
        when(cloudinary.televerserImage(any()))
                .thenThrow(new BusinessException("L'hébergement d'images n'est pas configuré"));

        assertThatThrownBy(() -> service.televerser(null, "Titre", "Chambres"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("configuré");
        verify(repository, never()).save(any());
    }

    @Test
    void galerieVisible_neRemonteQueLesVisuelsActifs() {
        when(repository.findByActifTrueOrderByPositionAscIdAsc())
                .thenReturn(List.of(media(1, "A", 0), media(2, "B", 1)));

        assertThat(service.findVisibles()).hasSize(2);
        verify(repository, never()).findAllByOrderByPositionAscIdAsc();
    }
}
