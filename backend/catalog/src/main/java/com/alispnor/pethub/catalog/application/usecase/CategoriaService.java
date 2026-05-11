package com.alispnor.pethub.catalog.application.usecase;

import com.alispnor.pethub.catalog.application.dto.CategoriaRequest;
import com.alispnor.pethub.catalog.application.dto.CategoriaResponse;
import com.alispnor.pethub.catalog.application.mapper.CategoriaMapper;
import com.alispnor.pethub.catalog.domain.entity.Categoria;
import com.alispnor.pethub.catalog.infrastructure.persistence.CategoriaRepository;
import com.alispnor.pethub.common.exception.ConflictException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(boolean apenasAtivas) {
        log.debug("Iniciando listar categorias apenasAtivas={}", apenasAtivas);
        var categorias = apenasAtivas
                ? categoriaRepository.findByAtivoTrueOrderByOrdemAscNomeAsc()
                : categoriaRepository.findAllByOrderByOrdemAscNomeAsc();
        var response = categorias.stream().map(categoriaMapper::toResponse).toList();
        log.debug("Listar OK, {} categorias", response.size());
        return response;
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        log.debug("Iniciando criar categoria slug={}", request.slug());
        if (categoriaRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Categoria com slug já existe: " + request.slug());
        }
        var pai = resolvePai(request.categoriaPaiId());
        var categoria = Categoria.builder()
                .nome(request.nome())
                .slug(request.slug())
                .descricao(request.descricao())
                .categoriaPai(pai)
                .ordem(request.ordem())
                .ativo(true)
                .build();
        var salvo = categoriaRepository.save(categoria);
        var response = categoriaMapper.toResponse(salvo);
        log.debug("Categoria criada id={}", salvo.getId());
        return response;
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        log.debug("Iniciando atualizar categoria id={}", id);
        var categoria = obrigatorio(id);
        if (!categoria.getSlug().equals(request.slug()) && categoriaRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Categoria com slug já existe: " + request.slug());
        }
        if (request.categoriaPaiId() != null && request.categoriaPaiId().equals(id)) {
            throw new ConflictException("Categoria não pode ser pai de si mesma");
        }
        categoria.setNome(request.nome());
        categoria.setSlug(request.slug());
        categoria.setDescricao(request.descricao());
        categoria.setCategoriaPai(resolvePai(request.categoriaPaiId()));
        categoria.setOrdem(request.ordem());
        var response = categoriaMapper.toResponse(categoriaRepository.save(categoria));
        log.debug("Categoria atualizada id={}", id);
        return response;
    }

    @Transactional
    public void desativar(Long id) {
        log.debug("Iniciando desativar categoria id={}", id);
        var categoria = obrigatorio(id);
        categoria.setAtivo(false);
        categoriaRepository.save(categoria);
        log.debug("Categoria desativada id={}", id);
    }

    private Categoria obrigatorio(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + id));
    }

    private Categoria resolvePai(Long paiId) {
        if (paiId == null) {
            return null;
        }
        return categoriaRepository.findById(paiId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria pai não encontrada: " + paiId));
    }
}
