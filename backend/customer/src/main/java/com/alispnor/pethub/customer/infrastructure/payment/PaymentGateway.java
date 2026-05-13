package com.alispnor.pethub.customer.infrastructure.payment;

import com.alispnor.pethub.customer.domain.entity.Bandeira;

public interface PaymentGateway {

    record CardData(String numero, String cvv, String nomeImpresso, int validadeMes, int validadeAno) {
    }

    record TokenizationResult(String token, Bandeira bandeira, String ultimosQuatroDigitos) {
    }

    TokenizationResult tokenizeCard(CardData card);
}
