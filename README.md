# 💥 Circuit Breaker Demo con Spring Boot y Resilience4j

Este proyecto es una demostración práctica del patrón **Circuit Breaker** implementado con [Resilience4j](https://resilience4j.readme.io/) en una aplicación Spring Boot. Simula errores de manera controlada y muestra cómo el Circuit Breaker transita por los estados `CLOSED`, `OPEN` y `HALF_OPEN`.

---

## 🧩 Tecnologías utilizadas

- Java 21
- Spring Boot 3.x
- Resilience4j
- Spring Web (RestTemplate)
- Spring Actuator
- Log4j2

---

## 🎯 Objetivo

El propósito es ejecutar periódicamente un servicio (`processPayment`) que simula pagos:
- Algunas llamadas fallan de manera forzada.
- Se observa cómo el Circuit Breaker entra en estado `OPEN` cuando el umbral de fallos es superado.
- Luego pasa a `HALF_OPEN` y finalmente a `CLOSED` si se recupera.

---

## ⚙️ Configuración destacada (`application.properties`)

```properties
# Puerto y contexto
server.port=8080
server.servlet.context-path=/circuit-breaker-demo

# Actuator
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
management.health.circuitbreakers.enabled=true

# Resilience4j configuración global
resilience4j.circuitbreaker.configs.default.slidingWindowSize=10
resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=5
resilience4j.circuitbreaker.configs.default.failureRateThreshold=50
resilience4j.circuitbreaker.configs.default.waitDurationInOpenState=5s
resilience4j.circuitbreaker.configs.default.permittedNumberOfCallsInHalfOpenState=3
resilience4j.circuitbreaker.configs.default.automaticTransitionFromOpenToHalfOpenEnabled=true
resilience4j.circuitbreaker.configs.default.registerHealthIndicator=true
```

---

### ✅ Observabilidad vía Spring Actuator

| Endpoint                                                                 | Descripción                                         |
|--------------------------------------------------------------------------|-----------------------------------------------------|
| `http://localhost:8080/circuit-breaker-demo/actuator/health`            | Estado general del sistema                          |
| `http://localhost:8080/circuit-breaker-demo/actuator/health/circuitBreakers` | Estado específico de todos los Circuit Breakers     |
| `http://localhost:8080/circuit-breaker-demo/actuator/circuitbreakerevents`  | Lista de eventos generados (transiciones, fallos…)  |

---

## 🧪 Simulación de comportamiento

El componente `PaymentManager()` ejecuta una lógica controlada que:
- Procesa exitosamente las primeras llamadas.
- Fuerza errores del intento 5 al 8 y en el 10.
- Reinicia el contador después del intento 10.
- Los logs muestran claramente cómo y cuándo el circuito se abre, se mantiene abierto, permite intentos (`HALF_OPEN`), y se cierra si las respuestas son exitosas.

---

## 📂 Estructura relevante del proyecto

```
├── manager/
│   ├── PaymentManager.java      # Lógica del Circuit Breaker + simulación
├── scheduler/
│   ├── ScheduledCaller.java     # Ejecuta PaymentManager() cada Xs
├── config/
│   ├── AppConfig.java           # RestTemplate bean
├── resources/
│   ├── application.properties   # Configuración Resilience4j + Actuator
```

---

## 🧠 ¿Qué puedes observar?

- Cómo se comporta un Circuit Breaker real frente a fallos.
- La diferencia entre los estados `CLOSED`, `OPEN`, `HALF_OPEN`.
- El uso de `fallbackMethod` para garantizar resiliencia.
- El ciclo completo sin que la aplicación se caiga.

---

## 🛡️ Buenas prácticas aplicadas

- Separación de responsabilidades por paquetes (`manager`, `scheduler`, `config`)
- Uso de `@PostConstruct` para registrar eventos del Circuit Breaker
- Logs enriquecidos y estructurados para facilitar el seguimiento
- Configuración vía `application.properties` para fácil ajuste

---

## 🧩 Extensiones posibles

- Monitoreo con Micrometer + Prometheus + Grafana
- Alerta en fallback (Kafka, RabbitMQ, correo)
- Panel web de estado en tiempo real

---