package com.alispnor.pethub.catalog.application.mapper;

import com.alispnor.pethub.catalog.application.dto.CategoriaResponse;
import com.alispnor.pethub.catalog.domain.entity.Categoria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoriaMapper {

    @Mapping(target = "categoriaPaiId", source = "categoriaPai.id")
    CategoriaResponse toResponse(Categoria categoria);
}
