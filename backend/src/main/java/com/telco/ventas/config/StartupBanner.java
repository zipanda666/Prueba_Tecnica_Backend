package com.telco.ventas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Imprime las URLs principales apenas la app termina de arrancar, bien visibles en la
 * terminal (la mayoria de terminales modernas - Windows Terminal, iTerm, VS Code - detectan
 * texto http://... automaticamente y lo vuelven clickeable con Ctrl+Click, sin configuracion
 * adicional). Util sobre todo corriendo con `docker compose up` sin -d, donde los logs
 * quedan pegados en la terminal en vivo.
 */
@Component
public class StartupBanner {

    private static final Logger log = LoggerFactory.getLogger(StartupBanner.class);

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("");
        log.info("========================================================");
        log.info("  Telco Fija Hogar - Backend listo");
        log.info("  API:         http://localhost:8080/api/v1");
        log.info("  Swagger UI:  http://localhost:8080/swagger-ui.html");
        log.info("  Frontend:    http://localhost:5173  (si esta corriendo)");
        log.info("========================================================");
        log.info("");
    }
}