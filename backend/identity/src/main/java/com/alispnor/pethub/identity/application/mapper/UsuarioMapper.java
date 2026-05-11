package com.alispnor.pethub.identity.application.mapper;

import com.alispnor.pethub.identity.application.dto.UsuarioResponse;
import com.alispnor.pethub.identity.domain.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UsuarioMapper {

    UsuarioResponse toResponse(Usuario usuario);
}
