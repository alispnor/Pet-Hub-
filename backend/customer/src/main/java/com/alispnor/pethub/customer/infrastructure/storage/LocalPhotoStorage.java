package com.alispnor.pethub.customer.infrastructure.storage;

import com.alispnor.pethub.common.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@EnableConfigurationProperties(PhotoStorageProperties.class)
public class LocalPhotoStorage implements PhotoStorage {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final PhotoStorageProperties properties;

    public LocalPhotoStorage(PhotoStorageProperties properties) {
        this.properties = properties;
        try {
            Files.createDirectories(Path.of(properties.dir()));
            log.info("Diretório de uploads inicializado em {}", properties.dir());
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao criar diretório de uploads: " + properties.dir(), e);
        }
    }

    @Override
    public String store(String namespace, Long ownerId, MultipartFile file) {
        log.info("Salvando foto em namespace={}, ownerId={}, size={} bytes", namespace, ownerId, file.getSize());
        validate(file);

        var ext = extensionFor(file.getContentType());
        var filename = "%s-%d-%s%s".formatted(namespace, ownerId, UUID.randomUUID(), ext);
        var target = Path.of(properties.dir(), namespace, filename);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao salvar arquivo " + target, e);
        }

        var url = "%s/%s/%s".formatted(stripTrailingSlash(properties.publicBaseUrl()), namespace, filename);
        log.info("Foto salva: {} ({} bytes)", url, file.getSize());
        return url;
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessRuleException("Arquivo vazio");
        }
        if (file.getSize() > properties.maxBytes()) {
            throw new BusinessRuleException("Arquivo excede o tamanho máximo de " + properties.maxBytes() + " bytes");
        }
        var contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessRuleException("Tipo de arquivo não permitido: " + contentType);
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
