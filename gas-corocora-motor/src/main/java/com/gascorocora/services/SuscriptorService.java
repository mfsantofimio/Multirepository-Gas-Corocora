package com.gascorocora.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gascorocora.dto.RegistroSuscriptorRequest;
import com.gascorocora.persistence.NegocioRepository;

/** Registro, autenticacion y consulta de tramites de los usuarios externos. */
@Service
public class SuscriptorService {

    private final NegocioRepository repositorio;
    private final CasosService casosService;

    public SuscriptorService(NegocioRepository repositorio, CasosService casosService) {
        this.repositorio = repositorio;
        this.casosService = casosService;
    }

    public Map<String, Object> registrar(RegistroSuscriptorRequest r) {
        if (repositorio.existeSuscriptor(r.documento())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario registrado con el documento " + r.documento());
        }
        repositorio.insertarSuscriptor(r.documento(), r.nombre().trim(), r.email().trim(), r.telefono(),
                r.direccion().trim(), r.municipio(), hash(r.documento(), r.clave()), "PORTAL");
        return consultar(r.documento());
    }

    public Map<String, Object> autenticar(String documento, String clave) {
        if (!repositorio.claveValida(documento, hash(documento, clave))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Documento o clave incorrectos");
        }
        return consultar(documento);
    }

    public Map<String, Object> consultar(String documento) {
        Map<String, Object> suscriptor = repositorio.buscarSuscriptor(documento);
        if (suscriptor == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario " + documento);
        }
        suscriptor.put("cuentas", repositorio.cuentasDe(documento));
        return suscriptor;
    }

    /** Todo lo que el usuario tiene en tramite, con el estado del proceso y las acciones que puede hacer. */
    public Map<String, Object> tramites(String documento) {
        Map<String, Object> suscriptor = consultar(documento);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cuentas = (List<Map<String, Object>>) suscriptor.get("cuentas");
        List<String> numeros = cuentas.stream().map(c -> String.valueOf(c.get("numeroCuenta"))).toList();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("suscriptor", suscriptor);
        resultado.put("solicitudes", casosService.enriquecer(repositorio.solicitudesDe(documento)));
        resultado.put("facturas", casosService.enriquecer(repositorio.facturasDe(numeros)));
        resultado.put("pqrs", casosService.enriquecer(repositorio.pqrsDe(numeros)));
        return resultado;
    }

    /** SHA-256(documento:clave) en hexadecimal. Suficiente para el alcance academico del proyecto. */
    static String hash(String documento, String clave) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((documento + ":" + clave).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
