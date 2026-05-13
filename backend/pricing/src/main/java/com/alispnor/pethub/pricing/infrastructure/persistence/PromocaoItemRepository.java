package com.alispnor.pethub.pricing.infrastructure.persistence;

import com.alispnor.pethub.pricing.domain.entity.PromocaoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromocaoItemRepository extends JpaRepository<PromocaoItem, Long> {

    List<PromocaoItem> findByPromocaoId(Long promocaoId);
}
