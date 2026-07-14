package com.gnimah.backend.controller;

import com.gnimah.backend.dto.client.ClientRequest;
import com.gnimah.backend.dto.client.ClientResponse;
import com.gnimah.backend.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Gestion des clients")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @Operation(summary = "Créer un client")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        return ResponseEntity.ok(clientService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un client")
    public ResponseEntity<ClientResponse> update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.ok(clientService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un client par ID")
    public ResponseEntity<ClientResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Lister / rechercher les clients")
    public ResponseEntity<Page<ClientResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(clientService.search(search, PageRequest.of(page, size, Sort.by("nom"))));
    }

    @GetMapping("/by-telephone/{telephone}")
    @Operation(summary = "Rechercher par téléphone")
    public ResponseEntity<ClientResponse> findByTelephone(@PathVariable String telephone) {
        Optional<ClientResponse> client = clientService.findByTelephone(telephone);
        return client.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-piece/{numeroPiece}")
    @Operation(summary = "Rechercher par numéro de pièce")
    public ResponseEntity<ClientResponse> findByPiece(@PathVariable String numeroPiece) {
        Optional<ClientResponse> client = clientService.findByNumeroPiece(numeroPiece);
        return client.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un client")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
