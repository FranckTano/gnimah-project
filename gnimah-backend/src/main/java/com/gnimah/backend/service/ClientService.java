package com.gnimah.backend.service;

import com.gnimah.backend.dto.client.ClientRequest;
import com.gnimah.backend.dto.client.ClientResponse;
import com.gnimah.backend.entity.Client;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.ClientRepository;
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

    @Transactional
    public ClientResponse create(ClientRequest request) {
        if (clientRepository.findByTelephone(request.getTelephone()).isPresent()) {
            throw new BusinessException("Un client avec ce numéro de téléphone existe déjà");
        }
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
        return clientRepository.searchClients(search.trim(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ClientResponse> findByTelephone(String telephone) {
        return clientRepository.findByTelephone(telephone).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ClientResponse> findByNumeroPiece(String numeroPiece) {
        return clientRepository.findByNumeroPiece(numeroPiece).map(this::toResponse);
    }

    @Transactional
    public void delete(Long id) {
        Client client = findById(id);
        clientRepository.delete(client);
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
