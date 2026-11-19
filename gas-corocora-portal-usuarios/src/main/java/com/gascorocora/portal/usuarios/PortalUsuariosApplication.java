package com.gascorocora.portal.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Portal web de los usuarios externos de GAS COROCORA (suscriptores y solicitantes).
 * No tiene base de datos propia: todo lo consulta y registra a traves de la API del motor
 * (gas-corocora-motor) y del servicio de Recaudo (gas-corocora-recaudo).
 */
@SpringBootApplication
public class PortalUsuariosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortalUsuariosApplication.class, args);
    }
}
