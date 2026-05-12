package com.alispnor.pethub.customer.application.dto;

import com.alispnor.pethub.common.validation.ValidCpf;
import jakarta.validation.constraints.NotBlank;

public record SetCpfRequest(
        @NotBlank @ValidCpf String cpf
) {
}
