package com.alispnor.pethub.shipping.application.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Wrapper concreto em volta da lista de opções. Existe porque o serializer
 * Jackson default que registramos para o cache Redis não preserva o typing
 * de elementos de coleção — embrulhar num record nominal resolve o
 * "Unexpected token (START_OBJECT), expected VALUE_STRING" na desserialização.
 */
public record OpcoesFreteResponse(List<OpcaoFreteResponse> opcoes) implements Serializable {
}
