package com.gnimah.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "factures")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sejour_id", nullable = false)
    private Sejour sejour;

    @Column(name = "montant_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTotal;

    @Column(name = "montant_paye", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(name = "reste_a_payer", nullable = false, precision = 10, scale = 2)
    private BigDecimal resteAPayer = BigDecimal.ZERO;

    @Column(name = "pdf_path", length = 500)
    private String pdfPath;

    @Column(name = "date_emission", nullable = false)
    private LocalDateTime dateEmission = LocalDateTime.now();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
