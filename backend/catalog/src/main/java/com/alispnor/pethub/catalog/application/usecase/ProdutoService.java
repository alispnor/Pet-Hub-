package com.alispnor.pethub.catalog.application.usecase;

import com.alispnor.pethub.catalog.application.dto.AdicionarImagemRequest;
import com.alispnor.pethub.catalog.application.dto.DefinirPrecoRequest;
import com.alispnor.pethub.catalog.application.dto.ProdutoAdminSummaryResponse;
import com.alispnor.pethub.catalog.application.dto.ProdutoDetailResponse;
import com.alispnor.pethub.catalog.application.dto.ProdutoImagemResponse;
import com.alispnor.pethub.catalog.application.dto.ProdutoRequest;
import com.alispnor.pethub.catalog.application.dto.ProdutoSummaryResponse;
import com.alispnor.pethub.catalog.application.mapper.ProdutoMapper;
import com.alispnor.pethub.catalog.domain.entity.PrecoVigente;
import com.alispnor.pethub.catalog.domain.entity.Produto;
import com.alispnor.pethub.catalog.domain.entity.ProdutoImagem;
import com.alispnor.pethub.catalog.infrastructure.persistence.CategoriaRepository;
import com.alispnor.pethub.catalog.infrastructure.persistence.PrecoVigenteRepository;
import com.alispnor.pethub.catalog.infrastructure.persistence.ProdutoImagemRepository;
import com.alispnor.pethub.catalog.infrastructure.persistence.ProdutoRepository;
import com.alispnor.pethub.common.exception.ConflictException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.identity.domain.entity.Usuario;
import com.alispnor.pethub.identity.infrastructure.persistence.UsuarioRepository;
import com.alispnor.pethub.identity.infrastructure.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ProdutoImagemRepository produtoImagemRepository;
    private final PrecoVigenteRepository precoVigenteRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoMapper produtoMapper;

    @Transactional(readOnly = true)
    public Page<ProdutoSummaryResponse> listar(String categoriaSlug, Pageable pageable) {
        log.debug("Iniciando listar produtos categoria={} page={}", categoriaSlug, pageable);
        var page = categoriaSlug == null
                ? produtoRepository.findByAtivoTrue(pageable)
                : produtoRepository.findByAtivoTrueAndCategoria_Slug(categoriaSlug, pageable);
        var result = page.map(this::toSummary);
        log.debug("Listar OK, total={}", result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public Page<ProdutoSummaryResponse> buscar(String query, Pageable pageable) {
        log.debug("Iniciando buscar produtos query={}", query);
        var result = produtoRepository.search(query, pageable).map(this::toSummary);
        log.debug("Buscar OK, total={}", result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public Page<ProdutoAdminSummaryResponse> listarAdmin(String query, Long categoriaId, Boolean ativo, Pageable pageable) {
        log.debug("Iniciando listarAdmin q={} categoriaId={} ativo={} pageable={}", query, categoriaId, ativo, pageable);
        var queryNormalizado = (query == null || query.isBlank()) ? null : query.trim();
        var page = produtoRepository.filtrar(queryNormalizado, categoriaId, ativo, pageable);
        var result = page.map(this::toAdminSummary);
        log.debug("ListarAdmin OK total={}", result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public ProdutoDetailResponse buscarPorSku(String sku) {
        log.debug("Iniciando buscarPorSku sku={}", sku);
        var produto = produtoRepository.findBySkuAndAtivoTrue(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + sku));
        var response = toDetail(produto);
        log.debug("BuscarPorSku OK id={}", produto.getId());
        return response;
    }

    @Transactional
    public ProdutoDetailResponse criar(ProdutoRequest request, AuthenticatedUser autor) {
        log.debug("Iniciando criar produto sku={}", request.sku());
        if (produtoRepository.existsBySku(request.sku())) {
            throw new ConflictException("Produto com SKU já existe: " + request.sku());
        }
        var categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + request.categoriaId()));
        var produto = Produto.builder()
                .sku(request.sku())
                .nome(request.nome())
                .descricaoCurta(request.descricaoCurta())
                .descricaoCompleta(request.descricaoCompleta())
                .marca(request.marca())
                .categoria(categoria)
                .pesoKg(request.pesoKg())
                .alturaCm(request.alturaCm())
                .larguraCm(request.larguraCm())
                .profundidadeCm(request.profundidadeCm())
                .ncm(request.ncm())
                .origem(request.origem())
                .specs(request.specs() == null ? java.util.Map.of() : request.specs())
                .videoUrl(request.videoUrl())
                .destacado(request.destacado())
                .ativo(true)
                .build();
        var salvo = produtoRepository.save(produto);
        persistirPreco(salvo, request.precoInicial(), autor);
        var response = toDetail(salvo);
        log.debug("Produto criado id={}", salvo.getId());
        return response;
    }

    @Transactional
    public ProdutoDetailResponse atualizar(Long id, ProdutoRequest request) {
        log.debug("Iniciando atualizar produto id={}", id);
        var produto = obrigatorio(id);
        if (!produto.getSku().equals(request.sku()) && produtoRepository.existsBySku(request.sku())) {
            throw new ConflictException("Produto com SKU já existe: " + request.sku());
        }
        var categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + request.categoriaId()));
        produto.setSku(request.sku());
        produto.setNome(request.nome());
        produto.setDescricaoCurta(request.descricaoCurta());
        produto.setDescricaoCompleta(request.descricaoCompleta());
        produto.setMarca(request.marca());
        produto.setCategoria(categoria);
        produto.setPesoKg(request.pesoKg());
        produto.setAlturaCm(request.alturaCm());
        produto.setLarguraCm(request.larguraCm());
        produto.setProfundidadeCm(request.profundidadeCm());
        produto.setNcm(request.ncm());
        produto.setOrigem(request.origem());
        produto.setSpecs(request.specs() == null ? java.util.Map.of() : request.specs());
        produto.setVideoUrl(request.videoUrl());
        produto.setDestacado(request.destacado());
        var response = toDetail(produtoRepository.save(produto));
        log.debug("Produto atualizado id={}", id);
        return response;
    }

    @Transactional
    public ProdutoDetailResponse toggleAtivo(Long id) {
        log.debug("Iniciando toggleAtivo produto id={}", id);
        var produto = obrigatorio(id);
        produto.setAtivo(!produto.isAtivo());
        var response = toDetail(produtoRepository.save(produto));
        log.debug("ToggleAtivo OK id={} ativo={}", id, produto.isAtivo());
        return response;
    }

    @Transactional
    public ProdutoImagemResponse adicionarImagem(Long id, AdicionarImagemRequest request) {
        log.debug("Iniciando adicionarImagem produto id={}", id);
        var produto = obrigatorio(id);
        if (request.principal()) {
            produto.getImagens().forEach(img -> img.setPrincipal(false));
        }
        var nova = ProdutoImagem.builder()
                .produto(produto)
                .url(request.url())
                .ordem(produto.getImagens().size())
                .principal(request.principal() || produto.getImagens().isEmpty())
                .build();
        produto.getImagens().add(nova);
        produtoRepository.save(produto);
        var response = produtoMapper.toImagemResponse(nova);
        log.debug("Imagem adicionada produtoId={} imagemId={}", id, nova.getId());
        return response;
    }

    @Transactional
    public void removerImagem(Long produtoId, Long imagemId) {
        log.debug("Iniciando removerImagem produtoId={} imagemId={}", produtoId, imagemId);
        var produto = obrigatorio(produtoId);
        var removida = produto.getImagens().removeIf(img -> img.getId().equals(imagemId));
        if (!removida) {
            throw new ResourceNotFoundException("Imagem não encontrada: " + imagemId);
        }
        if (!produto.getImagens().isEmpty()
                && produto.getImagens().stream().noneMatch(ProdutoImagem::isPrincipal)) {
            produto.getImagens().get(0).setPrincipal(true);
        }
        produtoRepository.save(produto);
        log.debug("Imagem removida produtoId={} imagemId={}", produtoId, imagemId);
    }

    @Transactional
    public ProdutoDetailResponse definirPreco(Long produtoId, DefinirPrecoRequest request, AuthenticatedUser autor) {
        log.debug("Iniciando definirPreco produtoId={} valor={}", produtoId, request.valor());
        var produto = obrigatorio(produtoId);
        persistirPreco(produto, request.valor(), autor);
        var response = toDetail(produto);
        log.debug("Preco definido produtoId={}", produtoId);
        return response;
    }

    private void persistirPreco(Produto produto, BigDecimal valor, AuthenticatedUser autor) {
        var agora = LocalDateTime.now();
        precoVigenteRepository.findVigenteByProduto(produto).ifPresent(vigente -> {
            vigente.setDataFim(agora);
            precoVigenteRepository.save(vigente);
        });
        var criador = autor == null ? null : usuarioRepository.findById(autor.id()).orElse(null);
        var novo = PrecoVigente.builder()
                .produto(produto)
                .valorBase(valor)
                .dataInicio(agora)
                .criadoPor(criador)
                .build();
        precoVigenteRepository.save(novo);
    }

    private Produto obrigatorio(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + id));
    }

    private ProdutoSummaryResponse toSummary(Produto p) {
        var preco = precoVigenteRepository.findVigenteByProduto(p)
                .map(PrecoVigente::getValorBase)
                .orElse(BigDecimal.ZERO);
        return produtoMapper.toSummary(p, preco);
    }

    private ProdutoDetailResponse toDetail(Produto p) {
        var preco = precoVigenteRepository.findVigenteByProduto(p)
                .map(PrecoVigente::getValorBase)
                .orElse(BigDecimal.ZERO);
        return produtoMapper.toDetail(p, preco);
    }

    private ProdutoAdminSummaryResponse toAdminSummary(Produto produto) {
        var preco = precoVigenteRepository.findVigenteByProduto(produto)
                .map(PrecoVigente::getValorBase)
                .orElse(BigDecimal.ZERO);
        return produtoMapper.toAdminSummary(produto, preco);
    }
}
