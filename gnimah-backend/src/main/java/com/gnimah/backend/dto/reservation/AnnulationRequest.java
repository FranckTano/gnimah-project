package com.gnimah.backend.dto.reservation;

import lombok.Data;

@Data
public class AnnulationRequest {
    private String motif;
    /** Rembourser l acompte depuis la caisse au moment de l annulation. */
    private Boolean rembourser;
    private Long caisseId;
    private String mode;
}
