package com.alispnor.pethub.catalog.application.mapper;

import com.alispnor.pethub.catalog.application.dto.ProdutoAdminSummaryResponse;
import com.alispnor.pethub.catalog.application.dto.ProdutoDetailResponse;
import com.alispnor.pethub.catalog.application.dto.ProdutoImagemResponse;
import com.alispnor.pethub.catalog.application.dto.ProdutoSummaryResponse;
import com.alispnor.pethub.catalog.domain.entity.Produto;
import com.alispnor.pethub.catalog.domain.entity.ProdutoImagem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", uses = CategoriaMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProdutoMapper {

    @Mapping(target = "categoriaSlug", source = "produto.categoria.slug")
    @Mapping(target = "imagemPrincipal", source = "produto.imagens", qualifiedByName = "imagemPrincipalUrl")
    @Mapping(target = "preco", source = "preco")
    ProdutoSummaryResponse toSummary(Produto produto, BigDecimal preco);

    @Mapping(target = "categoriaId", source = "produto.categoria.id")
    @Mapping(target = "categoriaNome", source = "produto.categoria.nome")
    @Mapping(target = "categoriaSlug", source = "produto.categoria.slug")
    @Mapping(target = "imagemPrincipal", source = "produto.imagens", qualifiedByName = "imagemPrincipalUrl")
    @Mapping(target = "totalImagens", source = "produto.imagens", qualifiedByName = "totalImagens")
    @Mapping(target = "temVideo", source = "produto", qualifiedByName = "temVideo")
    @Mapping(target = "preco", source = "preco")
    ProdutoAdminSummaryResponse toAdminSummary(Produto produto, BigDecimal preco);

    @Mapping(target = "preco", source = "preco")
    @Mapping(target = "categoria", source = "produto.categoria")
    @Mapping(target = "imagens", source = "produto.imagens")
    ProdutoDetailResponse toDetail(Produto produto, BigDecimal preco);

    ProdutoImagemResponse toImagemResponse(ProdutoImagem imagem);

    @Named("imagemPrincipalUrl")
    default String imagemPrincipalUrl(List<ProdutoImagem> imagens) {
        if (imagens == null || imagens.isEmpty()) {
            return null;
        }
        return imagens.stream()
                .filter(ProdutoImagem::isPrincipal)
                .map(ProdutoImagem::getUrl)
                .findFirst()
                .orElseGet(() -> imagens.get(0).getUrl());
    }

    @Named("totalImagens")
    default int totalImagens(List<ProdutoImagem> imagens) {
        return imagens == null ? 0 : imagens.size();
    }

    @Named("temVideo")
    default boolean temVideo(Produto produto) {
        return produto.getVideoUrl() != null && !produto.getVideoUrl().isBlank();
    }
}
