package com.gascorocora;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicacion. Levanta el motor Camunda 7 embebido en Spring Boot
 * y despliega automaticamente los BPMN, DMN y Camunda Forms de src/main/resources
 * (ver META-INF/processes.xml).
 */
@SpringBootApplication
@EnableProcessApplication
public class Application {

  public static void main(String... args) {
    SpringApplication.run(Application.class, args);
  }

}
