package com.gascorocora.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea (si no existen) los grupos que usan los candidateGroups de los BPMN y un usuario por rol.
 * Asi el portal interno y Tasklist muestran a cada usuario solo las tareas de su rol.
 * El usuario administrador "demo" pertenece a todos los grupos.
 */
@Component
public class IdentidadInicializador implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(IdentidadInicializador.class);

    /** id del grupo -> nombre visible */
    static final Map<String, String> GRUPOS = new LinkedHashMap<>();

    /** usuario -> {nombre, apellido, grupo} */
    static final Map<String, String[]> USUARIOS = new LinkedHashMap<>();

    static {
        GRUPOS.put("asesoresComerciales", "Asesores comerciales (P3 Conexion)");
        GRUPOS.put("ingenierosRedes", "Ingenieria de redes (P3 Conexion)");
        GRUPOS.put("contratistas", "Firma contratista (P3 Construccion)");
        GRUPOS.put("inspectoresCalidad", "Inspectores de calidad (P3 Construccion)");
        GRUPOS.put("lectoresCampo", "Lectores de campo (P1 Facturacion)");
        GRUPOS.put("analistasFacturacion", "Analistas de facturacion (P1 Facturacion)");
        GRUPOS.put("tecnicosSuspension", "Tecnicos de suspension (P1 Suspension)");
        GRUPOS.put("agentesServicio", "Agentes de servicio al cliente (P2 PQRS)");
        GRUPOS.put("analistasPqrs", "Analistas de PQRS (P2 PQRS)");
        GRUPOS.put("jefesServicioCliente", "Jefatura de servicio al cliente (P2 Ajuste)");

        USUARIOS.put("asesor", new String[] {"Laura", "Asesora Comercial", "asesoresComerciales"});
        USUARIOS.put("ingeniero", new String[] {"Mateo", "Ingeniero de Redes", "ingenierosRedes"});
        USUARIOS.put("contratista", new String[] {"Redes del Llano", "Contratista", "contratistas"});
        USUARIOS.put("inspector", new String[] {"Sofia", "Inspectora de Calidad", "inspectoresCalidad"});
        USUARIOS.put("lector", new String[] {"Julian", "Lector de Campo", "lectoresCampo"});
        USUARIOS.put("facturador", new String[] {"Camila", "Analista de Facturacion", "analistasFacturacion"});
        USUARIOS.put("tecnico", new String[] {"Andres", "Tecnico de Suspension", "tecnicosSuspension"});
        USUARIOS.put("agente", new String[] {"Paula", "Agente de Servicio", "agentesServicio"});
        USUARIOS.put("analista", new String[] {"Diego", "Analista de PQRS", "analistasPqrs"});
        USUARIOS.put("jefe", new String[] {"Marcela", "Jefe de Servicio al Cliente", "jefesServicioCliente"});
    }

    private final IdentityService identityService;
    private final String claveUsuarios;
    private final String administrador;

    public IdentidadInicializador(IdentityService identityService,
                                  @Value("${gascorocora.usuarios.clave-por-defecto:demo}") String claveUsuarios,
                                  @Value("${camunda.bpm.admin-user.id:demo}") String administrador) {
        this.identityService = identityService;
        this.claveUsuarios = claveUsuarios;
        this.administrador = administrador;
    }

    @Override
    public void run(ApplicationArguments args) {
        GRUPOS.forEach(this::crearGrupo);
        USUARIOS.forEach((id, datos) -> {
            crearUsuario(id, datos[0], datos[1]);
            crearMembresia(id, datos[2]);
        });
        for (String grupo : List.copyOf(GRUPOS.keySet())) {
            crearMembresia(administrador, grupo);
        }
        LOG.info("[Identidad] {} grupos y {} usuarios por rol listos (clave: '{}'). '{}' pertenece a todos los grupos.",
                GRUPOS.size(), USUARIOS.size(), claveUsuarios, administrador);
    }

    private void crearGrupo(String id, String nombre) {
        if (identityService.createGroupQuery().groupId(id).singleResult() == null) {
            Group grupo = identityService.newGroup(id);
            grupo.setName(nombre);
            grupo.setType("WORKFLOW");
            identityService.saveGroup(grupo);
        }
    }

    private void crearUsuario(String id, String nombre, String apellido) {
        if (identityService.createUserQuery().userId(id).singleResult() == null) {
            User usuario = identityService.newUser(id);
            usuario.setFirstName(nombre);
            usuario.setLastName(apellido);
            usuario.setEmail(id + "@gascorocora.example");
            usuario.setPassword(claveUsuarios);
            identityService.saveUser(usuario);
        }
    }

    private void crearMembresia(String usuario, String grupo) {
        if (identityService.createUserQuery().userId(usuario).singleResult() == null) {
            return;
        }
        if (identityService.createUserQuery().userId(usuario).memberOfGroup(grupo).singleResult() == null) {
            identityService.createMembership(usuario, grupo);
        }
    }
}
