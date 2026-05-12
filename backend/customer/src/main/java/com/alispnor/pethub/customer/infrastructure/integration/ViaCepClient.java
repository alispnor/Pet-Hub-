package com.alispnor.pethub.customer.infrastructure.integration;

import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.customer.application.dto.ViaCepResponse;
import com.alispnor.pethub.customer.infrastructure.cache.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Slf4j
@Component
public class ViaCepClient {

    private final RestClient restClient;

    public ViaCepClient(@Value("${app.viacep.base-url:https://viacep.com.br}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(
                        java.net.http.HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(3))
                                .build()))
                .build();
    }

    @Cacheable(value = CacheConfig.CEP_CACHE, key = "#cep")
    public ViaCepResponse fetch(String cep) {
        log.info("Consultando ViaCEP para {} (cache MISS)", cep);
        try {
            var body = restClient.get()
                    .uri("/ws/{cep}/json/", cep)
                    .retrieve()
                    .body(ViaCepResponse.class);
            if (body == null || Boolean.TRUE.equals(body.erro())) {
                throw new ResourceNotFoundException("CEP " + cep + " não encontrado");
            }
            log.info("ViaCEP retornou {} / {}", body.cidade(), body.uf());
            return body;
        } catch (RestClientException e) {
            log.warn("ViaCEP indisponível para {}: {}", cep, e.getMessage());
            throw new ResourceNotFoundException("CEP " + cep + " não pôde ser consultado no momento");
        }
    }
}
