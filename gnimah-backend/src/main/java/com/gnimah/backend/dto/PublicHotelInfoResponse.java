package com.gnimah.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicHotelInfoResponse {
    private long totalChambres;
}
