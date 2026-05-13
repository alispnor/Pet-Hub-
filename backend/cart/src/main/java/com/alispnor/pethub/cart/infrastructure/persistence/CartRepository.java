package com.alispnor.pethub.cart.infrastructure.persistence;

import com.alispnor.pethub.cart.domain.Cart;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CartRepository {

    private static final Duration TTL = Duration.ofDays(30);
    private static final String KEY_PREFIX = "cart:";

    private final RedisTemplate<String, Cart> cartRedisTemplate;

    public Optional<Cart> findByUserId(Long userId) {
        var key = keyFor(userId);
        var value = cartRedisTemplate.opsForValue().get(key);
        return Optional.ofNullable(value);
    }

    public void save(Cart cart) {
        var key = keyFor(cart.userId());
        cartRedisTemplate.opsForValue().set(key, cart, TTL);
    }

    public void delete(Long userId) {
        cartRedisTemplate.delete(keyFor(userId));
    }

    private String keyFor(Long userId) {
        return KEY_PREFIX + userId;
    }
}
