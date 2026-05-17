# Admin Slice 6.2 — Catálogo (Produtos + Categorias + Vídeo) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar CRUDs admin de Produtos (lista filtrada + form criar/editar + upload de imagens via multipart + vídeo via URL externa) e Categorias (lista plana indentada + form inline) em `frontend/admin/`, mais a sub-feature de exibição de vídeo no PDP do storefront (iframe sandboxed). Backend ganha endpoint admin de listagem com filtros completos, endpoint multipart de upload (storage reaproveitado), Flyway V18 com coluna `video_url` e validador `@ValidVideoEmbedUrl`.

**Architecture:** Backend continua multi-module Maven com Clean Architecture. `PhotoStorage` (interface + impl + properties) migra de `customer/` para `common/` para ser reusada pelo catalog (catalog não depende de customer). Multipart upload usa o mesmo padrão de `PetController.uploadFoto` (campo `file`). Frontend admin ganha módulo `modules/catalog/` (3 páginas + 2 componentes + 2 services + 2 models) replicando padrões já estabelecidos no Slice 6.1 (signals + reactive forms + standalone + Tailwind). Storefront ganha `SafeResourceUrlPipe` e seção condicional de vídeo no PDP.

**Tech Stack:** Java 21, Spring Boot 3, JPA, MapStruct, Flyway, Bean Validation. Angular 17 standalone, TypeScript strict, Tailwind 3, Reactive Forms, signals, RxJS (concatMap/catchError/switchMap).

**Spec:** `docs/superpowers/specs/2026-05-17-admin-slice2-catalog-design.md` (referência durante implementação).

**Convenção de commits:** Conventional Commits com escopo `feat(backend):`, `feat(admin):`, `feat(storefront):`, `chore(*):` ou `refactor(backend):`, sufixo `slice2 NN-descricao` (NN = 01..23).

**Política de testes:** Tests formais Karma/Jest/JUnit continuam dívida acumulada (mesmo bloqueio das fases 1-6.1). Verificação por task = build verde do módulo afetado + smoke programático manual (curl/browser). Smoke checklist completa na Task 23.

**Divergências em relação à spec congeladas neste plano:**
1. `ProdutoRequest` permanece DTO único pra criar/atualizar (backend real); spec assumia dois DTOs.
2. `Origem` real é `NACIONAL | IMPORTADO_DIRETO | IMPORTADO_INDIRETO`; spec dizia `NACIONAL | IMPORTADO | NACIONAL_FABRICACAO_PROPRIA` — usar real.
3. Backend ganha **novo** endpoint admin `GET /admin/catalog/products` com filtros + sort, retornando `ProdutoAdminSummaryResponse` (novo DTO mais rico que o público).
4. Upload de imagens via **multipart real** (decisão do usuário): backend ganha `POST /admin/catalog/products/{id}/images/upload` consumindo `multipart/form-data` campo `file`, reusando `PhotoStorage` movido para `common`. URL legacy via `AdicionarImagemRequest` continua operacional (não removida).
5. Categoria ganha endpoint `POST /admin/catalog/categories/{id}/restaurar` para reverter soft-delete (admin precisa ativar de volta).
6. `AdminUser` tem `roles: Role[]` (não `role`); `roleGuard` já espera arrays — plano usa helpers existentes.

---

## File Structure

**Arquivos movidos (refactor):**

```
backend/customer/src/main/java/com/alispnor/pethub/customer/infrastructure/storage/
  PhotoStorage.java               ⊖ removido (movido para common)
  LocalPhotoStorage.java          ⊖ removido (movido para common)
  PhotoStorageProperties.java     ⊖ removido (movido para common)

backend/common/src/main/java/com/alispnor/pethub/common/storage/   ⊕ novo pacote
  PhotoStorage.java               ⊕ (mesmo conteúdo, novo package)
  LocalPhotoStorage.java          ⊕
  PhotoStorageProperties.java     ⊕
```

**Arquivos backend criados:**

```
backend/application/src/main/resources/db/migration/
  V18__add_video_url_to_produto.sql                                   ⊕

backend/common/src/main/java/com/alispnor/pethub/common/validation/
  ValidVideoEmbedUrl.java                                             ⊕
  ValidVideoEmbedUrlValidator.java                                    ⊕

backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/dto/
  ProdutoAdminSummaryResponse.java                                    ⊕

backend/catalog/src/main/java/com/alispnor/pethub/catalog/infrastructure/persistence/
  ProdutoAdminRepository.java                                         ⊕
```

**Arquivos backend modificados:**

```
backend/catalog/src/main/java/com/alispnor/pethub/catalog/
  domain/entity/Produto.java                  ⊕ campo videoUrl
  application/dto/ProdutoRequest.java         ⊕ campo videoUrl
  application/dto/ProdutoDetailResponse.java  ⊕ campo videoUrl
  application/mapper/ProdutoMapper.java       ⊕ mapping ProdutoAdminSummaryResponse
  application/usecase/ProdutoService.java     ⊕ setar videoUrl, novo método listarAdmin
  infrastructure/rest/ProdutoController.java  ⊕ GET /admin/catalog/products, POST /images/upload
  application/usecase/CategoriaService.java   ⊕ método restaurar
  infrastructure/rest/CategoriaController.java ⊕ POST /{id}/restaurar

backend/customer/src/main/java/com/alispnor/pethub/customer/
  application/usecase/PetService.java         ⊕ import path PhotoStorage (após move)
```

**Arquivos frontend admin criados:**

```
frontend/admin/src/app/
  modules/catalog/
    models/produto.ts                                      ⊕
    models/categoria.ts                                    ⊕
    services/produto.service.ts                            ⊕
    services/categoria.service.ts                          ⊕
    pages/produtos-lista/produtos-lista.page.{ts,html}     ⊕
    pages/produto-form/produto-form.page.{ts,html}         ⊕
    pages/categorias/categorias.page.{ts,html}             ⊕
    components/image-drop-zone/image-drop-zone.component.ts ⊕
    components/video-url-input/video-url-input.component.ts ⊕
  shared/
    pipes/safe-resource-url.pipe.ts                        ⊕
    utils/video-embed.ts                                   ⊕
```

**Arquivos frontend admin modificados:**

```
frontend/admin/src/app/app.routes.ts                       ⊕ rotas /produtos, /produtos/novo, /produtos/:sku/editar, /categorias
frontend/admin/src/app/core/layout/admin-shell/admin-shell.page.ts ⊕ habilitar item Catálogo + adicionar item Categorias
```

**Arquivos frontend storefront criados:**

```
frontend/storefront/src/app/shared/pipes/safe-resource-url.pipe.ts  ⊕
frontend/storefront/src/app/shared/utils/video-embed.ts             ⊕
```

**Arquivos frontend storefront modificados:**

```
frontend/storefront/src/app/modules/catalog/models/catalog.ts                            ⊕ campo videoUrl
frontend/storefront/src/app/modules/catalog/pages/product-detail/product-detail.page.ts  ⊕ computed videoEmbed
frontend/storefront/src/app/modules/catalog/pages/product-detail/product-detail.page.html ⊕ seção vídeo
```

**Arquivos docs criados/modificados:**

```
ai-memory/roadmap/fase-6-pendencias.md          ⊕ atualizar (slice 6.2 entregue)
ROADMAP.md                                       ⊕ atualizar status fase 6
```

---

## Task 1: Mover PhotoStorage para common module

**Files:**
- Create: `backend/common/src/main/java/com/alispnor/pethub/common/storage/PhotoStorage.java`
- Create: `backend/common/src/main/java/com/alispnor/pethub/common/storage/LocalPhotoStorage.java`
- Create: `backend/common/src/main/java/com/alispnor/pethub/common/storage/PhotoStorageProperties.java`
- Delete: 3 arquivos correspondentes em `backend/customer/.../storage/`
- Modify: `backend/customer/src/main/java/com/alispnor/pethub/customer/application/usecase/PetService.java` (import path)
- Modify: `backend/application/src/main/java/com/alispnor/pethub/config/WebMvcConfig.java` (import path)

- [ ] **Step 1: Criar diretório de destino**

```bash
mkdir -p /home/ali/projects/pet-hub/backend/common/src/main/java/com/alispnor/pethub/common/storage
```

- [ ] **Step 2: Criar `PhotoStorage.java` em common**

Caminho: `backend/common/src/main/java/com/alispnor/pethub/common/storage/PhotoStorage.java`

```java
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
```

- [ ] **Step 3: Criar `PhotoStorageProperties.java` em common**

Caminho: `backend/common/src/main/java/com/alispnor/pethub/common/storage/PhotoStorageProperties.java`

```java
package com.alispnor.pethub.common.storage;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.uploads")
public record PhotoStorageProperties(
        @NotBlank String dir,
        @NotBlank String publicBaseUrl,
        long maxBytes
) {
    public PhotoStorageProperties {
        if (maxBytes <= 0) {
            maxBytes = 5L * 1024 * 1024;
        }
    }
}
```

- [ ] **Step 4: Criar `LocalPhotoStorage.java` em common**

Caminho: `backend/common/src/main/java/com/alispnor/pethub/common/storage/LocalPhotoStorage.java`

```java
package com.alispnor.pethub.common.storage;

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
        log.info("Salvando arquivo em namespace={}, ownerId={}, size={} bytes", namespace, ownerId, file.getSize());
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
        log.info("Arquivo salvo: {} ({} bytes)", url, file.getSize());
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
```

- [ ] **Step 5: Atualizar import em `PetService.java`**

Em `backend/customer/src/main/java/com/alispnor/pethub/customer/application/usecase/PetService.java` substituir:

```java
import com.alispnor.pethub.customer.infrastructure.storage.PhotoStorage;
```

por

```java
import com.alispnor.pethub.common.storage.PhotoStorage;
```

- [ ] **Step 6: Atualizar import em `WebMvcConfig.java`**

Em `backend/application/src/main/java/com/alispnor/pethub/config/WebMvcConfig.java` substituir:

```java
import com.alispnor.pethub.customer.infrastructure.storage.PhotoStorageProperties;
```

por

```java
import com.alispnor.pethub.common.storage.PhotoStorageProperties;
```

- [ ] **Step 7: Deletar arquivos antigos em customer**

```bash
rm /home/ali/projects/pet-hub/backend/customer/src/main/java/com/alispnor/pethub/customer/infrastructure/storage/PhotoStorage.java
rm /home/ali/projects/pet-hub/backend/customer/src/main/java/com/alispnor/pethub/customer/infrastructure/storage/LocalPhotoStorage.java
rm /home/ali/projects/pet-hub/backend/customer/src/main/java/com/alispnor/pethub/customer/infrastructure/storage/PhotoStorageProperties.java
rmdir /home/ali/projects/pet-hub/backend/customer/src/main/java/com/alispnor/pethub/customer/infrastructure/storage 2>/dev/null || true
```

- [ ] **Step 8: Build full reactor**

```bash
cd /home/ali/projects/pet-hub
docker run --rm -v "$PWD/backend:/work" -v "$HOME/.m2:/root/.m2" \
  -w /work maven:3.9-eclipse-temurin-21 mvn -q -DskipTests package
```

Expected: build verde em todos os módulos.

- [ ] **Step 9: Commit**

```bash
cd /home/ali/projects/pet-hub
git add backend/common/src/main/java/com/alispnor/pethub/common/storage \
        backend/customer/src/main/java/com/alispnor/pethub/customer/infrastructure/storage \
        backend/customer/src/main/java/com/alispnor/pethub/customer/application/usecase/PetService.java \
        backend/application/src/main/java/com/alispnor/pethub/config/WebMvcConfig.java
git commit -m "refactor(backend): slice2 01-move PhotoStorage to common module"
```

---

## Task 2: Flyway V18 — coluna video_url

**Files:**
- Create: `backend/application/src/main/resources/db/migration/V18__add_video_url_to_produto.sql`

- [ ] **Step 1: Criar migration**

Caminho: `backend/application/src/main/resources/db/migration/V18__add_video_url_to_produto.sql`

```sql
ALTER TABLE produtos
    ADD COLUMN video_url VARCHAR(500);

COMMENT ON COLUMN produtos.video_url IS 'URL externa de vídeo (YouTube/Vimeo) — validada por @ValidVideoEmbedUrl no DTO.';
```

- [ ] **Step 2: Build (Flyway só roda em runtime; build valida sintaxe via plugin)**

```bash
cd /home/ali/projects/pet-hub
docker run --rm -v "$PWD/backend:/work" -v "$HOME/.m2:/root/.m2" \
  -w /work maven:3.9-eclipse-temurin-21 mvn -q -DskipTests -pl application -am package
```

Expected: build verde.

- [ ] **Step 3: Commit**

```bash
cd /home/ali/projects/pet-hub
git add backend/application/src/main/resources/db/migration/V18__add_video_url_to_produto.sql
git commit -m "feat(backend): slice2 02-flyway V18 add video_url to produtos"
```

---

## Task 3: Validador @ValidVideoEmbedUrl

**Files:**
- Create: `backend/common/src/main/java/com/alispnor/pethub/common/validation/ValidVideoEmbedUrl.java`
- Create: `backend/common/src/main/java/com/alispnor/pethub/common/validation/ValidVideoEmbedUrlValidator.java`

- [ ] **Step 1: Criar annotation `ValidVideoEmbedUrl.java`**

Caminho: `backend/common/src/main/java/com/alispnor/pethub/common/validation/ValidVideoEmbedUrl.java`

```java
package com.alispnor.pethub.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidVideoEmbedUrlValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidVideoEmbedUrl {
    String message() default "URL de vídeo deve ser do YouTube ou Vimeo (máx. 500 caracteres)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

- [ ] **Step 2: Criar `ValidVideoEmbedUrlValidator.java`**

Caminho: `backend/common/src/main/java/com/alispnor/pethub/common/validation/ValidVideoEmbedUrlValidator.java`

```java
package com.alispnor.pethub.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ValidVideoEmbedUrlValidator implements ConstraintValidator<ValidVideoEmbedUrl, String> {

    private static final Pattern PADRAO = Pattern.compile(
            "^https://(www\\.)?(youtube\\.com/watch\\?v=[\\w-]+|youtu\\.be/[\\w-]+|vimeo\\.com/\\d+)([&?][\\w-=&]*)?$"
    );
    private static final int TAMANHO_MAXIMO = 500;

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        if (valor == null || valor.isBlank()) {
            return true;
        }
        if (valor.length() > TAMANHO_MAXIMO) {
            return false;
        }
        return PADRAO.matcher(valor).matches();
    }
}
```

- [ ] **Step 3: Build common**

```bash
cd /home/ali/projects/pet-hub
docker run --rm -v "$PWD/backend:/work" -v "$HOME/.m2:/root/.m2" \
  -w /work maven:3.9-eclipse-temurin-21 mvn -q -DskipTests -pl common -am package
```

Expected: build verde.

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add backend/common/src/main/java/com/alispnor/pethub/common/validation/ValidVideoEmbedUrl.java \
        backend/common/src/main/java/com/alispnor/pethub/common/validation/ValidVideoEmbedUrlValidator.java
git commit -m "feat(backend): slice2 03-add ValidVideoEmbedUrl constraint"
```

---

## Task 4: Produto entity + DTOs videoUrl

**Files:**
- Modify: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/domain/entity/Produto.java`
- Modify: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/dto/ProdutoRequest.java`
- Modify: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/dto/ProdutoDetailResponse.java`

- [ ] **Step 1: Adicionar campo `videoUrl` em `Produto.java`**

Em `backend/catalog/src/main/java/com/alispnor/pethub/catalog/domain/entity/Produto.java`, após o campo `private String ncm;` (linha ~81), adicionar antes do `@Enumerated`:

```java
    @Column(name = "video_url", length = 500)
    private String videoUrl;
```

- [ ] **Step 2: Adicionar campo `videoUrl` em `ProdutoRequest.java`**

Substituir o record por:

```java
package com.alispnor.pethub.catalog.application.dto;

import com.alispnor.pethub.catalog.domain.entity.Origem;
import com.alispnor.pethub.common.validation.ValidNcm;
import com.alispnor.pethub.common.validation.ValidVideoEmbedUrl;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record ProdutoRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 200) String nome,
        @Size(max = 500) String descricaoCurta,
        String descricaoCompleta,
        @Size(max = 100) String marca,
        @NotNull Long categoriaId,
        @NotNull @DecimalMin(value = "0.001", message = "Peso deve ser maior que zero") BigDecimal pesoKg,
        @DecimalMin(value = "0.0") BigDecimal alturaCm,
        @DecimalMin(value = "0.0") BigDecimal larguraCm,
        @DecimalMin(value = "0.0") BigDecimal profundidadeCm,
        @NotBlank @ValidNcm String ncm,
        @NotNull Origem origem,
        Map<String, Object> specs,
        boolean destacado,
        @NotNull @DecimalMin(value = "0.01", message = "Preço inicial deve ser maior que zero") BigDecimal precoInicial,
        @ValidVideoEmbedUrl @Size(max = 500) String videoUrl
) {
}
```

- [ ] **Step 3: Adicionar campo `videoUrl` em `ProdutoDetailResponse.java`**

Substituir record por:

```java
package com.alispnor.pethub.catalog.application.dto;

import com.alispnor.pethub.catalog.domain.entity.Origem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProdutoDetailResponse(
        Long id,
        String sku,
        String nome,
        String descricaoCurta,
        String descricaoCompleta,
        String marca,
        String ncm,
        Origem origem,
        BigDecimal preco,
        BigDecimal pesoKg,
        BigDecimal alturaCm,
        BigDecimal larguraCm,
        BigDecimal profundidadeCm,
        Map<String, Object> specs,
        List<ProdutoImagemResponse> imagens,
        CategoriaResponse categoria,
        boolean ativo,
        boolean destacado,
        String videoUrl
) {
}
```

- [ ] **Step 4: Build catalog**

```bash
cd /home/ali/projects/pet-hub
docker run --rm -v "$PWD/backend:/work" -v "$HOME/.m2:/root/.m2" \
  -w /work maven:3.9-eclipse-temurin-21 mvn -q -DskipTests -pl catalog -am package
```

Expected: build vai falhar com erro no `ProdutoService` e `ProdutoMapper` porque ainda não atribuem `videoUrl`. **Esperado** — corrige na Task 5.

- [ ] **Step 5: Commit**

```bash
cd /home/ali/projects/pet-hub
git add backend/catalog/src/main/java/com/alispnor/pethub/catalog/domain/entity/Produto.java \
        backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/dto/ProdutoRequest.java \
        backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/dto/ProdutoDetailResponse.java
git commit -m "feat(backend): slice2 04-produto entity and DTOs gain videoUrl field"
```

---

## Task 5: ProdutoService + Mapper wiring para videoUrl

**Files:**
- Modify: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/usecase/ProdutoService.java`

- [ ] **Step 1: Setar `videoUrl` no método `criar`**

Em `criar()`, no `Produto.builder()` (linha ~81), adicionar antes de `.destacado(...)`:

```java
                .videoUrl(request.videoUrl())
```

Para que fique assim:

```java
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
```

- [ ] **Step 2: Setar `videoUrl` no método `atualizar`**

Em `atualizar()`, antes de `produto.setDestacado(...)`, adicionar:

```java
        produto.setVideoUrl(request.videoUrl());
```

- [ ] **Step 3: Build catalog**

```bash
cd /home/ali/projects/pet-hub
docker run --rm -v "$PWD/backend:/work" -v "$HOME/.m2:/root/.m2" \
  -w /work maven:3.9-eclipse-temurin-21 mvn -q -DskipTests -pl catalog -am package
```

Expected: build verde — `ProdutoMapper` mapeia `videoUrl → videoUrl` automaticamente (mesmo nome no entity e no response).

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/usecase/ProdutoService.java
git commit -m "feat(backend): slice2 05-produto service persists videoUrl on create and update"
```

---

## Task 6: ProdutoAdminSummaryResponse + repositório admin

**Files:**
- Create: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/dto/ProdutoAdminSummaryResponse.java`
- Create: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/infrastructure/persistence/ProdutoAdminRepository.java`

- [ ] **Step 1: Criar `ProdutoAdminSummaryResponse.java`**

Caminho: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/dto/ProdutoAdminSummaryResponse.java`

```java
package com.alispnor.pethub.catalog.application.dto;

import java.math.BigDecimal;

public record ProdutoAdminSummaryResponse(
        Long id,
        String sku,
        String nome,
        String marca,
        BigDecimal preco,
        Long categoriaId,
        String categoriaNome,
        String categoriaSlug,
        String imagemPrincipal,
        int totalImagens,
        boolean destacado,
        boolean ativo,
        boolean temVideo
) {
}
```

- [ ] **Step 2: Criar `ProdutoAdminRepository.java`**

Caminho: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/infrastructure/persistence/ProdutoAdminRepository.java`

```java
package com.alispnor.pethub.catalog.infrastructure.persistence;

import com.alispnor.pethub.catalog.domain.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoAdminRepository extends JpaRepository<Produto, Long> {

    @Query("""
            SELECT p FROM Produto p
            WHERE (:q IS NULL OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :q, '%'))
                                  OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%'))
                                  OR LOWER(p.marca) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:categoriaId IS NULL OR p.categoria.id = :categoriaId)
              AND (:ativo IS NULL OR p.ativo = :ativo)
            """)
    Page<Produto> filtrar(@Param("q") String query,
                          @Param("categoriaId") Long categoriaId,
                          @Param("ativo") Boolean ativo,
                          Pageable pageable);
}
```

- [ ] **Step 3: Build catalog**

```bash
cd /home/ali/projects/pet-hub
docker run --rm -v "$PWD/backend:/work" -v "$HOME/.m2:/root/.m2" \
  -w /work maven:3.9-eclipse-temurin-21 mvn -q -DskipTests -pl catalog -am package
```

Expected: build verde.

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/dto/ProdutoAdminSummaryResponse.java \
        backend/catalog/src/main/java/com/alispnor/pethub/catalog/infrastructure/persistence/ProdutoAdminRepository.java
git commit -m "feat(backend): slice2 06-add admin product summary DTO and repository"
```

---

## Task 7: Mapper toAdminSummary + service listarAdmin

**Files:**
- Modify: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/mapper/ProdutoMapper.java`
- Modify: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/usecase/ProdutoService.java`

- [ ] **Step 1: Adicionar `toAdminSummary` em `ProdutoMapper.java`**

Substituir o conteúdo do arquivo por:

```java
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
```

- [ ] **Step 2: Adicionar dependência `ProdutoAdminRepository` em `ProdutoService.java`**

No topo dos imports, adicionar:

```java
import com.alispnor.pethub.catalog.infrastructure.persistence.ProdutoAdminRepository;
```

No campo `private final ProdutoRepository produtoRepository;` adicionar logo abaixo:

```java
    private final ProdutoAdminRepository produtoAdminRepository;
```

- [ ] **Step 3: Adicionar método `listarAdmin` em `ProdutoService.java`**

Adicionar antes do método `buscarPorSku`:

```java
    @Transactional(readOnly = true)
    public Page<ProdutoAdminSummaryResponse> listarAdmin(String query, Long categoriaId, Boolean ativo, Pageable pageable) {
        log.debug("Iniciando listarAdmin q={} categoriaId={} ativo={} pageable={}", query, categoriaId, ativo, pageable);
        var q = (query == null || query.isBlank()) ? null : query.trim();
        var page = produtoAdminRepository.filtrar(q, categoriaId, ativo, pageable);
        var result = page.map(this::toAdminSummary);
        log.debug("ListarAdmin OK total={}", result.getTotalElements());
        return result;
    }

    private ProdutoAdminSummaryResponse toAdminSummary(Produto produto) {
        var preco = precoVigenteRepository.findVigenteByProduto(produto)
                .map(PrecoVigente::getValorBase)
                .orElse(BigDecimal.ZERO);
        return produtoMapper.toAdminSummary(produto, preco);
    }
```

Adicionar import no topo da classe:

```java
import com.alispnor.pethub.catalog.application.dto.ProdutoAdminSummaryResponse;
```

- [ ] **Step 4: Build catalog**

```bash
cd /home/ali/projects/pet-hub
docker run --rm -v "$PWD/backend:/work" -v "$HOME/.m2:/root/.m2" \
  -w /work maven:3.9-eclipse-temurin-21 mvn -q -DskipTests -pl catalog -am package
```

Expected: build verde.

- [ ] **Step 5: Commit**

```bash
cd /home/ali/projects/pet-hub
git add backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/mapper/ProdutoMapper.java \
        backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/usecase/ProdutoService.java
git commit -m "feat(backend): slice2 07-mapper toAdminSummary and service listarAdmin"
```

---

## Task 8: ProdutoController — GET /admin/catalog/products

**Files:**
- Modify: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/infrastructure/rest/ProdutoController.java`

- [ ] **Step 1: Adicionar endpoint admin de listagem**

Em `ProdutoController.java`, adicionar import no topo:

```java
import com.alispnor.pethub.catalog.application.dto.ProdutoAdminSummaryResponse;
```

Adicionar o endpoint **antes** de `@PostMapping("/api/v1/admin/catalog/products")` (linha ~65):

```java
    @GetMapping("/api/v1/admin/catalog/products")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE','OPERADOR')")
    @Operation(summary = "Lista produtos para o admin (incluindo inativos) com filtros e ordenação")
    public PageableResponse<ProdutoAdminSummaryResponse> listarAdmin(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean ativo,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return PageableResponse.of(produtoService.listarAdmin(q, categoriaId, ativo, pageable));
    }
```

- [ ] **Step 2: Build catalog**

```bash
cd /home/ali/projects/pet-hub
docker run --rm -v "$PWD/backend:/work" -v "$HOME/.m2:/root/.m2" \
  -w /work maven:3.9-eclipse-temurin-21 mvn -q -DskipTests -pl catalog -am package
```

Expected: build verde.

- [ ] **Step 3: Commit**

```bash
cd /home/ali/projects/pet-hub
git add backend/catalog/src/main/java/com/alispnor/pethub/catalog/infrastructure/rest/ProdutoController.java
git commit -m "feat(backend): slice2 08-admin endpoint to list products with filters"
```

---

## Task 9: ProdutoController + Service — upload multipart de imagem

**Files:**
- Modify: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/usecase/ProdutoService.java`
- Modify: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/infrastructure/rest/ProdutoController.java`

- [ ] **Step 1: Injetar `PhotoStorage` no `ProdutoService`**

Adicionar import:

```java
import com.alispnor.pethub.common.storage.PhotoStorage;
import org.springframework.web.multipart.MultipartFile;
```

Adicionar campo após `private final ProdutoMapper produtoMapper;`:

```java
    private final PhotoStorage photoStorage;
```

- [ ] **Step 2: Adicionar método `adicionarImagemUpload` em `ProdutoService.java`**

Adicionar após o método `adicionarImagem(...)`:

```java
    @Transactional
    public ProdutoImagemResponse adicionarImagemUpload(Long id, MultipartFile file, boolean principal) {
        log.debug("Iniciando adicionarImagemUpload produto id={} principal={}", id, principal);
        var produto = obrigatorio(id);
        var url = photoStorage.store("products", produto.getId(), file);
        if (principal) {
            produto.getImagens().forEach(img -> img.setPrincipal(false));
        }
        var nova = ProdutoImagem.builder()
                .produto(produto)
                .url(url)
                .ordem(produto.getImagens().size())
                .principal(principal || produto.getImagens().isEmpty())
                .build();
        produto.getImagens().add(nova);
        produtoRepository.save(produto);
        var response = produtoMapper.toImagemResponse(nova);
        log.debug("Imagem (upload) adicionada produtoId={} imagemId={} url={}", id, nova.getId(), url);
        return response;
    }
```

- [ ] **Step 3: Adicionar endpoint multipart em `ProdutoController.java`**

Adicionar imports no topo:

```java
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
```

Adicionar endpoint após o `adicionarImagem` existente (que recebe `AdicionarImagemRequest`):

```java
    @PostMapping(value = "/api/v1/admin/catalog/products/{id}/images/upload",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")
    @Operation(summary = "Upload de imagem (multipart, campo `file`)")
    public ResponseEntity<ProdutoImagemResponse> uploadImagem(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(produtoService.adicionarImagemUpload(id, file, principal));
    }
```

- [ ] **Step 4: Build catalog**

```bash
cd /home/ali/projects/pet-hub
docker run --rm -v "$PWD/backend:/work" -v "$HOME/.m2:/root/.m2" \
  -w /work maven:3.9-eclipse-temurin-21 mvn -q -DskipTests -pl catalog -am package
```

Expected: build verde.

- [ ] **Step 5: Commit**

```bash
cd /home/ali/projects/pet-hub
git add backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/usecase/ProdutoService.java \
        backend/catalog/src/main/java/com/alispnor/pethub/catalog/infrastructure/rest/ProdutoController.java
git commit -m "feat(backend): slice2 09-multipart image upload endpoint for produtos"
```

---

## Task 10: CategoriaService + Controller — endpoint restaurar

**Files:**
- Modify: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/usecase/CategoriaService.java`
- Modify: `backend/catalog/src/main/java/com/alispnor/pethub/catalog/infrastructure/rest/CategoriaController.java`

- [ ] **Step 1: Adicionar método `restaurar` em `CategoriaService.java`**

Adicionar após o método `desativar(...)`:

```java
    @Transactional
    public CategoriaResponse restaurar(Long id) {
        log.debug("Iniciando restaurar categoria id={}", id);
        var categoria = obrigatorio(id);
        categoria.setAtivo(true);
        var response = categoriaMapper.toResponse(categoriaRepository.save(categoria));
        log.debug("Categoria restaurada id={}", id);
        return response;
    }
```

- [ ] **Step 2: Adicionar endpoint em `CategoriaController.java`**

Adicionar import:

```java
import org.springframework.web.bind.annotation.PostMapping;
```

(Já está importado — verificar; senão adicionar.)

Adicionar endpoint após `desativar`:

```java
    @PostMapping("/api/v1/admin/catalog/categories/{id}/restaurar")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")
    @Operation(summary = "Restaura categoria desativada (soft delete reverso)")
    public CategoriaResponse restaurar(@PathVariable Long id) {
        return categoriaService.restaurar(id);
    }
```

- [ ] **Step 3: Build catalog**

```bash
cd /home/ali/projects/pet-hub
docker run --rm -v "$PWD/backend:/work" -v "$HOME/.m2:/root/.m2" \
  -w /work maven:3.9-eclipse-temurin-21 mvn -q -DskipTests -pl catalog -am package
```

Expected: build verde.

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add backend/catalog/src/main/java/com/alispnor/pethub/catalog/application/usecase/CategoriaService.java \
        backend/catalog/src/main/java/com/alispnor/pethub/catalog/infrastructure/rest/CategoriaController.java
git commit -m "feat(backend): slice2 10-categoria restaurar endpoint"
```

---

## Task 11: Backend build full + smoke E2E

**Files:** sem novos arquivos — só validar e atualizar JAR rodando.

- [ ] **Step 1: Full reactor build**

```bash
cd /home/ali/projects/pet-hub
docker run --rm -v "$PWD/backend:/work" -v "$HOME/.m2:/root/.m2" \
  -w /work maven:3.9-eclipse-temurin-21 mvn -q -DskipTests package
```

Expected: build verde em todos os módulos.

- [ ] **Step 2: Reiniciar backend container com novo JAR + verificar saúde**

```bash
docker rm -f pethub-backend 2>/dev/null || true
docker run -d --name pethub-backend --network host --env-file .env.local \
  -v "$PWD/backend/application/target/pet-hub-backend.jar:/app/app.jar:ro" \
  -v "$PWD/backend/application/var:/app/var" \
  -w /app eclipse-temurin:21-jre java -jar /app/app.jar

sleep 20 && curl -s -o /dev/null -w "backend health: %{http_code}\n" http://localhost:8080/actuator/health
```

Expected: backend health 200. Logs mostram aplicar V18.

- [ ] **Step 3: Smoke — coluna existe no Postgres**

```bash
docker exec -i pethub-postgres psql -U pethub -d pethub -c "\d produtos" | grep video_url
```

Expected: linha `video_url | character varying(500) |`.

- [ ] **Step 4: Smoke — login admin**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@pethub.com","senha":"Admin@123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
echo "TOKEN: ${TOKEN:0:30}..."
```

Expected: token impresso (não vazio).

- [ ] **Step 5: Smoke — listar produtos admin**

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/admin/catalog/products?page=0&size=5" | python3 -m json.tool | head -30
```

Expected: JSON `{ "content": [{ "id":..., "sku":..., "categoriaNome":..., "ativo":true, "temVideo":false, ... }], "totalElements":20, ... }`.

- [ ] **Step 6: Smoke — criar produto com videoUrl válida**

```bash
CATEGORIA_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/admin/catalog/categories" | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['id'])")

curl -s -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"sku\":\"TEST-VIDEO-01\",\"nome\":\"Teste vídeo\",\"categoriaId\":$CATEGORIA_ID,\"pesoKg\":1.0,\"ncm\":\"42010000\",\"origem\":\"NACIONAL\",\"precoInicial\":99.90,\"videoUrl\":\"https://www.youtube.com/watch?v=dQw4w9WgXcQ\"}" \
  "http://localhost:8080/api/v1/admin/catalog/products" | python3 -m json.tool | head -20
```

Expected: 201 com `"videoUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"`.

- [ ] **Step 7: Smoke — criar com videoUrl inválida → 422**

```bash
curl -s -o /dev/null -w "videoUrl invalida: %{http_code}\n" \
  -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"sku\":\"TEST-VIDEO-02\",\"nome\":\"X\",\"categoriaId\":$CATEGORIA_ID,\"pesoKg\":1.0,\"ncm\":\"42010000\",\"origem\":\"NACIONAL\",\"precoInicial\":99.90,\"videoUrl\":\"https://malicious.example.com/payload\"}" \
  "http://localhost:8080/api/v1/admin/catalog/products"
```

Expected: `videoUrl invalida: 400` (ou 422 dependendo do handler — qualquer 4xx).

- [ ] **Step 8: Smoke — upload multipart**

Criar uma imagem JPG de 1 pixel pra teste:

```bash
PRODUTO_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/admin/catalog/products?q=TEST-VIDEO-01" | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['content'][0]['id'])")

printf '\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x01\x00\x48\x00\x48\x00\x00\xff\xd9' > /tmp/test-pixel.jpg

curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -F "file=@/tmp/test-pixel.jpg;type=image/jpeg" -F "principal=true" \
  "http://localhost:8080/api/v1/admin/catalog/products/$PRODUTO_ID/images/upload" | python3 -m json.tool
```

Expected: 201 com `{"id":..., "url":"http://localhost:8080/files/products/...","principal":true,...}`.

Verificar arquivo no disco:

```bash
ls /home/ali/projects/pet-hub/backend/application/var/uploads/products/ | tail -3
```

Expected: arquivo `products-<id>-<uuid>.jpg` presente.

- [ ] **Step 9: Smoke — desativar e restaurar categoria**

```bash
TEMP_CAT=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"nome":"Temp Test","slug":"temp-test-slice2","ordem":99}' \
  "http://localhost:8080/api/v1/admin/catalog/categories" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

curl -s -o /dev/null -w "desativar: %{http_code}\n" \
  -X DELETE -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/admin/catalog/categories/$TEMP_CAT"

curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/admin/catalog/categories/$TEMP_CAT/restaurar" | python3 -m json.tool | grep ativo
```

Expected: `desativar: 204` e depois `"ativo": true`.

- [ ] **Step 10: Limpar dados de teste**

```bash
docker exec -i pethub-postgres psql -U pethub -d pethub <<'SQL'
DELETE FROM produto_imagens WHERE produto_id IN (SELECT id FROM produtos WHERE sku LIKE 'TEST-VIDEO-%');
DELETE FROM precos_vigentes WHERE produto_id IN (SELECT id FROM produtos WHERE sku LIKE 'TEST-VIDEO-%');
DELETE FROM produtos WHERE sku LIKE 'TEST-VIDEO-%';
DELETE FROM categorias WHERE slug = 'temp-test-slice2';
SQL
```

- [ ] **Step 11: Commit (somente se algo precisar registrar; caso contrário pular)**

Não há arquivos novos nesta task — só smoke. Pular commit, mas marcar a task como concluída.

---

## Task 12: Frontend admin — models catalog

**Files:**
- Create: `frontend/admin/src/app/modules/catalog/models/produto.ts`
- Create: `frontend/admin/src/app/modules/catalog/models/categoria.ts`

- [ ] **Step 1: Criar pasta de modules/catalog**

```bash
mkdir -p /home/ali/projects/pet-hub/frontend/admin/src/app/modules/catalog/models \
         /home/ali/projects/pet-hub/frontend/admin/src/app/modules/catalog/services \
         /home/ali/projects/pet-hub/frontend/admin/src/app/modules/catalog/pages/produtos-lista \
         /home/ali/projects/pet-hub/frontend/admin/src/app/modules/catalog/pages/produto-form \
         /home/ali/projects/pet-hub/frontend/admin/src/app/modules/catalog/pages/categorias \
         /home/ali/projects/pet-hub/frontend/admin/src/app/modules/catalog/components/image-drop-zone \
         /home/ali/projects/pet-hub/frontend/admin/src/app/modules/catalog/components/video-url-input \
         /home/ali/projects/pet-hub/frontend/admin/src/app/shared/pipes \
         /home/ali/projects/pet-hub/frontend/admin/src/app/shared/utils
```

- [ ] **Step 2: Criar `produto.ts`**

Caminho: `frontend/admin/src/app/modules/catalog/models/produto.ts`

```ts
export type Origem = 'NACIONAL' | 'IMPORTADO_DIRETO' | 'IMPORTADO_INDIRETO';

export const ORIGEM_LABEL: Record<Origem, string> = {
  NACIONAL: 'Nacional',
  IMPORTADO_DIRETO: 'Importado (direto)',
  IMPORTADO_INDIRETO: 'Importado (indireto)',
};

export const ORIGEM_VALUES: Origem[] = ['NACIONAL', 'IMPORTADO_DIRETO', 'IMPORTADO_INDIRETO'];

export interface ProdutoImagem {
  id: number;
  url: string;
  ordem: number;
  principal: boolean;
}

export interface CategoriaEmbedida {
  id: number;
  nome: string;
  slug: string;
  descricao: string | null;
  categoriaPaiId: number | null;
  ativo: boolean;
  ordem: number;
}

export interface ProdutoAdminSummary {
  id: number;
  sku: string;
  nome: string;
  marca: string | null;
  preco: number;
  categoriaId: number;
  categoriaNome: string;
  categoriaSlug: string;
  imagemPrincipal: string | null;
  totalImagens: number;
  destacado: boolean;
  ativo: boolean;
  temVideo: boolean;
}

export interface ProdutoDetail {
  id: number;
  sku: string;
  nome: string;
  descricaoCurta: string | null;
  descricaoCompleta: string | null;
  marca: string | null;
  ncm: string;
  origem: Origem;
  preco: number;
  pesoKg: number;
  alturaCm: number | null;
  larguraCm: number | null;
  profundidadeCm: number | null;
  specs: Record<string, unknown> | null;
  imagens: ProdutoImagem[];
  categoria: CategoriaEmbedida;
  ativo: boolean;
  destacado: boolean;
  videoUrl: string | null;
}

export interface ProdutoRequest {
  sku: string;
  nome: string;
  descricaoCurta?: string | null;
  descricaoCompleta?: string | null;
  marca?: string | null;
  categoriaId: number;
  pesoKg: number;
  alturaCm?: number | null;
  larguraCm?: number | null;
  profundidadeCm?: number | null;
  ncm: string;
  origem: Origem;
  specs?: Record<string, unknown> | null;
  destacado: boolean;
  precoInicial: number;
  videoUrl?: string | null;
}

export const REGEX_VIDEO_URL =
  /^https:\/\/(www\.)?(youtube\.com\/watch\?v=[\w-]+|youtu\.be\/[\w-]+|vimeo\.com\/\d+)([&?][\w-=&]*)?$/;

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
```

- [ ] **Step 3: Criar `categoria.ts`**

Caminho: `frontend/admin/src/app/modules/catalog/models/categoria.ts`

```ts
export interface CategoriaResponse {
  id: number;
  nome: string;
  slug: string;
  descricao: string | null;
  categoriaPaiId: number | null;
  ativo: boolean;
  ordem: number;
}

export interface CategoriaRequest {
  nome: string;
  slug: string;
  descricao?: string | null;
  categoriaPaiId?: number | null;
  ordem: number;
}

export interface CategoriaArvoreNode {
  categoria: CategoriaResponse;
  filhas: CategoriaArvoreNode[];
  nivel: number;
}

/**
 * Constrói árvore a partir de lista plana. Categorias órfãs (pai não na lista
 * — ex: pai desativado) viram raízes pra não sumir da UI.
 */
export function construirArvore(planas: CategoriaResponse[]): CategoriaArvoreNode[] {
  const indice = new Map<number, CategoriaArvoreNode>();
  planas.forEach(categoria =>
    indice.set(categoria.id, { categoria, filhas: [], nivel: 0 }),
  );

  const raizes: CategoriaArvoreNode[] = [];
  planas.forEach(categoria => {
    const node = indice.get(categoria.id)!;
    if (categoria.categoriaPaiId && indice.has(categoria.categoriaPaiId)) {
      const pai = indice.get(categoria.categoriaPaiId)!;
      node.nivel = pai.nivel + 1;
      pai.filhas.push(node);
    } else {
      raizes.push(node);
    }
  });

  const ordenarRecursivamente = (nodes: CategoriaArvoreNode[]) => {
    nodes.sort((a, b) => a.categoria.ordem - b.categoria.ordem
                          || a.categoria.nome.localeCompare(b.categoria.nome));
    nodes.forEach(node => ordenarRecursivamente(node.filhas));
  };
  ordenarRecursivamente(raizes);
  return raizes;
}

/**
 * Achata árvore em lista plana mantendo ordem DFS — usado pra renderizar
 * a lista indentada e pro `<select>` de pai.
 */
export function achatarArvore(raizes: CategoriaArvoreNode[]): CategoriaArvoreNode[] {
  const resultado: CategoriaArvoreNode[] = [];
  const visitar = (node: CategoriaArvoreNode) => {
    resultado.push(node);
    node.filhas.forEach(visitar);
  };
  raizes.forEach(visitar);
  return resultado;
}

/**
 * IDs do node + descendentes — usado pra filtrar `<select>` de pai
 * (impede ciclo no client antes do backend rejeitar).
 */
export function descendentesId(node: CategoriaArvoreNode): Set<number> {
  const conjunto = new Set<number>();
  const visitar = (atual: CategoriaArvoreNode) => {
    conjunto.add(atual.categoria.id);
    atual.filhas.forEach(visitar);
  };
  visitar(node);
  return conjunto;
}
```

- [ ] **Step 4: Build admin**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build verde (modelos puros, sem dependências de outras camadas).

- [ ] **Step 5: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/modules/catalog/models
git commit -m "feat(admin): slice2 11-catalog produto and categoria models"
```

---

## Task 13: Frontend admin — services catalog

**Files:**
- Create: `frontend/admin/src/app/modules/catalog/services/produto.service.ts`
- Create: `frontend/admin/src/app/modules/catalog/services/categoria.service.ts`

- [ ] **Step 1: Criar `produto.service.ts`**

Caminho: `frontend/admin/src/app/modules/catalog/services/produto.service.ts`

```ts
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
import {
  PageResponse,
  ProdutoAdminSummary,
  ProdutoDetail,
  ProdutoImagem,
  ProdutoRequest,
} from '@modules/catalog/models/produto';

export interface FiltrosListagem {
  q?: string;
  categoriaId?: number;
  ativo?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ProdutoService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  listar(filtros: FiltrosListagem = {}): Observable<PageResponse<ProdutoAdminSummary>> {
    let params = new HttpParams();
    if (filtros.q) params = params.set('q', filtros.q);
    if (filtros.categoriaId !== undefined) params = params.set('categoriaId', filtros.categoriaId);
    if (filtros.ativo !== undefined) params = params.set('ativo', filtros.ativo);
    if (filtros.page !== undefined) params = params.set('page', filtros.page);
    if (filtros.size !== undefined) params = params.set('size', filtros.size);
    if (filtros.sort) params = params.set('sort', filtros.sort);
    return this.http.get<PageResponse<ProdutoAdminSummary>>(
      `${this.api}/admin/catalog/products`,
      { params },
    );
  }

  buscarPorSku(sku: string): Observable<ProdutoDetail> {
    return this.http.get<ProdutoDetail>(`${this.api}/catalog/products/${sku}`);
  }

  criar(req: ProdutoRequest): Observable<ProdutoDetail> {
    return this.http.post<ProdutoDetail>(`${this.api}/admin/catalog/products`, req);
  }

  atualizar(id: number, req: ProdutoRequest): Observable<ProdutoDetail> {
    return this.http.put<ProdutoDetail>(`${this.api}/admin/catalog/products/${id}`, req);
  }

  atualizarPreco(id: number, valor: number): Observable<ProdutoDetail> {
    return this.http.post<ProdutoDetail>(
      `${this.api}/admin/catalog/products/${id}/price`,
      { valor },
    );
  }

  toggleAtivo(id: number): Observable<ProdutoDetail> {
    return this.http.patch<ProdutoDetail>(`${this.api}/admin/catalog/products/${id}/ativo`, {});
  }

  uploadImagem(id: number, file: File, principal: boolean): Observable<ProdutoImagem> {
    const formData = new FormData();
    formData.append('file', file);
    let params = new HttpParams().set('principal', String(principal));
    return this.http.post<ProdutoImagem>(
      `${this.api}/admin/catalog/products/${id}/images/upload`,
      formData,
      { params },
    );
  }

  removerImagem(produtoId: number, imagemId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.api}/admin/catalog/products/${produtoId}/images/${imagemId}`,
    );
  }
}
```

- [ ] **Step 2: Criar `categoria.service.ts`**

Caminho: `frontend/admin/src/app/modules/catalog/services/categoria.service.ts`

```ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
import { CategoriaRequest, CategoriaResponse } from '@modules/catalog/models/categoria';

@Injectable({ providedIn: 'root' })
export class CategoriaService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  listar(): Observable<CategoriaResponse[]> {
    return this.http.get<CategoriaResponse[]>(`${this.api}/admin/catalog/categories`);
  }

  criar(req: CategoriaRequest): Observable<CategoriaResponse> {
    return this.http.post<CategoriaResponse>(`${this.api}/admin/catalog/categories`, req);
  }

  atualizar(id: number, req: CategoriaRequest): Observable<CategoriaResponse> {
    return this.http.put<CategoriaResponse>(`${this.api}/admin/catalog/categories/${id}`, req);
  }

  desativar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/admin/catalog/categories/${id}`);
  }

  restaurar(id: number): Observable<CategoriaResponse> {
    return this.http.post<CategoriaResponse>(
      `${this.api}/admin/catalog/categories/${id}/restaurar`,
      {},
    );
  }
}
```

- [ ] **Step 3: Build admin**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build verde.

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/modules/catalog/services
git commit -m "feat(admin): slice2 12-catalog produto and categoria services"
```

---

## Task 14: Frontend admin — helper video-embed + pipe safe-resource-url

**Files:**
- Create: `frontend/admin/src/app/shared/utils/video-embed.ts`
- Create: `frontend/admin/src/app/shared/pipes/safe-resource-url.pipe.ts`

- [ ] **Step 1: Criar `video-embed.ts`**

Caminho: `frontend/admin/src/app/shared/utils/video-embed.ts`

```ts
/**
 * Converte URL pública (YouTube watch ou Vimeo) na URL de embed.
 * Retorna null se a URL não casa com nenhum padrão whitelisted.
 */
export function urlEmbed(urlPublica: string | null | undefined): string | null {
  if (!urlPublica) return null;

  const youtubeWatch = urlPublica.match(/youtube\.com\/watch\?v=([\w-]+)/);
  if (youtubeWatch) return `https://www.youtube.com/embed/${youtubeWatch[1]}`;

  const youtubeShort = urlPublica.match(/youtu\.be\/([\w-]+)/);
  if (youtubeShort) return `https://www.youtube.com/embed/${youtubeShort[1]}`;

  const vimeo = urlPublica.match(/vimeo\.com\/(\d+)/);
  if (vimeo) return `https://player.vimeo.com/video/${vimeo[1]}`;

  return null;
}
```

- [ ] **Step 2: Criar `safe-resource-url.pipe.ts`**

Caminho: `frontend/admin/src/app/shared/pipes/safe-resource-url.pipe.ts`

```ts
import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Pipe({ name: 'safeResourceUrl', standalone: true })
export class SafeResourceUrlPipe implements PipeTransform {
  private readonly sanitizer = inject(DomSanitizer);

  transform(value: string | null | undefined): SafeResourceUrl | null {
    if (!value) return null;
    return this.sanitizer.bypassSecurityTrustResourceUrl(value);
  }
}
```

- [ ] **Step 3: Build admin**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build verde.

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/shared/utils/video-embed.ts \
        frontend/admin/src/app/shared/pipes/safe-resource-url.pipe.ts
git commit -m "feat(admin): slice2 13-video embed helper and safe-resource-url pipe"
```

---

## Task 15: VideoUrlInputComponent

**Files:**
- Create: `frontend/admin/src/app/modules/catalog/components/video-url-input/video-url-input.component.ts`

- [ ] **Step 1: Criar `video-url-input.component.ts`**

Caminho: `frontend/admin/src/app/modules/catalog/components/video-url-input/video-url-input.component.ts`

```ts
import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';

import { REGEX_VIDEO_URL } from '@modules/catalog/models/produto';
import { urlEmbed } from '@shared/utils/video-embed';
import { SafeResourceUrlPipe } from '@shared/pipes/safe-resource-url.pipe';

@Component({
  selector: 'app-video-url-input',
  standalone: true,
  imports: [CommonModule, SafeResourceUrlPipe],
  template: `
    <label class="block text-sm font-medium text-graphite-700" for="video-url">
      URL do vídeo <span class="text-graphite-500">(YouTube ou Vimeo, opcional)</span>
    </label>
    <input id="video-url" type="url" [value]="valor()"
           (input)="aoMudar(asTarget($event).value)"
           placeholder="https://www.youtube.com/watch?v=..."
           class="mt-1 w-full rounded-md border-graphite-300" />
    <p *ngIf="erroValidacao()" class="text-xs text-red-600 mt-1">{{ erroValidacao() }}</p>
    <p *ngIf="!erroValidacao() && valor()" class="text-xs text-graphite-500 mt-1">
      Preview abaixo. Em produtos públicos o vídeo aparece no detalhe do produto.
    </p>
    <div *ngIf="urlEmbedComputado() as embed" class="mt-3 aspect-video rounded-lg overflow-hidden bg-graphite-100">
      <iframe [src]="embed | safeResourceUrl"
              sandbox="allow-scripts allow-same-origin allow-presentation"
              referrerpolicy="strict-origin-when-cross-origin"
              class="w-full h-full" allowfullscreen
              title="Preview do vídeo"></iframe>
    </div>
  `,
})
export class VideoUrlInputComponent {
  private readonly _valor = signal<string>('');

  @Input() set valorInicial(v: string | null | undefined) {
    this._valor.set(v ?? '');
  }
  @Output() valorChange = new EventEmitter<string | null>();

  readonly valor = this._valor.asReadonly();
  readonly erroValidacao = computed(() => {
    const atual = this._valor();
    if (!atual) return null;
    return REGEX_VIDEO_URL.test(atual)
      ? null
      : 'URL inválida. Apenas YouTube (watch?v=...) ou Vimeo são aceitos.';
  });
  readonly urlEmbedComputado = computed(() =>
    this.erroValidacao() ? null : urlEmbed(this._valor()),
  );

  aoMudar(novo: string): void {
    this._valor.set(novo);
    const valorFinal = novo.trim() === '' ? null : novo.trim();
    this.valorChange.emit(REGEX_VIDEO_URL.test(novo) || valorFinal === null ? valorFinal : valorFinal);
  }

  asTarget(event: Event): HTMLInputElement {
    return event.target as HTMLInputElement;
  }
}
```

- [ ] **Step 2: Build admin**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build verde.

- [ ] **Step 3: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/modules/catalog/components/video-url-input
git commit -m "feat(admin): slice2 14-video url input component with sandboxed preview"
```

---

## Task 16: ImageDropZoneComponent (multipart upload)

**Files:**
- Create: `frontend/admin/src/app/modules/catalog/components/image-drop-zone/image-drop-zone.component.ts`

- [ ] **Step 1: Criar `image-drop-zone.component.ts`**

Caminho: `frontend/admin/src/app/modules/catalog/components/image-drop-zone/image-drop-zone.component.ts`

```ts
import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { from, of } from 'rxjs';
import { catchError, concatMap, tap } from 'rxjs/operators';

import { ProdutoImagem } from '@modules/catalog/models/produto';
import { ProdutoService } from '@modules/catalog/services/produto.service';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';
import { ToastService } from '@shared/services/toast.service';

const TAMANHO_MAXIMO_BYTES = 5 * 1024 * 1024;
const TIPOS_PERMITIDOS = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif']);

@Component({
  selector: 'app-image-drop-zone',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="rounded-lg border-2 border-dashed p-6 text-center transition-colors"
         [class.border-coral-500]="arrastando()"
         [class.bg-coral-50]="arrastando()"
         [class.border-graphite-300]="!arrastando()"
         (dragover)="onDragOver($event)"
         (dragleave)="onDragLeave()"
         (drop)="onDrop($event)">
      <p class="text-sm text-graphite-600">Arraste imagens aqui ou</p>
      <label class="mt-2 btn-ghost text-sm py-1.5 px-3 cursor-pointer inline-block">
        Escolher arquivos
        <input type="file" accept="image/*" multiple class="hidden"
               (change)="onSelecionarArquivos($event)" />
      </label>
      <p class="mt-2 text-xs text-graphite-500">JPG/PNG/WEBP/GIF, máx 5 MB cada</p>
      <p *ngIf="enviando()" class="mt-2 text-xs text-coral-700">
        Enviando {{ totalEnviando() }} {{ totalEnviando() === 1 ? 'arquivo' : 'arquivos' }}...
      </p>
    </div>

    <div *ngIf="imagens.length > 0" class="mt-4 grid grid-cols-3 md:grid-cols-4 gap-3">
      <div *ngFor="let img of imagens"
           class="relative aspect-square rounded-md overflow-hidden border border-graphite-200">
        <img [src]="img.url" alt="" class="w-full h-full object-cover" />
        <button type="button" (click)="remover(img)"
                class="absolute top-1 right-1 bg-red-600 text-white rounded-full w-6 h-6
                       flex items-center justify-center text-xs"
                aria-label="Remover imagem">✕</button>
        <span *ngIf="img.principal"
              class="absolute bottom-1 left-1 bg-emerald-600 text-white rounded-full
                     px-1.5 py-0.5 text-xs">
          Principal
        </span>
      </div>
    </div>
  `,
})
export class ImageDropZoneComponent {
  private readonly produtoService = inject(ProdutoService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmDialogService);

  @Input() produtoId!: number;
  @Input() imagens: ProdutoImagem[] = [];
  @Output() imagensAtualizadas = new EventEmitter<ProdutoImagem[]>();

  readonly arrastando = signal(false);
  readonly enviando = signal(false);
  readonly totalEnviando = signal(0);

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.arrastando.set(true);
  }

  onDragLeave(): void {
    this.arrastando.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.arrastando.set(false);
    const arquivos = event.dataTransfer?.files;
    if (arquivos) this.processarArquivos(Array.from(arquivos));
  }

  onSelecionarArquivos(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) this.processarArquivos(Array.from(input.files));
    input.value = '';
  }

  remover(img: ProdutoImagem): void {
    this.confirm.open({
      titulo: 'Remover imagem?',
      mensagem: 'A imagem será excluída do produto. Esta ação não pode ser desfeita.',
      acaoLabel: 'Remover',
      acaoVariant: 'danger',
    }).then(confirmado => {
      if (!confirmado) return;
      this.produtoService.removerImagem(this.produtoId, img.id).subscribe({
        next: () => {
          this.imagensAtualizadas.emit(this.imagens.filter(i => i.id !== img.id));
          this.toast.success('Imagem removida.');
        },
        error: () => this.toast.error('Falha ao remover imagem.'),
      });
    });
  }

  private processarArquivos(arquivos: File[]): void {
    const validos = arquivos.filter(file => this.validar(file));
    if (validos.length === 0) return;

    this.enviando.set(true);
    this.totalEnviando.set(validos.length);
    const primeiraPrincipal = this.imagens.length === 0;

    from(validos).pipe(
      concatMap((file, indice) =>
        this.produtoService.uploadImagem(
          this.produtoId,
          file,
          primeiraPrincipal && indice === 0,
        ).pipe(
          tap(novaImagem => {
            this.imagensAtualizadas.emit([...this.imagens, novaImagem]);
            this.totalEnviando.update(atual => Math.max(0, atual - 1));
          }),
          catchError((erro: HttpErrorResponse) => {
            const detail = erro.error?.detail ?? 'Falha ao enviar imagem.';
            this.toast.error(`${file.name}: ${detail}`);
            this.totalEnviando.update(atual => Math.max(0, atual - 1));
            return of(null);
          }),
        ),
      ),
    ).subscribe({
      complete: () => {
        this.enviando.set(false);
        this.totalEnviando.set(0);
        this.toast.success('Upload concluído.');
      },
    });
  }

  private validar(file: File): boolean {
    if (!TIPOS_PERMITIDOS.has(file.type)) {
      this.toast.error(`${file.name}: tipo não suportado.`);
      return false;
    }
    if (file.size > TAMANHO_MAXIMO_BYTES) {
      this.toast.error(`${file.name}: excede 5 MB.`);
      return false;
    }
    return true;
  }
}
```

- [ ] **Step 2: Build admin**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build verde.

- [ ] **Step 3: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/modules/catalog/components/image-drop-zone
git commit -m "feat(admin): slice2 15-image drop zone component with multipart upload"
```

---

## Task 17: ProdutosListaPage

**Files:**
- Create: `frontend/admin/src/app/modules/catalog/pages/produtos-lista/produtos-lista.page.ts`
- Create: `frontend/admin/src/app/modules/catalog/pages/produtos-lista/produtos-lista.page.html`

- [ ] **Step 1: Criar `produtos-lista.page.ts`**

Caminho: `frontend/admin/src/app/modules/catalog/pages/produtos-lista/produtos-lista.page.ts`

```ts
import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { debounceTime } from 'rxjs/operators';

import { CategoriaResponse } from '@modules/catalog/models/categoria';
import {
  PageResponse,
  ProdutoAdminSummary,
} from '@modules/catalog/models/produto';
import { CategoriaService } from '@modules/catalog/services/categoria.service';
import { FiltrosListagem, ProdutoService } from '@modules/catalog/services/produto.service';
import { AdminAuthService } from '@core/services/admin-auth.service';
import { ToastService } from '@shared/services/toast.service';

@Component({
  selector: 'app-produtos-lista-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './produtos-lista.page.html',
})
export class ProdutosListaPage implements OnInit {
  private readonly produtoService = inject(ProdutoService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly auth = inject(AdminAuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly busca = new FormControl<string>('', { nonNullable: true });
  readonly categoriaSelecionada = new FormControl<number | null>(null);
  readonly ativoSelecionado = new FormControl<'todos' | 'ativos' | 'inativos'>('todos', { nonNullable: true });

  readonly pagina = signal<PageResponse<ProdutoAdminSummary> | null>(null);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly ordenacao = signal<string>('nome,asc');

  readonly podeMutar = computed(() =>
    this.auth.isAdmin() || this.auth.isGerente(),
  );

  ngOnInit(): void {
    this.categoriaService.listar().subscribe({
      next: lista => this.categorias.set(lista),
      error: () => this.toast.error('Falha ao carregar categorias.'),
    });

    this.route.queryParamMap.subscribe(params => {
      this.busca.setValue(params.get('q') ?? '', { emitEvent: false });
      const catId = params.get('categoriaId');
      this.categoriaSelecionada.setValue(catId ? Number(catId) : null, { emitEvent: false });
      const ativoParam = params.get('ativo');
      this.ativoSelecionado.setValue(
        ativoParam === 'true' ? 'ativos' : ativoParam === 'false' ? 'inativos' : 'todos',
        { emitEvent: false },
      );
      this.ordenacao.set(params.get('sort') ?? 'nome,asc');
      this.carregar(Number(params.get('page') ?? '0'));
    });

    this.busca.valueChanges.pipe(debounceTime(300)).subscribe(() => this.atualizarUrl(0));
    this.categoriaSelecionada.valueChanges.subscribe(() => this.atualizarUrl(0));
    this.ativoSelecionado.valueChanges.subscribe(() => this.atualizarUrl(0));
  }

  irParaPagina(page: number): void {
    this.atualizarUrl(page);
  }

  ordenarPor(campo: 'nome' | 'sku'): void {
    const atual = this.ordenacao();
    const proximo = atual === `${campo},asc` ? `${campo},desc` : `${campo},asc`;
    this.ordenacao.set(proximo);
    this.atualizarUrl(0);
  }

  toggleAtivoLinha(produto: ProdutoAdminSummary): void {
    if (!this.podeMutar()) return;
    this.produtoService.toggleAtivo(produto.id).subscribe({
      next: atualizado => {
        const paginaAtual = this.pagina();
        if (paginaAtual) {
          const novoConteudo = paginaAtual.content.map(p =>
            p.id === produto.id ? { ...p, ativo: atualizado.ativo } : p,
          );
          this.pagina.set({ ...paginaAtual, content: novoConteudo });
        }
        this.toast.success(atualizado.ativo ? 'Produto ativado.' : 'Produto desativado.');
      },
      error: () => this.toast.error('Falha ao alterar status.'),
    });
  }

  private atualizarUrl(page: number): void {
    const queryParams: Record<string, string | null> = {
      q: this.busca.value || null,
      categoriaId: this.categoriaSelecionada.value !== null ? String(this.categoriaSelecionada.value) : null,
      ativo:
        this.ativoSelecionado.value === 'ativos' ? 'true'
        : this.ativoSelecionado.value === 'inativos' ? 'false'
        : null,
      sort: this.ordenacao(),
      page: page > 0 ? String(page) : null,
    };
    this.router.navigate([], { queryParams, queryParamsHandling: 'merge', replaceUrl: true });
  }

  private carregar(page: number): void {
    this.carregando.set(true);
    this.erro.set(null);
    const filtros: FiltrosListagem = {
      q: this.busca.value || undefined,
      categoriaId: this.categoriaSelecionada.value ?? undefined,
      ativo:
        this.ativoSelecionado.value === 'ativos' ? true
        : this.ativoSelecionado.value === 'inativos' ? false
        : undefined,
      page,
      size: 20,
      sort: this.ordenacao(),
    };
    this.produtoService.listar(filtros).subscribe({
      next: resultado => {
        this.pagina.set(resultado);
        this.carregando.set(false);
      },
      error: (httpError: HttpErrorResponse) => {
        this.carregando.set(false);
        this.erro.set(httpError.status === 0 ? 'Sem conexão com o servidor.' : 'Erro ao carregar produtos.');
      },
    });
  }
}
```

- [ ] **Step 2: Criar `produtos-lista.page.html`**

Caminho: `frontend/admin/src/app/modules/catalog/pages/produtos-lista/produtos-lista.page.html`

```html
<section>
  <header class="flex items-center justify-between gap-4 mb-6">
    <div>
      <h1 class="text-2xl font-display font-semibold text-graphite-900">Produtos</h1>
      <p *ngIf="pagina() as p" class="text-sm text-graphite-500 mt-1">
        {{ p.totalElements }} {{ p.totalElements === 1 ? 'produto' : 'produtos' }} no total
      </p>
    </div>
    <a *ngIf="podeMutar()" routerLink="/produtos/novo" class="btn-primary text-sm py-2 px-4">
      + Novo produto
    </a>
  </header>

  <div class="rounded-lg border border-graphite-200 bg-graphite-0 p-4 mb-6">
    <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
      <div>
        <label class="block text-xs font-medium text-graphite-700 mb-1">Buscar</label>
        <input type="text" [formControl]="busca"
               placeholder="SKU, nome ou marca"
               class="w-full rounded-md border-graphite-300 text-sm" />
      </div>
      <div>
        <label class="block text-xs font-medium text-graphite-700 mb-1">Categoria</label>
        <select [formControl]="categoriaSelecionada"
                class="w-full rounded-md border-graphite-300 text-sm">
          <option [ngValue]="null">Todas</option>
          <option *ngFor="let c of categorias()" [ngValue]="c.id">{{ c.nome }}</option>
        </select>
      </div>
      <div>
        <label class="block text-xs font-medium text-graphite-700 mb-1">Status</label>
        <select [formControl]="ativoSelecionado"
                class="w-full rounded-md border-graphite-300 text-sm">
          <option value="todos">Todos</option>
          <option value="ativos">Ativos</option>
          <option value="inativos">Inativos</option>
        </select>
      </div>
    </div>
  </div>

  <div *ngIf="carregando()" class="space-y-2">
    <div *ngFor="let _ of [1,2,3,4,5]"
         class="h-14 w-full rounded-md bg-graphite-100 animate-pulse"></div>
  </div>

  <div *ngIf="erro() as msg"
       class="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-900">
    {{ msg }}
  </div>

  <ng-container *ngIf="pagina() as p">
    <div *ngIf="!carregando() && p.content.length === 0"
         class="rounded-lg border border-dashed border-graphite-300 bg-graphite-0 p-12 text-center">
      <p class="text-graphite-600">Nenhum produto encontrado com esses filtros.</p>
      <a *ngIf="podeMutar()" routerLink="/produtos/novo"
         class="btn-primary mt-4 inline-flex text-sm py-2 px-4">+ Adicionar primeiro produto</a>
    </div>

    <div *ngIf="!carregando() && p.content.length > 0"
         class="rounded-lg border border-graphite-200 bg-graphite-0 overflow-hidden">
      <table class="w-full text-sm">
        <thead class="border-b border-graphite-200 bg-graphite-50 text-xs uppercase text-graphite-600">
          <tr>
            <th class="text-left px-4 py-3 w-16">Img</th>
            <th class="text-left px-4 py-3">
              <button type="button" (click)="ordenarPor('nome')"
                      class="hover:text-graphite-900">Nome ↕</button>
            </th>
            <th class="text-left px-4 py-3">Categoria</th>
            <th class="text-right px-4 py-3">Preço</th>
            <th class="text-center px-4 py-3">Imagens</th>
            <th class="text-center px-4 py-3">Vídeo</th>
            <th class="text-center px-4 py-3">Status</th>
            <th class="text-right px-4 py-3">Ações</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-graphite-100">
          <tr *ngFor="let prod of p.content"
              [class.opacity-60]="!prod.ativo">
            <td class="px-4 py-3">
              <img *ngIf="prod.imagemPrincipal" [src]="prod.imagemPrincipal" alt=""
                   class="w-12 h-12 rounded object-cover bg-graphite-100" />
              <div *ngIf="!prod.imagemPrincipal"
                   class="w-12 h-12 rounded bg-graphite-100 flex items-center justify-center text-graphite-400 text-xs">
                —
              </div>
            </td>
            <td class="px-4 py-3">
              <div class="font-medium text-graphite-900">{{ prod.nome }}</div>
              <div class="text-xs text-graphite-500 font-mono">{{ prod.sku }}</div>
              <div *ngIf="prod.marca" class="text-xs text-graphite-500">{{ prod.marca }}</div>
            </td>
            <td class="px-4 py-3 text-graphite-700">{{ prod.categoriaNome }}</td>
            <td class="px-4 py-3 text-right text-graphite-900">
              {{ prod.preco | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
            </td>
            <td class="px-4 py-3 text-center text-graphite-700">{{ prod.totalImagens }}</td>
            <td class="px-4 py-3 text-center">
              <span *ngIf="prod.temVideo" class="text-emerald-700 text-xs">▶</span>
              <span *ngIf="!prod.temVideo" class="text-graphite-300 text-xs">—</span>
            </td>
            <td class="px-4 py-3 text-center">
              <span class="inline-block rounded-full px-2 py-0.5 text-xs"
                    [class.bg-emerald-100]="prod.ativo"
                    [class.text-emerald-800]="prod.ativo"
                    [class.bg-graphite-100]="!prod.ativo"
                    [class.text-graphite-700]="!prod.ativo">
                {{ prod.ativo ? 'Ativo' : 'Inativo' }}
              </span>
            </td>
            <td class="px-4 py-3 text-right">
              <a [routerLink]="['/produtos', prod.sku, 'editar']"
                 class="btn-ghost text-xs py-1 px-2">Editar</a>
              <button *ngIf="podeMutar()" type="button"
                      class="btn-ghost text-xs py-1 px-2 ml-1"
                      (click)="toggleAtivoLinha(prod)">
                {{ prod.ativo ? 'Desativar' : 'Ativar' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div *ngIf="p.totalPages > 1"
         class="mt-4 flex items-center justify-between text-sm text-graphite-700">
      <button type="button" class="btn-ghost py-1.5 px-3"
              [disabled]="p.first" (click)="irParaPagina(p.number - 1)">« Anterior</button>
      <span>Página {{ p.number + 1 }} de {{ p.totalPages }}</span>
      <button type="button" class="btn-ghost py-1.5 px-3"
              [disabled]="p.last" (click)="irParaPagina(p.number + 1)">Próximo »</button>
    </div>
  </ng-container>
</section>
```

- [ ] **Step 3: Build admin**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build verde.

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/modules/catalog/pages/produtos-lista
git commit -m "feat(admin): slice2 16-produtos lista page with url-synced filters"
```

---

## Task 18: ProdutoFormPage

**Files:**
- Create: `frontend/admin/src/app/modules/catalog/pages/produto-form/produto-form.page.ts`
- Create: `frontend/admin/src/app/modules/catalog/pages/produto-form/produto-form.page.html`

- [ ] **Step 1: Criar `produto-form.page.ts`**

Caminho: `frontend/admin/src/app/modules/catalog/pages/produto-form/produto-form.page.ts`

```ts
import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { CategoriaResponse, achatarArvore, construirArvore } from '@modules/catalog/models/categoria';
import {
  ORIGEM_LABEL,
  ORIGEM_VALUES,
  Origem,
  ProdutoDetail,
  ProdutoImagem,
  ProdutoRequest,
} from '@modules/catalog/models/produto';
import { CategoriaService } from '@modules/catalog/services/categoria.service';
import { ProdutoService } from '@modules/catalog/services/produto.service';
import { ImageDropZoneComponent } from '@modules/catalog/components/image-drop-zone/image-drop-zone.component';
import { VideoUrlInputComponent } from '@modules/catalog/components/video-url-input/video-url-input.component';
import { ToastService } from '@shared/services/toast.service';

type Modo = 'novo' | 'editar';

@Component({
  selector: 'app-produto-form-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    ImageDropZoneComponent,
    VideoUrlInputComponent,
  ],
  templateUrl: './produto-form.page.html',
})
export class ProdutoFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly produtoService = inject(ProdutoService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly origens = ORIGEM_VALUES;
  readonly origemLabel = ORIGEM_LABEL;

  readonly modo = signal<Modo>('novo');
  readonly carregando = signal(false);
  readonly salvando = signal(false);
  readonly produto = signal<ProdutoDetail | null>(null);
  readonly categoriasArvore = signal<{ label: string; id: number }[]>([]);
  readonly novoPreco = signal<number | null>(null);
  readonly atualizandoPreco = signal(false);

  readonly tituloModo = computed(() => this.modo() === 'novo' ? 'Novo produto' : 'Editar produto');

  readonly formulario = this.fb.nonNullable.group({
    sku: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(50)]),
    nome: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(200)]),
    descricaoCurta: this.fb.control<string | null>(null),
    descricaoCompleta: this.fb.control<string | null>(null),
    marca: this.fb.control<string | null>(null),
    categoriaId: this.fb.control<number | null>(null, Validators.required),
    pesoKg: this.fb.nonNullable.control(1.0, [Validators.required, Validators.min(0.001)]),
    alturaCm: this.fb.control<number | null>(null),
    larguraCm: this.fb.control<number | null>(null),
    profundidadeCm: this.fb.control<number | null>(null),
    ncm: this.fb.nonNullable.control('', [Validators.required, Validators.pattern(/^\d{8}$/)]),
    origem: this.fb.nonNullable.control<Origem>('NACIONAL', Validators.required),
    destacado: this.fb.nonNullable.control(false),
    precoInicial: this.fb.nonNullable.control(0.01, [Validators.required, Validators.min(0.01)]),
    videoUrl: this.fb.control<string | null>(null),
  });

  ngOnInit(): void {
    this.categoriaService.listar().subscribe({
      next: lista => this.popularCategorias(lista),
      error: () => this.toast.error('Falha ao carregar categorias.'),
    });

    const sku = this.route.snapshot.paramMap.get('sku');
    if (sku) {
      this.modo.set('editar');
      this.carregar(sku);
    }
  }

  submeter(): void {
    if (this.formulario.invalid || this.salvando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    const valor = this.formulario.getRawValue();
    const request: ProdutoRequest = {
      sku: valor.sku,
      nome: valor.nome,
      descricaoCurta: valor.descricaoCurta,
      descricaoCompleta: valor.descricaoCompleta,
      marca: valor.marca,
      categoriaId: valor.categoriaId!,
      pesoKg: valor.pesoKg,
      alturaCm: valor.alturaCm,
      larguraCm: valor.larguraCm,
      profundidadeCm: valor.profundidadeCm,
      ncm: valor.ncm,
      origem: valor.origem,
      destacado: valor.destacado,
      precoInicial: valor.precoInicial,
      videoUrl: valor.videoUrl || null,
    };

    if (this.modo() === 'novo') {
      this.produtoService.criar(request).subscribe({
        next: criado => {
          this.salvando.set(false);
          this.toast.success('Produto criado. Adicione imagens e vídeo.');
          this.router.navigate(['/produtos', criado.sku, 'editar']);
        },
        error: (httpError: HttpErrorResponse) => this.tratarErroSalvar(httpError),
      });
    } else {
      const id = this.produto()?.id;
      if (!id) {
        this.salvando.set(false);
        return;
      }
      this.produtoService.atualizar(id, request).subscribe({
        next: atualizado => {
          this.salvando.set(false);
          this.produto.set(atualizado);
          this.toast.success('Produto atualizado.');
        },
        error: (httpError: HttpErrorResponse) => this.tratarErroSalvar(httpError),
      });
    }
  }

  atualizarPreco(): void {
    const id = this.produto()?.id;
    const novoValor = this.novoPreco();
    if (!id || !novoValor || novoValor < 0.01) return;
    this.atualizandoPreco.set(true);
    this.produtoService.atualizarPreco(id, novoValor).subscribe({
      next: atualizado => {
        this.atualizandoPreco.set(false);
        this.produto.set(atualizado);
        this.formulario.patchValue({ precoInicial: atualizado.preco });
        this.novoPreco.set(null);
        this.toast.success('Preço atualizado.');
      },
      error: () => {
        this.atualizandoPreco.set(false);
        this.toast.error('Falha ao atualizar preço.');
      },
    });
  }

  onImagensAtualizadas(novas: ProdutoImagem[]): void {
    const atual = this.produto();
    if (atual) {
      this.produto.set({ ...atual, imagens: novas });
    }
  }

  onVideoUrlChange(valor: string | null): void {
    this.formulario.patchValue({ videoUrl: valor });
  }

  private carregar(sku: string): void {
    this.carregando.set(true);
    this.produtoService.buscarPorSku(sku).subscribe({
      next: detalhe => {
        this.carregando.set(false);
        this.produto.set(detalhe);
        this.formulario.patchValue({
          sku: detalhe.sku,
          nome: detalhe.nome,
          descricaoCurta: detalhe.descricaoCurta,
          descricaoCompleta: detalhe.descricaoCompleta,
          marca: detalhe.marca,
          categoriaId: detalhe.categoria.id,
          pesoKg: detalhe.pesoKg,
          alturaCm: detalhe.alturaCm,
          larguraCm: detalhe.larguraCm,
          profundidadeCm: detalhe.profundidadeCm,
          ncm: detalhe.ncm,
          origem: detalhe.origem,
          destacado: detalhe.destacado,
          precoInicial: detalhe.preco,
          videoUrl: detalhe.videoUrl,
        });
        this.formulario.controls.sku.disable();
        this.formulario.controls.precoInicial.disable();
      },
      error: () => {
        this.carregando.set(false);
        this.toast.error('Produto não encontrado.');
        this.router.navigate(['/produtos']);
      },
    });
  }

  private popularCategorias(lista: CategoriaResponse[]): void {
    const arvore = construirArvore(lista);
    const planas = achatarArvore(arvore);
    this.categoriasArvore.set(
      planas.map(node => ({
        id: node.categoria.id,
        label: '— '.repeat(node.nivel) + node.categoria.nome
          + (node.categoria.ativo ? '' : ' (inativa)'),
      })),
    );
  }

  private tratarErroSalvar(erro: HttpErrorResponse): void {
    this.salvando.set(false);
    if (erro.status === 409) {
      this.toast.error(erro.error?.detail ?? 'SKU já existe.');
    } else if (erro.status === 400 || erro.status === 422) {
      this.toast.error(erro.error?.detail ?? 'Dados inválidos. Verifique o formulário.');
    } else {
      this.toast.error('Falha ao salvar produto.');
    }
  }
}
```

- [ ] **Step 2: Criar `produto-form.page.html`**

Caminho: `frontend/admin/src/app/modules/catalog/pages/produto-form/produto-form.page.html`

```html
<section>
  <nav class="text-sm text-graphite-500 mb-4">
    <a routerLink="/produtos" class="hover:text-graphite-700">← Voltar para produtos</a>
  </nav>

  <header class="mb-6">
    <h1 class="text-2xl font-display font-semibold text-graphite-900">{{ tituloModo() }}</h1>
    <p *ngIf="produto() as p" class="text-sm text-graphite-500 mt-1">
      SKU <span class="font-mono">{{ p.sku }}</span> · criado em {{ p.id }}
    </p>
  </header>

  <div *ngIf="carregando()" class="space-y-3">
    <div class="h-12 w-1/2 rounded bg-graphite-100 animate-pulse"></div>
    <div class="h-12 w-full rounded bg-graphite-100 animate-pulse"></div>
    <div class="h-12 w-3/4 rounded bg-graphite-100 animate-pulse"></div>
  </div>

  <form *ngIf="!carregando()"
        [formGroup]="formulario" (ngSubmit)="submeter()"
        class="grid grid-cols-1 md:grid-cols-2 gap-6">

    <div class="rounded-lg border border-graphite-200 bg-graphite-0 p-5 space-y-4">
      <h2 class="text-sm font-semibold text-graphite-700">Dados básicos</h2>

      <div>
        <label class="label" for="sku">SKU <span class="text-red-600">*</span></label>
        <input id="sku" type="text" formControlName="sku"
               class="input w-full" maxlength="50" />
      </div>
      <div>
        <label class="label" for="nome">Nome <span class="text-red-600">*</span></label>
        <input id="nome" type="text" formControlName="nome"
               class="input w-full" maxlength="200" />
      </div>
      <div>
        <label class="label" for="marca">Marca</label>
        <input id="marca" type="text" formControlName="marca"
               class="input w-full" maxlength="100" />
      </div>
      <div>
        <label class="label" for="categoria">Categoria <span class="text-red-600">*</span></label>
        <select id="categoria" formControlName="categoriaId" class="input w-full">
          <option [ngValue]="null">Selecione…</option>
          <option *ngFor="let c of categoriasArvore()" [ngValue]="c.id">{{ c.label }}</option>
        </select>
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="label" for="ncm">NCM <span class="text-red-600">*</span></label>
          <input id="ncm" type="text" formControlName="ncm"
                 class="input w-full font-mono" maxlength="8" />
        </div>
        <div>
          <label class="label" for="origem">Origem <span class="text-red-600">*</span></label>
          <select id="origem" formControlName="origem" class="input w-full">
            <option *ngFor="let o of origens" [ngValue]="o">{{ origemLabel[o] }}</option>
          </select>
        </div>
      </div>
      <div>
        <label class="label" for="descricaoCurta">Descrição curta</label>
        <textarea id="descricaoCurta" formControlName="descricaoCurta"
                  class="input w-full" rows="2" maxlength="500"></textarea>
      </div>
      <div>
        <label class="label" for="descricaoCompleta">Descrição completa (HTML permitido)</label>
        <textarea id="descricaoCompleta" formControlName="descricaoCompleta"
                  class="input w-full" rows="5"></textarea>
      </div>
      <div class="grid grid-cols-4 gap-3">
        <div>
          <label class="label" for="pesoKg">Peso (kg) <span class="text-red-600">*</span></label>
          <input id="pesoKg" type="number" step="0.001" min="0.001"
                 formControlName="pesoKg" class="input w-full" />
        </div>
        <div>
          <label class="label" for="alturaCm">Altura (cm)</label>
          <input id="alturaCm" type="number" step="0.1" formControlName="alturaCm" class="input w-full" />
        </div>
        <div>
          <label class="label" for="larguraCm">Largura (cm)</label>
          <input id="larguraCm" type="number" step="0.1" formControlName="larguraCm" class="input w-full" />
        </div>
        <div>
          <label class="label" for="profundidadeCm">Profundidade (cm)</label>
          <input id="profundidadeCm" type="number" step="0.1" formControlName="profundidadeCm" class="input w-full" />
        </div>
      </div>
      <label class="flex items-center gap-2 text-sm text-graphite-700">
        <input type="checkbox" formControlName="destacado" />
        Destacar este produto na home
      </label>

      <div *ngIf="modo() === 'novo'">
        <label class="label" for="precoInicial">Preço inicial (R$) <span class="text-red-600">*</span></label>
        <input id="precoInicial" type="number" step="0.01" min="0.01"
               formControlName="precoInicial" class="input w-full" />
      </div>

      <div *ngIf="modo() === 'editar' && produto() as p" class="border-t border-graphite-200 pt-4">
        <label class="label">Preço atual</label>
        <p class="text-2xl font-display text-graphite-900">{{ p.preco | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</p>
        <div class="mt-3 flex items-center gap-2">
          <input type="number" step="0.01" min="0.01" placeholder="Novo preço"
                 [ngModel]="novoPreco()" (ngModelChange)="novoPreco.set($event)"
                 [ngModelOptions]="{ standalone: true }"
                 class="input flex-1" />
          <button type="button" class="btn-ghost py-2 px-3 text-sm"
                  [disabled]="!novoPreco() || atualizandoPreco()"
                  (click)="atualizarPreco()">
            {{ atualizandoPreco() ? 'Atualizando…' : 'Atualizar preço' }}
          </button>
        </div>
      </div>

      <div class="border-t border-graphite-200 pt-4 flex gap-3">
        <button type="submit" class="btn-primary py-2 px-4 text-sm"
                [disabled]="formulario.invalid || salvando()">
          {{ salvando() ? 'Salvando…' : (modo() === 'novo' ? 'Criar produto' : 'Salvar alterações') }}
        </button>
        <a routerLink="/produtos" class="btn-ghost py-2 px-4 text-sm">Cancelar</a>
      </div>
    </div>

    <div class="rounded-lg border border-graphite-200 bg-graphite-0 p-5 space-y-6">
      <h2 class="text-sm font-semibold text-graphite-700">Mídia</h2>

      <div *ngIf="modo() === 'novo'"
           class="rounded-md bg-amber-50 border border-amber-200 p-3 text-sm text-amber-900">
        Salve o produto primeiro para habilitar upload de imagens e preview de vídeo.
      </div>

      <ng-container *ngIf="modo() === 'editar' && produto() as p">
        <div>
          <h3 class="text-xs font-medium text-graphite-600 mb-2">Imagens ({{ p.imagens.length }})</h3>
          <app-image-drop-zone
            [produtoId]="p.id"
            [imagens]="p.imagens"
            (imagensAtualizadas)="onImagensAtualizadas($event)" />
        </div>

        <div class="border-t border-graphite-200 pt-4">
          <app-video-url-input
            [valorInicial]="p.videoUrl"
            (valorChange)="onVideoUrlChange($event)" />
          <p class="mt-2 text-xs text-graphite-500">
            Salve o produto após colar/limpar o link para persistir.
          </p>
        </div>
      </ng-container>
    </div>
  </form>
</section>
```

- [ ] **Step 3: Adicionar classes utilitárias `label` e `input` (se ainda não existirem)**

Verificar em `frontend/admin/src/styles.scss` se já existe `@layer components { .label ... .input ... }`. Se NÃO existir, adicionar no `@layer components` existente:

```scss
  .label { @apply block text-sm font-medium text-graphite-700 mb-1; }
  .input { @apply rounded-md border-graphite-300 text-sm focus:border-coral-500 focus:ring-coral-500; }
```

- [ ] **Step 4: Build admin**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build verde.

- [ ] **Step 5: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/modules/catalog/pages/produto-form \
        frontend/admin/src/styles.scss
git commit -m "feat(admin): slice2 17-produto form page with mode-aware routing"
```

---

## Task 19: CategoriasPage

**Files:**
- Create: `frontend/admin/src/app/modules/catalog/pages/categorias/categorias.page.ts`
- Create: `frontend/admin/src/app/modules/catalog/pages/categorias/categorias.page.html`

- [ ] **Step 1: Criar `categorias.page.ts`**

Caminho: `frontend/admin/src/app/modules/catalog/pages/categorias/categorias.page.ts`

```ts
import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import {
  CategoriaArvoreNode,
  CategoriaRequest,
  CategoriaResponse,
  achatarArvore,
  construirArvore,
  descendentesId,
} from '@modules/catalog/models/categoria';
import { CategoriaService } from '@modules/catalog/services/categoria.service';
import { AdminAuthService } from '@core/services/admin-auth.service';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';
import { ToastService } from '@shared/services/toast.service';

@Component({
  selector: 'app-categorias-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './categorias.page.html',
})
export class CategoriasPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly categoriaService = inject(CategoriaService);
  private readonly auth = inject(AdminAuthService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmDialogService);

  readonly podeMutar = computed(() => this.auth.isAdmin() || this.auth.isGerente());
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly emEdicao = signal<CategoriaResponse | null>(null);
  readonly formAberto = signal(false);

  readonly arvore = computed<CategoriaArvoreNode[]>(() => construirArvore(this.categorias()));
  readonly planasIndentadas = computed(() => achatarArvore(this.arvore()));
  readonly opcoesPai = computed(() => {
    const editando = this.emEdicao();
    const proibidos = editando
      ? descendentesId(this.encontrarNode(editando.id) ?? { categoria: editando, filhas: [], nivel: 0 })
      : new Set<number>();
    return this.planasIndentadas()
      .filter(node => !proibidos.has(node.categoria.id))
      .map(node => ({
        id: node.categoria.id,
        label: '— '.repeat(node.nivel) + node.categoria.nome,
      }));
  });

  readonly formulario = this.fb.nonNullable.group({
    nome: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(100)]),
    slug: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(120),
      Validators.pattern(/^[a-z0-9]+(?:-[a-z0-9]+)*$/),
    ]),
    descricao: this.fb.control<string | null>(null),
    categoriaPaiId: this.fb.control<number | null>(null),
    ordem: this.fb.nonNullable.control(0, [Validators.min(0)]),
  });

  ngOnInit(): void {
    this.recarregar();

    this.formulario.controls.nome.valueChanges.subscribe(novoNome => {
      if (!this.emEdicao()) {
        this.formulario.patchValue(
          { slug: this.gerarSlug(novoNome) },
          { emitEvent: false },
        );
      }
    });
  }

  abrirNovo(): void {
    this.emEdicao.set(null);
    this.formulario.reset({ nome: '', slug: '', descricao: null, categoriaPaiId: null, ordem: 0 });
    this.formAberto.set(true);
  }

  abrirEditar(categoria: CategoriaResponse): void {
    this.emEdicao.set(categoria);
    this.formulario.reset({
      nome: categoria.nome,
      slug: categoria.slug,
      descricao: categoria.descricao,
      categoriaPaiId: categoria.categoriaPaiId,
      ordem: categoria.ordem,
    });
    this.formAberto.set(true);
  }

  fecharForm(): void {
    this.formAberto.set(false);
    this.emEdicao.set(null);
  }

  submeter(): void {
    if (this.formulario.invalid || this.salvando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    const valor = this.formulario.getRawValue();
    const req: CategoriaRequest = {
      nome: valor.nome,
      slug: valor.slug,
      descricao: valor.descricao,
      categoriaPaiId: valor.categoriaPaiId,
      ordem: valor.ordem,
    };

    const editando = this.emEdicao();
    const obs = editando
      ? this.categoriaService.atualizar(editando.id, req)
      : this.categoriaService.criar(req);

    obs.subscribe({
      next: () => {
        this.salvando.set(false);
        this.toast.success(editando ? 'Categoria atualizada.' : 'Categoria criada.');
        this.fecharForm();
        this.recarregar();
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        this.toast.error(erro.error?.detail ?? 'Falha ao salvar categoria.');
      },
    });
  }

  desativar(categoria: CategoriaResponse): void {
    this.confirm.open({
      titulo: 'Desativar categoria?',
      mensagem: `"${categoria.nome}" será desativada (soft delete). Produtos vinculados continuam, mas a categoria some das listagens públicas.`,
      acaoLabel: 'Desativar',
      acaoVariant: 'danger',
    }).then(confirmado => {
      if (!confirmado) return;
      this.categoriaService.desativar(categoria.id).subscribe({
        next: () => {
          this.toast.success('Categoria desativada.');
          this.recarregar();
        },
        error: (erro: HttpErrorResponse) => {
          this.toast.error(erro.error?.detail ?? 'Falha ao desativar.');
        },
      });
    });
  }

  restaurar(categoria: CategoriaResponse): void {
    this.categoriaService.restaurar(categoria.id).subscribe({
      next: () => {
        this.toast.success('Categoria restaurada.');
        this.recarregar();
      },
      error: () => this.toast.error('Falha ao restaurar.'),
    });
  }

  private encontrarNode(id: number): CategoriaArvoreNode | null {
    return this.planasIndentadas().find(node => node.categoria.id === id) ?? null;
  }

  private recarregar(): void {
    this.carregando.set(true);
    this.categoriaService.listar().subscribe({
      next: lista => {
        this.categorias.set(lista);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.toast.error('Falha ao carregar categorias.');
      },
    });
  }

  private gerarSlug(nome: string): string {
    return nome
      .toLowerCase()
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .replace(/[^a-z0-9\s-]/g, '')
      .trim()
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-');
  }
}
```

- [ ] **Step 2: Criar `categorias.page.html`**

Caminho: `frontend/admin/src/app/modules/catalog/pages/categorias/categorias.page.html`

```html
<section>
  <header class="flex items-center justify-between mb-6">
    <div>
      <h1 class="text-2xl font-display font-semibold text-graphite-900">Categorias</h1>
      <p class="text-sm text-graphite-500 mt-1">
        {{ categorias().length }} {{ categorias().length === 1 ? 'categoria' : 'categorias' }} no total
      </p>
    </div>
    <button *ngIf="podeMutar()" type="button" (click)="abrirNovo()"
            class="btn-primary text-sm py-2 px-4">+ Nova categoria</button>
  </header>

  <div *ngIf="formAberto()"
       class="mb-6 rounded-lg border border-coral-200 bg-coral-50 p-5">
    <form [formGroup]="formulario" (ngSubmit)="submeter()" class="space-y-3">
      <h2 class="text-sm font-semibold text-graphite-900">
        {{ emEdicao() ? 'Editar categoria' : 'Nova categoria' }}
      </h2>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
        <div>
          <label class="label" for="cat-nome">Nome <span class="text-red-600">*</span></label>
          <input id="cat-nome" type="text" formControlName="nome"
                 class="input w-full" maxlength="100" />
        </div>
        <div>
          <label class="label" for="cat-slug">Slug <span class="text-red-600">*</span></label>
          <input id="cat-slug" type="text" formControlName="slug"
                 class="input w-full font-mono" maxlength="120" />
        </div>
      </div>
      <div>
        <label class="label" for="cat-desc">Descrição</label>
        <textarea id="cat-desc" formControlName="descricao" rows="2" maxlength="500"
                  class="input w-full"></textarea>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
        <div>
          <label class="label" for="cat-pai">Categoria pai</label>
          <select id="cat-pai" formControlName="categoriaPaiId" class="input w-full">
            <option [ngValue]="null">— Nenhuma (raiz)</option>
            <option *ngFor="let opcao of opcoesPai()" [ngValue]="opcao.id">{{ opcao.label }}</option>
          </select>
        </div>
        <div>
          <label class="label" for="cat-ordem">Ordem</label>
          <input id="cat-ordem" type="number" min="0" formControlName="ordem"
                 class="input w-full" />
        </div>
      </div>
      <div class="flex gap-2 pt-2">
        <button type="submit" class="btn-primary py-2 px-4 text-sm"
                [disabled]="formulario.invalid || salvando()">
          {{ salvando() ? 'Salvando…' : (emEdicao() ? 'Salvar' : 'Criar') }}
        </button>
        <button type="button" class="btn-ghost py-2 px-4 text-sm" (click)="fecharForm()">
          Cancelar
        </button>
      </div>
    </form>
  </div>

  <div *ngIf="carregando()" class="space-y-2">
    <div *ngFor="let _ of [1,2,3,4]" class="h-12 w-full rounded-md bg-graphite-100 animate-pulse"></div>
  </div>

  <div *ngIf="!carregando() && planasIndentadas().length === 0"
       class="rounded-lg border border-dashed border-graphite-300 bg-graphite-0 p-12 text-center">
    <p class="text-graphite-600">Nenhuma categoria cadastrada.</p>
  </div>

  <div *ngIf="!carregando() && planasIndentadas().length > 0"
       class="rounded-lg border border-graphite-200 bg-graphite-0 overflow-hidden">
    <table class="w-full text-sm">
      <thead class="border-b border-graphite-200 bg-graphite-50 text-xs uppercase text-graphite-600">
        <tr>
          <th class="text-left px-4 py-3">Nome</th>
          <th class="text-left px-4 py-3">Slug</th>
          <th class="text-center px-4 py-3">Filhas</th>
          <th class="text-center px-4 py-3">Ordem</th>
          <th class="text-center px-4 py-3">Status</th>
          <th class="text-right px-4 py-3">Ações</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-graphite-100">
        <tr *ngFor="let node of planasIndentadas()"
            [class.opacity-60]="!node.categoria.ativo">
          <td class="px-4 py-3" [style.padding-left.rem]="1 + node.nivel * 1.5">
            <span class="text-graphite-900">{{ node.categoria.nome }}</span>
            <span *ngIf="node.categoria.descricao" class="block text-xs text-graphite-500 mt-0.5">
              {{ node.categoria.descricao }}
            </span>
          </td>
          <td class="px-4 py-3 text-graphite-500 font-mono text-xs">{{ node.categoria.slug }}</td>
          <td class="px-4 py-3 text-center text-graphite-700">{{ node.filhas.length }}</td>
          <td class="px-4 py-3 text-center text-graphite-700">{{ node.categoria.ordem }}</td>
          <td class="px-4 py-3 text-center">
            <span class="inline-block rounded-full px-2 py-0.5 text-xs"
                  [class.bg-emerald-100]="node.categoria.ativo"
                  [class.text-emerald-800]="node.categoria.ativo"
                  [class.bg-graphite-100]="!node.categoria.ativo"
                  [class.text-graphite-700]="!node.categoria.ativo">
              {{ node.categoria.ativo ? 'Ativa' : 'Inativa' }}
            </span>
          </td>
          <td class="px-4 py-3 text-right whitespace-nowrap">
            <button *ngIf="podeMutar()" type="button"
                    class="btn-ghost text-xs py-1 px-2"
                    (click)="abrirEditar(node.categoria)">Editar</button>
            <button *ngIf="podeMutar() && node.categoria.ativo" type="button"
                    class="btn-ghost text-xs py-1 px-2 ml-1"
                    (click)="desativar(node.categoria)">Desativar</button>
            <button *ngIf="podeMutar() && !node.categoria.ativo" type="button"
                    class="btn-ghost text-xs py-1 px-2 ml-1"
                    (click)="restaurar(node.categoria)">Restaurar</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</section>
```

- [ ] **Step 3: Build admin**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build verde.

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/modules/catalog/pages/categorias
git commit -m "feat(admin): slice2 18-categorias page with indented tree and inline form"
```

---

## Task 20: Rotas admin + sidebar update

**Files:**
- Modify: `frontend/admin/src/app/app.routes.ts`
- Modify: `frontend/admin/src/app/core/layout/admin-shell/admin-shell.page.ts`

- [ ] **Step 1: Atualizar `app.routes.ts`**

Substituir todo o conteúdo por:

```ts
import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';
import { roleGuard } from '@core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('@modules/auth/pages/login/login.page').then(m => m.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@core/layout/admin-shell/admin-shell.page').then(m => m.AdminShellPage),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('@modules/dashboard/pages/home/home.page').then(m => m.DashboardHomePage),
      },
      {
        path: 'produtos',
        canActivate: [roleGuard(['ROLE_ADMIN_LOJA', 'ROLE_GERENTE', 'ROLE_OPERADOR'])],
        loadComponent: () =>
          import('@modules/catalog/pages/produtos-lista/produtos-lista.page')
            .then(m => m.ProdutosListaPage),
      },
      {
        path: 'produtos/novo',
        canActivate: [roleGuard(['ROLE_ADMIN_LOJA', 'ROLE_GERENTE'])],
        loadComponent: () =>
          import('@modules/catalog/pages/produto-form/produto-form.page')
            .then(m => m.ProdutoFormPage),
      },
      {
        path: 'produtos/:sku/editar',
        canActivate: [roleGuard(['ROLE_ADMIN_LOJA', 'ROLE_GERENTE', 'ROLE_OPERADOR'])],
        loadComponent: () =>
          import('@modules/catalog/pages/produto-form/produto-form.page')
            .then(m => m.ProdutoFormPage),
      },
      {
        path: 'categorias',
        canActivate: [roleGuard(['ROLE_ADMIN_LOJA', 'ROLE_GERENTE', 'ROLE_OPERADOR'])],
        loadComponent: () =>
          import('@modules/catalog/pages/categorias/categorias.page')
            .then(m => m.CategoriasPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
```

- [ ] **Step 2: Atualizar `itensMenu` em `admin-shell.page.ts`**

Substituir o array `itensMenu` por:

```ts
  readonly itensMenu: ItemMenu[] = [
    { rota: '/dashboard',     rotulo: 'Dashboard',     icone: '📊', habilitada: true, exact: true },
    { rota: '/produtos',      rotulo: 'Catálogo',      icone: '📦', habilitada: true },
    { rota: '/categorias',    rotulo: 'Categorias',    icone: '🗂️', habilitada: true },
    { rota: '/pedidos',       rotulo: 'Pedidos',       icone: '📋', habilitada: false },
    { rota: '/comercial',     rotulo: 'Comercial',     icone: '🏷️', habilitada: false },
    { rota: '/clientes',      rotulo: 'Clientes',      icone: '👥', habilitada: false },
    { rota: '/relatorios',    rotulo: 'Relatórios',    icone: '📈', habilitada: false },
    { rota: '/configuracoes', rotulo: 'Configurações', icone: '⚙️', habilitada: false },
  ];
```

- [ ] **Step 3: Build admin**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build verde.

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/app.routes.ts \
        frontend/admin/src/app/core/layout/admin-shell/admin-shell.page.ts
git commit -m "feat(admin): slice2 19-routes and sidebar entries for catalog"
```

---

## Task 21: Storefront — pipe + helper video-embed

**Files:**
- Create: `frontend/storefront/src/app/shared/pipes/safe-resource-url.pipe.ts`
- Create: `frontend/storefront/src/app/shared/utils/video-embed.ts`

- [ ] **Step 1: Criar pasta utils (se não existir)**

```bash
mkdir -p /home/ali/projects/pet-hub/frontend/storefront/src/app/shared/utils
```

- [ ] **Step 2: Criar `safe-resource-url.pipe.ts`**

Caminho: `frontend/storefront/src/app/shared/pipes/safe-resource-url.pipe.ts`

```ts
import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Pipe({ name: 'safeResourceUrl', standalone: true })
export class SafeResourceUrlPipe implements PipeTransform {
  private readonly sanitizer = inject(DomSanitizer);

  transform(value: string | null | undefined): SafeResourceUrl | null {
    if (!value) return null;
    return this.sanitizer.bypassSecurityTrustResourceUrl(value);
  }
}
```

- [ ] **Step 3: Criar `video-embed.ts`**

Caminho: `frontend/storefront/src/app/shared/utils/video-embed.ts`

```ts
/**
 * Converte URL pública (YouTube watch ou Vimeo) na URL de embed.
 * Retorna null se a URL não casa com nenhum padrão whitelisted.
 */
export function urlEmbed(urlPublica: string | null | undefined): string | null {
  if (!urlPublica) return null;

  const youtubeWatch = urlPublica.match(/youtube\.com\/watch\?v=([\w-]+)/);
  if (youtubeWatch) return `https://www.youtube.com/embed/${youtubeWatch[1]}`;

  const youtubeShort = urlPublica.match(/youtu\.be\/([\w-]+)/);
  if (youtubeShort) return `https://www.youtube.com/embed/${youtubeShort[1]}`;

  const vimeo = urlPublica.match(/vimeo\.com\/(\d+)/);
  if (vimeo) return `https://player.vimeo.com/video/${vimeo[1]}`;

  return null;
}
```

- [ ] **Step 4: Build storefront**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Expected: build verde.

- [ ] **Step 5: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/storefront/src/app/shared/pipes/safe-resource-url.pipe.ts \
        frontend/storefront/src/app/shared/utils/video-embed.ts
git commit -m "feat(storefront): slice2 20-safe-resource-url pipe and video-embed helper"
```

---

## Task 22: Storefront — videoUrl em ProdutoDetail + seção no PDP

**Files:**
- Modify: `frontend/storefront/src/app/modules/catalog/models/catalog.ts`
- Modify: `frontend/storefront/src/app/modules/catalog/pages/product-detail/product-detail.page.ts`
- Modify: `frontend/storefront/src/app/modules/catalog/pages/product-detail/product-detail.page.html`

- [ ] **Step 1: Adicionar `videoUrl` em `ProdutoDetail`**

Em `frontend/storefront/src/app/modules/catalog/models/catalog.ts`, adicionar antes do fechamento da interface `ProdutoDetail`:

```ts
  videoUrl?: string | null;
```

(Ficará após `destacado: boolean;`.)

- [ ] **Step 2: Atualizar `product-detail.page.ts`**

Adicionar imports após os existentes:

```ts
import { SafeResourceUrlPipe } from '@shared/pipes/safe-resource-url.pipe';
import { urlEmbed } from '@shared/utils/video-embed';
import { computed } from '@angular/core';
```

(Atenção: `computed` provavelmente já é importado via `signal`; ajustar para `import { Component, OnInit, computed, inject, signal } from '@angular/core';` se faltar.)

Em `@Component({ imports: [...] })`, adicionar `SafeResourceUrlPipe`:

```ts
  imports: [
    CommonModule, RouterLink, PriceDisplayComponent, ShippingCalculatorComponent,
    SafeHtmlPipe, SafeResourceUrlPipe,
  ],
```

Adicionar após `readonly imagemAtiva = ...`:

```ts
  readonly videoEmbed = computed(() => urlEmbed(this.produto()?.videoUrl ?? null));
```

- [ ] **Step 3: Adicionar seção de vídeo em `product-detail.page.html`**

Localizar a seção `<section *ngIf="listaEspecificacoes(produtoExibido).length" ...>` (linha ~85). Adicionar **antes** dela:

```html
  <section *ngIf="videoEmbed() as embed" class="mt-12 max-w-reading">
    <h2 class="text-2xl text-graphite-900">Vídeo</h2>
    <div class="mt-4 aspect-video rounded-lg overflow-hidden bg-graphite-100">
      <iframe [src]="embed | safeResourceUrl"
              sandbox="allow-scripts allow-same-origin allow-presentation"
              referrerpolicy="strict-origin-when-cross-origin"
              class="w-full h-full" allowfullscreen
              title="Vídeo do produto"></iframe>
    </div>
  </section>
```

- [ ] **Step 4: Build storefront**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Expected: build verde.

- [ ] **Step 5: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/storefront/src/app/modules/catalog/models/catalog.ts \
        frontend/storefront/src/app/modules/catalog/pages/product-detail/product-detail.page.ts \
        frontend/storefront/src/app/modules/catalog/pages/product-detail/product-detail.page.html
git commit -m "feat(storefront): slice2 21-video section in product detail page"
```

---

## Task 23: Smoke E2E completa + docs

**Files:**
- Modify: `ai-memory/roadmap/fase-6-pendencias.md`
- Modify: `ROADMAP.md`

- [ ] **Step 1: Verificar serviços rodando**

```bash
curl -s -o /dev/null -w "backend: %{http_code}\n" http://localhost:8080/actuator/health
docker ps --format '{{.Names}}' | grep -E "pethub-(backend|postgres|redis)"
```

Expected: backend 200, três containers ativos.

- [ ] **Step 2: Subir storefront e admin em background**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm start > /tmp/storefront.log 2>&1 &
cd /home/ali/projects/pet-hub/frontend/admin && npm start > /tmp/admin.log 2>&1 &
sleep 40
curl -s -o /dev/null -w "storefront /: %{http_code}\n" http://127.0.0.1:4242/
curl -s -o /dev/null -w "admin /login: %{http_code}\n" http://127.0.0.1:4244/login
```

Expected: ambos 200.

- [ ] **Step 3: Smoke checklist manual no browser (§8 da spec)**

Login admin em `http://127.0.0.1:4244` com `admin@pethub.com / Admin@123`. Validar:

- [ ] Sidebar mostra **Catálogo** e **Categorias** habilitados.
- [ ] `/admin/produtos` lista carrega com 20 produtos do seed.
- [ ] Filtrar por categoria → URL ganha `?categoriaId=...`, lista recarrega.
- [ ] Filtrar `Inativos` → lista vazia (seed só tem ativos).
- [ ] Buscar `coleira` → resultados filtrados.
- [ ] Ordenar por nome (clicar header) → URL ganha `sort=nome,desc`.
- [ ] Paginação `Próximo »` se houver mais de 20.
- [ ] Clicar `Editar` num produto → carrega form, SKU readonly, preço readonly (mostra valor + input separado).
- [ ] Atualizar preço via input dedicado → toast verde, preço refletido no card.
- [ ] No modo editar, arrastar 1 imagem JPG ≤5MB → upload ok, aparece no grid, marcada principal.
- [ ] Arrastar 1 PDF → toast vermelho "tipo não suportado".
- [ ] Remover imagem com confirm dialog.
- [ ] Colar URL inválida no campo de vídeo (ex: `https://malicious.com/v`) → erro inline, sem preview.
- [ ] Colar URL YouTube válida (`https://www.youtube.com/watch?v=dQw4w9WgXcQ`) → preview iframe abaixo do input.
- [ ] Salvar produto → toast "Produto atualizado".
- [ ] Abrir storefront `http://127.0.0.1:4242/produtos/<SKU>` → seção "Vídeo" renderiza acima de Especificações.
- [ ] Abrir produto SEM `videoUrl` → seção de vídeo não aparece.
- [ ] Clicar `+ Novo produto` → form, SKU editável, preço inicial obrigatório. Submeter dados válidos → redirect pra `/produtos/:sku/editar` com toast verde.
- [ ] `/admin/categorias` lista todas as categorias (incluindo seed) com indentação por nível.
- [ ] Criar categoria filha (selecionar pai no select) → reaparece indentada.
- [ ] Tentar editar categoria e selecionar a si mesma como pai → opção não aparece no select.
- [ ] Desativar categoria → soft delete, fica `opacity-60` + badge "Inativa", botão muda pra `Restaurar`.
- [ ] Restaurar → volta a Ativa.
- [ ] Logout, login com `operador@pethub.com / Operador@123` → vê `/admin/produtos` (read-only). Botão `+ Novo produto` não aparece. Tentar acessar `/admin/produtos/novo` na URL → redirect pra `/dashboard`.

Anotar falhas; corrigir antes de prosseguir.

- [ ] **Step 4: Parar dev servers**

```bash
pkill -f "ng serve.*4242" 2>/dev/null || true
pkill -f "ng serve.*4244" 2>/dev/null || true
```

- [ ] **Step 5: Atualizar `ai-memory/roadmap/fase-6-pendencias.md`**

Substituir todo o arquivo por:

```markdown
# Fase 6 — Pendências e checklist

> **Status:** Slices 6.1 ✅, 6.2 ✅ entregues em 2026-05-17. Slices 6.3-6.6 ⏳ pendentes.
> Testes Karma/Jest/JUnit formais continuam dívida acumulada (mesmo bloqueio das Fases 1-5).

## ✅ Slices entregues

### Slice 6.1 — Foundation (2026-05-17)

14 commits sequenciais (`slice1 01-..` a `slice1 13b-..` + docs).
Detalhes em [`docs/superpowers/specs/2026-05-17-admin-slice1-foundation-design.md`].

### Slice 6.2 — Catálogo (2026-05-17)

~22 commits sequenciais com sufixos `slice2 01-..` a `slice2 21-..` + docs.

**Backend:**
- `refactor(backend) 01`: PhotoStorage movido `customer/` → `common/storage/`. PetService e WebMvcConfig atualizam imports.
- `feat(backend) 02`: Flyway V18 — `produtos.video_url VARCHAR(500)`.
- `feat(backend) 03`: `@ValidVideoEmbedUrl` (whitelist regex YouTube/Vimeo, anti-SSRF; backend nunca faz HTTP request à URL).
- `feat(backend) 04`: Produto.videoUrl + ProdutoRequest.videoUrl + ProdutoDetailResponse.videoUrl.
- `feat(backend) 05`: ProdutoService persiste videoUrl em criar/atualizar.
- `feat(backend) 06`: ProdutoAdminSummaryResponse + ProdutoAdminRepository com filtros (`q`, `categoriaId`, `ativo`).
- `feat(backend) 07`: ProdutoMapper.toAdminSummary + ProdutoService.listarAdmin.
- `feat(backend) 08`: `GET /api/v1/admin/catalog/products` (admin, com filtros + sort).
- `feat(backend) 09`: `POST /api/v1/admin/catalog/products/{id}/images/upload` (multipart, reusa PhotoStorage).
- `feat(backend) 10`: CategoriaService.restaurar + `POST /api/v1/admin/catalog/categories/{id}/restaurar`.

**Frontend admin:**
- `feat(admin) 11`: models produto.ts + categoria.ts (com construirArvore/achatarArvore/descendentesId).
- `feat(admin) 12`: services produto + categoria.
- `feat(admin) 13`: shared utils/video-embed.ts + pipes/safe-resource-url.pipe.ts.
- `feat(admin) 14`: VideoUrlInputComponent (validação inline + preview iframe sandboxed).
- `feat(admin) 15`: ImageDropZoneComponent (DnD nativo + multipart sequencial via concatMap).
- `feat(admin) 16`: ProdutosListaPage (filtros URL-sync, ordenação, toggle ativo inline).
- `feat(admin) 17`: ProdutoFormPage (modo via rota, criar redireciona pra editar).
- `feat(admin) 18`: CategoriasPage (lista indentada + form inline + desativar/restaurar).
- `feat(admin) 19`: rotas + roleGuard + sidebar update.

**Frontend storefront:**
- `feat(storefront) 20`: SafeResourceUrlPipe + helper urlEmbed.
- `feat(storefront) 21`: ProdutoDetail.videoUrl + seção condicional de vídeo no PDP.

**Decisões congeladas no Slice 6.2:**
- PhotoStorage agora vive em `common/` — reusado por customer (pets) e catalog (produtos).
- Upload multipart real: `POST /admin/catalog/products/{id}/images/upload` campo `file`; aceita JPG/PNG/WEBP/GIF até 5 MB; storage local em `var/uploads/products/`. URL legacy via `AdicionarImagemRequest` continua disponível mas não é usada pelo admin.
- Vídeo de produto: URL externa apenas (YouTube/Vimeo). Backend nunca faz request à URL (mitigação SSRF). Storefront renderiza via `<iframe sandbox>` com `bypassSecurityTrustResourceUrl` em URL já validada por regex.
- Lista admin tem endpoint dedicado `GET /admin/catalog/products` com filtros (`q`, `categoriaId`, `ativo`, page, sort). Public list e search continuam intactos.
- Categoria: desativar = soft delete (já existia); restaurar = novo endpoint POST `/{id}/restaurar`.
- Origem real do enum (`NACIONAL | IMPORTADO_DIRETO | IMPORTADO_INDIRETO`) usada no admin — spec inicial assumia outro conjunto.
- ProdutoRequest permanece DTO único pra criar/atualizar (não dois DTOs).
- Storage local não escala em K8s — revisita em Fase 11 com S3/MinIO/GCS.

## ⏳ Slices 6.3-6.6 pendentes

Plano em [`ai-memory/roadmap/fase-6-decomposicao.md`](./fase-6-decomposicao.md).

- **6.3 Operações** — Estoque admin, Pedidos admin (transições + etiqueta PDF), cancelamento pelo cliente (fecha dívida da Fase 5), audit log. ~7-9h.
- **6.4 Comercial** — Cupons, Promoções, Regras de Imposto (3 entidades novas no `pricing` + CRUDs admin). ~5-6h.
- **6.5 Insights** — Dashboard ApexCharts real, clientes admin, audit log UI, SSE de novos pedidos. ~5-6h.
- **6.6 Reviews** — Sistema de avaliações (backend + storefront + admin moderação). ~7-9h.

## ⚠️ Dívida técnica

### Testes formais
Mesmo bloqueio das fases 1-6.1 — sem CI rodando ainda.

### AppSec do Slice 6.2
- LOG-2 (mascaramento de PII em logs do frontend) — endereçar na transição pra staging.
- SSRF mitigado por design (backend nunca consome `videoUrl`). Validador whitelist + iframe sandbox = defesa em profundidade.
- Storage local de imagens fica como dívida até Fase 11 (K8s requer storage externo).

### Funcional
- Sem reordenar imagens (DnD reorder) — backlog (depende de endpoint backend novo).
- Sem bulk actions na lista de produtos — backlog.
- Sem upload de vídeo interno (arquivo) — sempre URL externa.
- Sem audit log das ações de catálogo — entra no Slice 6.3.

## 🚀 Comando rápido para retomar

```bash
cd /home/ali/projects/pet-hub
git log --oneline | head -25

# Infra
docker compose --env-file .env.local -f infrastructure/docker/docker-compose.dev.yml up -d

# Backend
docker run --rm -d --name pethub-backend --network host --env-file .env.local \
  -v "$PWD/backend/application/target/pet-hub-backend.jar:/app/app.jar:ro" \
  -v "$PWD/backend/application/var:/app/var" \
  -w /app eclipse-temurin:21-jre java -jar /app/app.jar

# Frontends
cd frontend/storefront && npm start &   # :4242
cd frontend/admin && npm start &        # :4244
```

**Próximo passo: Slice 6.3 (Operações + cancelamento pelo cliente).**
```

- [ ] **Step 6: Atualizar `ROADMAP.md`**

Localizar a linha:

```
## Fase 6 — Frontend Admin (Back-Office) · 🚧 (slice 6.1 ✅, slices 6.2-6.5 pendentes)
```

Substituir por:

```
## Fase 6 — Frontend Admin (Back-Office) · 🚧 (slices 6.1 ✅ e 6.2 ✅, slices 6.3-6.6 pendentes)
```

E logo abaixo, substituir o blockquote da Fase 6 por:

```
> **Slices 6.1 e 6.2 entregues em 2026-05-17.** 6.1: scaffold admin + auth com validação de role + shell + login + dashboard placeholder. 6.2: CRUDs admin de Produtos (lista filtrada + form modo via rota + upload multipart de imagens + vídeo URL externa) e Categorias (lista plana indentada + form inline + desativar/restaurar); PDP do storefront renderiza vídeo em iframe sandboxed. Backend: PhotoStorage movido para `common/`, Flyway V18, `@ValidVideoEmbedUrl`, endpoint admin de listagem com filtros, upload multipart. Plano em [`ai-memory/roadmap/fase-6-decomposicao.md`](./ai-memory/roadmap/fase-6-decomposicao.md); histórico em [`ai-memory/roadmap/fase-6-pendencias.md`](./ai-memory/roadmap/fase-6-pendencias.md).
```

- [ ] **Step 7: Commit final**

```bash
cd /home/ali/projects/pet-hub
git add ai-memory/roadmap/fase-6-pendencias.md ROADMAP.md
git commit -m "$(cat <<'EOF'
docs(roadmap): slice 6.2 entregue — catálogo admin

CRUDs admin de produtos e categorias, upload multipart real
(PhotoStorage reusado de customer→common), vídeo de produto via URL
externa com validador anti-SSRF, iframe sandboxed no PDP do storefront.
~22 commits sequenciais. Build verde nos 3 módulos; smoke E2E manual ok.

Próximo slice: 6.3 (Operações + cancelamento pelo cliente).
EOF
)"
```

- [ ] **Step 8: Verificar histórico dos commits do slice**

```bash
git log --oneline | grep "slice2" | head -25
```

Expected: ~22 commits sequenciais com prefixo `slice2` + 1 commit final `docs(roadmap)`.

---

## Verificação final

**Critérios de "pronto" (spec §10):**

- [ ] Flyway V18 aplicada.
- [ ] Backend build verde no full reactor.
- [ ] `frontend/admin/` build verde.
- [ ] `frontend/storefront/` build verde.
- [ ] Smoke checklist §8 da spec (e Task 23 Step 3) verde manual.
- [ ] Commits Conventional Commits com escopo `feat(admin):`, `feat(backend):`, `feat(storefront):`, `refactor(backend):`, `chore(*)` (~22 commits + docs).
- [ ] `ai-memory/roadmap/fase-6-pendencias.md` atualizado.
- [ ] `ROADMAP.md`: Fase 6 ganha nota de 6.2 entregue.

**Dívida técnica registrada:**

- Testes formais Karma/Jest/JUnit — mesmo bloqueio das fases 1-6.1.
- Endpoint backend de reordenar imagens — backlog.
- Bulk actions na lista de produtos — backlog.
- Upload de vídeo interno — backlog (sempre URL externa).
- Audit log de ações de catálogo — Slice 6.3.
- Storage local de imagens não escala em K8s — revisita em Fase 11.

**Próximo passo:** Slice 6.3 (Operações + cancelamento pelo cliente).
