package com.moyeo.service.meeting;

import java.math.BigDecimal;

public record CommercialArea(
        String areaCode,
        String areaName,
        String categoryName,
        BigDecimal latitude,
        BigDecimal longitude,
        String guName,
        String dongName
) {
}
