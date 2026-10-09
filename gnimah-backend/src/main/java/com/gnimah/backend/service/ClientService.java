package com.gnimah.backend.service;

import com.gnimah.backend.dto.client.ClientRequest;
import com.gnimah.backend.dto.client.ClientResponse;
import com.gnimah.backend.entity.Client;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.ClientRepository;
import com.gnimah.backend.repository.EvenementRepository;
import com.gnimah.backend.repository.ReservationRepository;
import com.gnimah.backend.repository.SejourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final SejourRepository sejourRepository;
    private final ReservationRepository reservationRepository;
    private final EvenementRepository evenementRepository;

    @Transactional
    public ClientResponse create(ClientRequest request) {
        verifierUnicite(request, null);
        Client client = Client.builder()
                .civilite(request.getCivilite())
                .nom(request.getNom().toUpperCase())
                .prenom(request.getPrenom())
                .telephone(request.getTelephone())
                .email(request.getEmail())
                .typePiece(request.getTypePiece())
                .numeroPiece(request.getNumeroPiece())
                .nationalite(request.getNationalite() != null ? request.getNationalite() : "Ivoirienne")
                .adresse(request.getAdresse())
                .build();
        return toResponse(clientRepository.save(client));
    }

    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = findById(id);
        verifierUnicite(request, id);
        client.setCivilite(request.getCivilite());
        client.setNom(request.getNom().toUpperCase());
        client.setPrenom(request.getPrenom());
        client.setTelephone(request.getTelephone());
        client.setEmail(request.getEmail());
        client.setTypePiece(request.getTypePiece());
        client.setNumeroPiece(request.getNumeroPiece());
        if (request.getNationalite() != null) client.setNationalite(request.getNationalite());
        client.setAdresse(request.getAdresse());
        return toResponse(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public ClientResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public Page<ClientResponse> search(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return clientRepository.findAll(pageable).map(this::toResponse);
        }
        String terme = search.trim();
        String compact = terme.replaceAll("\\s+", "");
        return clientRepository.searchClients(terme, compact.isEmpty() ? terme : compact, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ClientResponse> findByTelephone(String telephone) {
        return clientRepository.findFirstByTelephoneOrderByIdDesc(telephone).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ClientResponse> findByNumeroPiece(String numeroPiece) {
        return clientRepository.findFirstByNumeroPieceOrderByIdDesc(numeroPiece).map(this::toResponse);
    }

    /**
     * La fiche d'un client qui a séjourné, réservé ou organisé un événement est
     * conservée : elle porte l'historique des reçus et des paiements.
     */
    @Transactional
    public void delete(Long id) {
        Client client = findById(id);
        if (sejourRepository.existsByClientId(id) || reservationRepository.existsByClientId(id)
                || evenementRepository.existsByClientId(id)) {
            throw new BusinessException("Ce client a un historique (séjours, réservations ou événements) : "
                    + "sa fiche doit être conservée.");
        }
        clientRepository.delete(client);
    }

    /**
     * Une même personne ne doit avoir qu'une fiche : la pièce d'identité est
     * unique en base, le téléphone sert de second garde-fou à la réception.
     * L'échec est annoncé en clair — sans ce contrôle, la contrainte de la base
     * produirait une erreur technique incompréhensible pour l'agent.
     */
    private void verifierUnicite(ClientRequest request, Long idCourant) {
        clientRepository.findFirstByTypePieceAndNumeroPiece(request.getTypePiece(), request.getNumeroPiece())
                .filter(c -> !c.getId().equals(idCourant))
                .ifPresent(c -> {
                    throw new BusinessException("Cette pièce d'identité est déjà enregistrée pour "
                            + c.getNomComplet() + " (" + c.getTelephone() + ")");
                });
        clientRepository.findFirstByTelephoneOrderByIdDesc(request.getTelephone())
                .filter(c -> !c.getId().equals(idCourant))
                .ifPresent(c -> {
                    throw new BusinessException("Ce numéro de téléphone est déjà celui de " + c.getNomComplet());
                });
    }

    public Client findById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
    }

    public ClientResponse toResponse(Client c) {
        return ClientResponse.builder()
                .id(c.getId())
                .civilite(c.getCivilite())
                .nom(c.getNom())
                .prenom(c.getPrenom())
                .nomComplet(c.getNomComplet())
                .telephone(c.getTelephone())
                .email(c.getEmail())
                .typePiece(c.getTypePiece())
                .numeroPiece(c.getNumeroPiece())
                .nationalite(c.getNationalite())
                .adresse(c.getAdresse())
                .nbSejours(c.getNbSejours())
                .clientFidele(c.isClientFidele())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
