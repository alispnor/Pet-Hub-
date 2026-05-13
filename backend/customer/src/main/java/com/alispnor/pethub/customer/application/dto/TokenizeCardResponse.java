package com.alispnor.pethub.customer.application.dto;

import com.alispnor.pethub.customer.domain.entity.Bandeira;

public record TokenizeCardResponse(
        String token,
        Bandeira bandeira,
        String ultimosQuatroDigitos
) {
}
