package com.gnimah.backend.controller;

import com.gnimah.backend.dto.PublicHotelInfoResponse;
import com.gnimah.backend.repository.ChambreRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "Informations publiques")
public class PublicController {

    private final ChambreRepository chambreRepository;

    @GetMapping("/hotel-info")
    @Operation(summary = "Informations publiques affichées sur l'écran de connexion")
    public ResponseEntity<PublicHotelInfoResponse> hotelInfo() {
        return ResponseEntity.ok(PublicHotelInfoResponse.builder()
                .totalChambres(chambreRepository.countByActifTrue())
                .build());
    }
}
