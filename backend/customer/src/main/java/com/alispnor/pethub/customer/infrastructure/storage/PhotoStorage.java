package com.alispnor.pethub.customer.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface PhotoStorage {

    /**
     * Persiste a foto em algum backend (filesystem local, S3, etc) e retorna a URL pública.
     */
    String store(String namespace, Long ownerId, MultipartFile file);
}
