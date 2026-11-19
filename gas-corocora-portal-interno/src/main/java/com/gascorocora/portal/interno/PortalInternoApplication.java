package com.gascorocora.portal.interno;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Portal web de los funcionarios de GAS COROCORA. Cada usuario ve y atiende solo las tareas de su rol
 * (grupos de Camunda), con la informacion del caso y los mismos Camunda Forms del modelo BPMN.
 * Se integra con la API REST de Camunda (/engine-rest) y con la API de negocio del motor (/api).
 */
@SpringBootApplication
public class PortalInternoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortalInternoApplication.class, args);
    }
}
