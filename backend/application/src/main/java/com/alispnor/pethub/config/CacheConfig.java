package com.alispnor.pethub.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Configuração central de caches Redis. Cada cache nomeada tem TTL próprio.
 * Caches:
 *  - {@code cep}    — resultado do ViaCEP (TTL 24h)
 *  - {@code frete}  — cotações de frete por CEP+itens (TTL 1h)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CEP_CACHE = "cep";
    public static final String FRETE_CACHE = "frete";

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        var jsonSerializer = new GenericJackson2JsonRedisSerializer();
        var defaults = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withCacheConfiguration(CEP_CACHE, defaults.entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration(FRETE_CACHE, defaults.entryTtl(Duration.ofHours(1)))
                .build();
    }
}
