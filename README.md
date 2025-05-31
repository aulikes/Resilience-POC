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

## 🧠 Comportamiento detallado del flujo de Circuit Breaker

1. Cuando `contador % modular ∈ (1,2,3,4,5)`, el circuito está **CLOSED**.
2. Cuando `contador % modular ∈ (6,7,8)`, el circuito sigue **CLOSED**, aunque estas peticiones **fallan**.
3. Al superar el porcentaje establecido en `failureRateThreshold`, el circuito pasa a **OPEN**.
4. Cuando el circuito está en **OPEN**, no se llama a `processPayment`, sino que se ejecuta directamente `fallbackPayment`.
5. En la petición número **9**, el circuito ya está **OPEN**.
6. El circuito pasa de **OPEN** a **HALF-OPEN** después de esperar el tiempo definido en `waitDurationInOpenState`.
7. Ese tiempo empieza a contarse desde el paso 3 (después de la llamada 8).
8. Como el `waitDurationInOpenState` es de **35 segundos**, las peticiones **9, 10 y 11** pasan directo a `fallbackPayment`.
9. La petición **12** se ejecuta con el circuito en **HALF-OPEN** y entra a `processPayment` con éxito.
10. La petición **13** también entra en estado **HALF-OPEN**, pero falla.
11. La petición **14** entra en estado **HALF-OPEN** y se ejecuta bien.
12. Como **no se completaron con éxito** todas las llamadas permitidas en `permittedNumberOfCallsInHalfOpenState`, el circuito vuelve a **OPEN**.
13. El circuito pasará nuevamente a **HALF-OPEN** tras esperar `waitDurationInOpenState`.
14. Por eso, las peticiones **15, 16 y 17** siguen yendo directo a `fallbackPayment`.
15. Las peticiones **18, 19 y 20** se ejecutan en estado **HALF-OPEN** y todas son exitosas.
16. Se cumple el mínimo de llamadas exitosas requerido.
17. El circuito vuelve finalmente a estado **CLOSED**.