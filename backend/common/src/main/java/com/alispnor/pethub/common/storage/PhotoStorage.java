package com.alispnor.pethub.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface PhotoStorage {

    /**
     * Persiste o arquivo num backend (filesystem local, S3, etc.) e devolve a URL pública.
     * @param namespace pasta lógica (ex: "pets", "products").
     * @param ownerId id do dono (ex: pet id, produto id) — usado pra nomear arquivos previsivelmente.
     * @param file arquivo recebido via multipart.
     */
    String store(String namespace, Long ownerId, MultipartFile file);
}
