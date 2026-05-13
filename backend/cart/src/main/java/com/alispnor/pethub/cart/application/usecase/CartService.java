package com.alispnor.pethub.cart.application.usecase;

import com.alispnor.pethub.cart.application.dto.ApplyCouponRequest;
import com.alispnor.pethub.cart.application.dto.CartResponse;
import com.alispnor.pethub.cart.application.dto.UpdateQtyRequest;
import com.alispnor.pethub.cart.domain.Cart;
import com.alispnor.pethub.cart.domain.CartCoupon;
import com.alispnor.pethub.cart.domain.CartItem;
import com.alispnor.pethub.cart.infrastructure.persistence.CartRepository;
import com.alispnor.pethub.catalog.domain.entity.Produto;
import com.alispnor.pethub.catalog.infrastructure.persistence.PrecoVigenteRepository;
import com.alispnor.pethub.catalog.infrastructure.persistence.ProdutoRepository;
import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProdutoRepository produtoRepository;
    private final PrecoVigenteRepository precoVigenteRepository;

    public CartResponse obter(Long userId) {
        log.info("Obtendo carrinho do usuário {}", userId);
        var cart = cartRepository.findByUserId(userId).orElse(Cart.empty(userId));
        log.info("Carrinho do usuário {} retornado com {} itens", userId, cart.items().size());
        return CartResponse.from(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse adicionarItem(Long userId, String sku, int qty) {
        log.info("Adicionando item ao carrinho user={}, sku={}, qty={}", userId, sku, qty);
        if (qty <= 0) {
            throw new BusinessRuleException("Quantidade precisa ser positiva");
        }
        var produto = produtoRepository.findBySkuAndAtivoTrue(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Produto " + sku + " não encontrado ou inativo"));
        var preco = precoVigenteRepository.findVigenteByProduto(produto)
                .orElseThrow(() -> new BusinessRuleException("Produto " + sku + " sem preço vigente"))
                .getValorBase();

        var imagem = produto.getImagens().stream()
                .filter(i -> i.isPrincipal())
                .findFirst()
                .or(() -> produto.getImagens().stream().findFirst())
                .map(i -> i.getUrl())
                .orElse(null);

        var cart = cartRepository.findByUserId(userId).orElse(Cart.empty(userId));
        var updated = upsertItem(cart, produto, sku, qty, preco, imagem);
        cartRepository.save(updated);

        log.info("Carrinho user={} agora tem {} itens", userId, updated.items().size());
        return CartResponse.from(updated);
    }

    public CartResponse atualizarItem(Long userId, String sku, UpdateQtyRequest request) {
        log.info("Atualizando qty user={}, sku={}, qty={}", userId, sku, request.qty());
        var cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrinho vazio"));
        var found = false;
        var items = new ArrayList<CartItem>(cart.items().size());
        for (var item : cart.items()) {
            if (item.sku().equals(sku)) {
                items.add(item.withQty(request.qty()));
                found = true;
            } else {
                items.add(item);
            }
        }
        if (!found) {
            throw new ResourceNotFoundException("Item " + sku + " não está no carrinho");
        }
        var updated = cart.withItems(items);
        cartRepository.save(updated);
        log.info("Carrinho user={} item {} atualizado para qty={}", userId, sku, request.qty());
        return CartResponse.from(updated);
    }

    public CartResponse removerItem(Long userId, String sku) {
        log.info("Removendo item user={}, sku={}", userId, sku);
        var cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrinho vazio"));
        var items = cart.items().stream()
                .filter(i -> !i.sku().equals(sku))
                .toList();
        if (items.size() == cart.items().size()) {
            throw new ResourceNotFoundException("Item " + sku + " não está no carrinho");
        }
        var updated = cart.withItems(items);
        cartRepository.save(updated);
        log.info("Carrinho user={} agora tem {} itens", userId, items.size());
        return CartResponse.from(updated);
    }

    public CartResponse aplicarCupom(Long userId, ApplyCouponRequest request) {
        log.info("Aplicando cupom user={}, codigo={}", userId, request.codigo());
        var cart = cartRepository.findByUserId(userId).orElse(Cart.empty(userId));
        // Nesta fase, o desconto real é computado pelo pricing/checkout. Aqui só
        // anexamos o código informado para que o pricing valide na sequência.
        var updated = cart.withCupom(new CartCoupon(request.codigo().trim().toUpperCase(), BigDecimal.ZERO));
        cartRepository.save(updated);
        log.info("Cupom {} anexado ao carrinho user={}", updated.cupom().codigo(), userId);
        return CartResponse.from(updated);
    }

    public CartResponse removerCupom(Long userId) {
        log.info("Removendo cupom do carrinho user={}", userId);
        var cart = cartRepository.findByUserId(userId).orElse(Cart.empty(userId));
        var updated = cart.withCupom(null);
        cartRepository.save(updated);
        log.info("Cupom removido do carrinho user={}", userId);
        return CartResponse.from(updated);
    }

    public void limpar(Long userId) {
        log.info("Limpando carrinho user={}", userId);
        cartRepository.delete(userId);
        log.info("Carrinho user={} apagado", userId);
    }

    private Cart upsertItem(Cart cart, Produto produto, String sku, int qty,
                            BigDecimal preco, String imagem) {
        var items = new ArrayList<CartItem>(cart.items().size() + 1);
        var found = false;
        for (var existing : cart.items()) {
            if (existing.sku().equals(sku)) {
                var totalQty = existing.qty() + qty;
                items.add(new CartItem(produto.getId(), sku, produto.getNome(), imagem,
                        totalQty, preco, preco.multiply(BigDecimal.valueOf(totalQty))));
                found = true;
            } else {
                items.add(existing);
            }
        }
        if (!found) {
            items.add(new CartItem(produto.getId(), sku, produto.getNome(), imagem,
                    qty, preco, preco.multiply(BigDecimal.valueOf(qty))));
        }
        return cart.withItems(items);
    }
}
