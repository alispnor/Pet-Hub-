package com.alispnor.pethub.inventory.infrastructure.scheduler;

import com.alispnor.pethub.inventory.application.usecase.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Roda a cada minuto, libera reservas com {@code expira_em < agora} e ainda
 * {@code status = ATIVA}. O cancelamento dos pedidos correspondentes (status
 * {@code PENDENTE_PAGAMENTO} expirado) é responsabilidade do order-service —
 * separamos para manter um job por bounded context.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservaExpiracaoJob {

    private final InventoryService inventoryService;

    @Scheduled(fixedDelayString = "${inventory.reserva.expiracao.fixed-delay-ms:60000}",
               initialDelayString = "${inventory.reserva.expiracao.initial-delay-ms:30000}")
    public void run() {
        log.debug("ReservaExpiracaoJob tick");
        try {
            var afetados = inventoryService.expirarReservasVencidas();
            if (!afetados.isEmpty()) {
                log.info("ReservaExpiracaoJob — pedidos com reservas expiradas: {}", afetados.keySet());
            }
        } catch (RuntimeException e) {
            log.error("ReservaExpiracaoJob falhou: {}", e.getMessage(), e);
        }
    }
}
