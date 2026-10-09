package com.gnimah.backend.service;

import com.gnimah.backend.dto.client.ClientRequest;
import com.gnimah.backend.entity.Client;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.repository.ClientRepository;
import com.gnimah.backend.repository.EvenementRepository;
import com.gnimah.backend.repository.ReservationRepository;
import com.gnimah.backend.repository.SejourRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private SejourRepository sejourRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private EvenementRepository evenementRepository;

    @InjectMocks
    private ClientService clientService;

    private ClientRequest request() {
        ClientRequest r = new ClientRequest();
        r.setCivilite("M.");
        r.setNom("Yao");
        r.setPrenom("Konan");
        r.setTelephone("+225 07 11 22 33");
        r.setTypePiece("CNI");
        r.setNumeroPiece("CI0011223");
        return r;
    }

    private Client existant() {
        return Client.builder().id(9L).civilite("M.").nom("YAO").prenom("Konan")
                .telephone("+225 07 11 22 33").typePiece("CNI").numeroPiece("CI0011223").build();
    }

    @Test
    void create_avecUnePieceDejaEnregistree_annonceLeDoublonEnClair() {
        when(clientRepository.findFirstByTypePieceAndNumeroPiece("CNI", "CI0011223"))
                .thenReturn(Optional.of(existant()));

        assertThatThrownBy(() -> clientService.create(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pièce d'identité")
                .hasMessageContaining("YAO");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void update_duMemeClient_neSeHeurtePasASaPropreFiche() {
        Client client = existant();
        when(clientRepository.findById(9L)).thenReturn(Optional.of(client));
        when(clientRepository.findFirstByTypePieceAndNumeroPiece("CNI", "CI0011223")).thenReturn(Optional.of(client));
        when(clientRepository.findFirstByTelephoneOrderByIdDesc("+225 07 11 22 33")).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = clientService.update(9L, request());

        assertThat(response.getNom()).isEqualTo("YAO");
    }

    @Test
    void delete_dUnClientAvecHistorique_estRefuse() {
        when(clientRepository.findById(9L)).thenReturn(Optional.of(existant()));
        when(sejourRepository.existsByClientId(9L)).thenReturn(true);

        assertThatThrownBy(() -> clientService.delete(9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("historique");
        verify(clientRepository, never()).delete(any());
    }

    @Test
    void search_compareAussiLeTelephoneSansEspaces() {
        when(clientRepository.searchClients(eq("05 44"), eq("0544"), any()))
                .thenReturn(new PageImpl<>(List.of(existant())));

        var page = clientService.search("05 44", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }
}
