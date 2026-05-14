package com.alispnor.pethub.inventory.application.dto;

public record EstoqueResponse(
        Long id,
        Long produtoId,
        String sku,
        String nomeProduto,
        int quantidade,
        int quantidadeReservada,
        int disponivel,
        int quantidadeMinima,
        String localizacao
) {
}
