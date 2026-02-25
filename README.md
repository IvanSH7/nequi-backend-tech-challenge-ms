# Nequi Tech Challenge - Ticketing System API

Microservicio backend reactivo diseñado para gestionar el ciclo de vida de tickets para eventos
(creación, reserva, compra y liberación). Planteando solución al desafío técnico.

## 🛠️ Stack Tecnológico
* **Lenguaje:** Java 25
* **Framework:** Spring Boot 4.x (WebFlux / Project Reactor)
* **Cloud & Infraestructura:** AWS SDK v2 (Async), DynamoDB, SQS
* **Resiliencia:** Resilience4j (Circuit Breaker)
* **Testing:** Junit, Mockito, reactor-test
* **Herramientas:** Gradle, MapStruct, Lombok, LocalStack, Docker Compose

## Arquitectura de Solución AWS

<img src="./Architecture.jpeg" width="1000" alt="Architecture"/>

## 🏗️ Decisiones de Diseño

### 1. Arquitectura Hexagonal y Domain-Driven Design (DDD)
Se utilizó el estándar de *[Scaffold de Bancolombia](https://bancolombia.github.io/scaffold-clean-architecture/docs/intro)*
con la finalidad de mantener las siguientes consideraciónes:
* **Aislamiento del Dominio:** Garantizar que las reglas de negocio (entidades y casos de uso) no tengan dependencias
con tecnologías externas.
* **Inversión de Dependencias (DIP):** La interacción de la lógica de negocio (capa de dominio) con la base de datos y
la mensajería se efectúa por medio de interfaces. La implementación real (DynamoDB, SQS) ocurre en los `Driven Adapters`,
facilitando el testing aislado mediante Mocks y permitiendo el desligamiento tecnológico sin afectar el core de negocio.

[Clean Architecture — Aislando los detalles](https://medium.com/bancolombia-tech/clean-architecture-aislando-los-detalles-4f9530f35d7a)

![Clean Architecture](https://miro.medium.com/max/1400/1*ZdlHz8B0-qu9Y-QO3AXR_w.png)

### 2. Base de Datos: Single-Table Design y Control de Concurrencia (DynamoDB)
* **Modelado NoSQL:** Se implementó el patrón **Single-Table Design** de DynamoDB con el fin de optimizar rendimiento.
Entidades dispares como `Event`, `Order` y `Ticket` conviven en una unica tabla `ticketing-table-dev` diferenciadas por
llaves compuestas (`pk`, `sk`) y un Global Secondary Index (`EntityTypeIndex`) para satisfacer todos los patrones de
acceso en consultas, reduciendo complejidad y saltos de red.
* **Optimistic Locking y Transacciones ACID:** Con la finalidad de evitar la sobreventa en entornos altamente
transaccionales, se implementaron transacciones atómicas [(`TransactWriteItems`)](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/transaction-apis.html#transaction-apis-txwriteitems) respaldadas por expresiones de
condición (`ConditionExpression`). Al actualizar una orden o ticket, DynamoDB válida el estado previo a nivel de motor
de base de datos, rechazando peticiones concurrentes que intenten modificar el mismo ticket.

### 3. Procesamiento de Nuevas Órdenes (SQS FiFo Queue):
* **FiFo Queue:** Desacopla la validación compleja de disponibilidad de tickets de la generación de nuevas órdenes,
reserva los tickets requeridos y actualiza el estado de la orden.
* **Dead Letter Queue (DLQ):** La cola FIFO principal cuenta con un *Redrive Policy*. Si el procesamiento de una orden
de pago falla repetidamente por un error transitorio de infraestructura, el mensaje es degradado a la DLQ para 
intervención manual o reprocesamiento, asegurando cero pérdida de datos.


### 4. Event-Driven Expiration (SQS Standard Queue)
* **Gestión de TTL sin Cron Jobs:** Con el objetivo de evitar procesos *batch* consultando órdenes expiradas cada
minuto, se implementó un flujo orientado a eventos para gestionar la expiración de órdenes y libración de tickets.
* **Release Delay Queue:** Una vez efectuada la reserva de tickets, se publica un mensaje en una cola estándar SQS
con un *Delay* equivalente al tiempo de gracia para el pago (ej. 10 minutos). Cuando el mensaje se vuelve visible
y es consumido por el listener de la aplicación, iniciando un proceso de validación de estado de la orden;
si no está pagada o en estado confirmada (`CONFIRMED`), ejecuta una transacción ACID compensatoria para liberar
los tickets transitándolos del estado (`RESERVED`) a (`AVAILABLE`) y expirar la orden correspondiente (`EXPIRED`).

### 5. Tolerancia a Fallos
* **Circuit Breaker Inteligente:** Implementado con *Resilience4j*, con la finalidad de proteger el servicio ante fallos
reiterativos por indisponibilidad de ambiente, permitiendo una estabilización del mismo. Se ignoran excepciones
de negocio mediante la propiedad (`ignoreExceptions`) para que no sumen a la tasa de fallos, evitando la apertura
accidental del circuito ante errores del cliente.


---

## ⚖️ Trade-Offs (Compromisos Arquitectónicos)

### 1. Latencia vs. Consistencia Fuerte en la Creación de Eventos:
* **Decisión:** Al crear un evento, la generación masiva de tickets (ej. 10,000 tickets) se ejecuta en un flujo
reactivo asíncrono para no bloquear la respuesta HTTP al cliente.
* **Trade-off:** Si se presenta un fallo en la generación de tickets después de responder `201 Created`, la creación
el evento no se encontrara publicado (`PUBLISHED`) para la compra de Tickets, y se actualizara el estado
del mismo a fallido (`FAILED`)
* **Evolución futura:** Encolar un comando (`CreateTicketsCommand`) en SQS para garantizar la consistencia eventual 
y la resiliencia ante caídas del pod.

### 2. Manejo de Errores con DLQ (Dead Letter Queues):
* **Decisión:** Integración de una política de reintentos (*Redrive Policy*) en SQS.
* **Trade-off:** Añade complejidad operativa. Si un mensaje falla 3 veces por errores transitorios, se aísla en la DLQ,
requiriendo mecanismos de reprocesamiento manual o alertas operativas.

## ⚙️ Requisitos Previos

Para ejecutar este proyecto de manera local, es necesario contar con:
* **Java 25** (JDK)
* **Docker y Docker Compose**


## 🚀 Ejecución del Proyecto

El proyecto está diseñado para levantar todo el ecosistema (Aplicación, Base de Datos, Colas y UI de monitoreo BD) 
con los siguientes pasos:

### 1. Clonar repositorio:
```bash
git clone https://github.com/IvanSH7/nequi-backend-tech-challenge-ms.git
cd nequi-backend-tech-challenge-ms
```

### 2. Compilado de la aplicación:
En la raíz del proyecto, se debe ejecutar el wrapper de Gradle para construir el `.jar`:

```bash
./gradlew clean build 
```

### 3. Despliegue infraestructura:
En la raíz del proyecto se levantará él `docker-compose.yml` el cual se encargara de construir la imagen a partir
del `.jar`, empaquetando la app, un health check configurado en el compose asegurará que LocalStack haya creado la
tabla DynamoDB y colas SQS antes de iniciar la applicación de Spring Boot.

```bash
docker-compose up --build -d
```
Una vez completado el proceso de despliegue el API estará aceptando peticiones en http://localhost:9090

## 📊 Monitoreo y Observabilidad Local
Para auditar el estado del sistema, el docker-compose.yml expone las siguientes herramientas:

### Logs del aplicativo:

```bash
docker logs -f nequi-backend-app
```

### DynamoDB Admin UI:

Se puede explorar visualmente la tabla Dynamo, los eventos, tickets y órdenes abriendo en el navegador:
http://localhost:8001

## 🛑 Detener el entorno
Para detener la aplicación y limpiar la red de contenedores:

```bash
docker-compose down
```

---
## 🧪 Pruebas Unitarias y Cobertura

El proyecto cuenta con una suite de pruebas enfocada en el comportamiento del dominio y la integración de los
adaptadores, asegurando la calidad del código y la prevención de regresiones.

Para ejecutar las pruebas y validar cobertura (JaCoCo):
```bash
./gradlew test jacocoTestReport
```

Una vez ejecutado, el reporte detallado en formato HTML estará disponible sobre la ruta: 
`build/reports/jacocoMergedReport/html/index.html`.
Se puede abrir en el navegador para revisar métricas.

## Endpoints expuestos
Se puede importar la colección de postman que se encuentra sobre
`postman/nequi-backend-tech-challenge.postman_collection.json` la cual comprende todos los endpoints expuestos por
la aplicación, ya cuenta con ejemplos configurados.

### Events (Eventos)
#### `POST /api/v1/events` → Crear nuevo evento <br>
```mermaid
sequenceDiagram
    actor Client
    participant RouterRest
    participant Handler
    participant HandlerValidator
    participant EventUseCase
    participant EventAdapter (DynamoDB)
    participant TicketAdapter (DynamoDB)

    Client->>RouterRest: POST /api/v1/events {name, place, date, capacity}
    RouterRest->>Handler: createEvent(serverRequest)
    Note over Handler: @CircuitBreaker(CREATE_EVENT)

    Handler->>Handler: bodyToMono(CreateEventRequest)
    Handler->>HandlerValidator: validateCreateEvent(request, requestId)
    Note over HandlerValidator: Validates: requestId (UUID),<br/>name, date, place, capacity

    alt Validation errors
        HandlerValidator-->>Handler: Errors
        Handler-->>Client: 400 Bad Request
    else Valid request
        HandlerValidator-->>Handler: Empty Errors

        Handler->>EventUseCase: create(event)
        EventUseCase->>EventUseCase: generate UUID (eventId)
        EventUseCase->>EventAdapter (DynamoDB): createEvent(event, eventId)
        Note over EventAdapter (DynamoDB): Saves event with status = CREATING
        EventAdapter (DynamoDB)-->>EventUseCase: Void

        EventUseCase-->>Handler: eventId
        Handler-->>Client: 201 Created {eventId}

        Note over EventUseCase: Async fire-and-forget<br/>(subscribeOn boundedElastic)
        EventUseCase->>TicketAdapter (DynamoDB): createTickets(eventId, capacity)
        Note over TicketAdapter (DynamoDB): Concurrent batch saves<br/>(Flux.range, flatMap concurrency=25)

        alt Tickets created successfully
            TicketAdapter (DynamoDB)-->>EventUseCase: Void
            EventUseCase->>EventAdapter (DynamoDB): updateEvent(eventId, PUBLISHED)
        else Ticket creation fails
            TicketAdapter (DynamoDB)-->>EventUseCase: Error
            EventUseCase->>EventAdapter (DynamoDB): updateEvent(eventId, FAILED)
        end
    end

```
#### `GET /api/v1/events/{eventId}` → Consultar detalle de un evento <br>
```mermaid
sequenceDiagram
    actor Client
    participant RouterRest
    participant Handler
    participant HandlerValidator
    participant EventUseCase
    participant EventAdapter (DynamoDB)

    Client->>RouterRest: GET /api/v1/events/{eventId}
    RouterRest->>Handler: queryEvent(serverRequest)
    Note over Handler: @CircuitBreaker(QUERY_EVENT)

    Handler->>HandlerValidator: validateQuery(eventId, requestId)
    Note over HandlerValidator: Validates: requestId (UUID),<br/>eventId (UUID)

    alt Validation errors
        HandlerValidator-->>Handler: Errors
        Handler-->>Client: 400 Bad Request
    else Valid request
        HandlerValidator-->>Handler: Empty Errors

        Handler->>EventUseCase: queryEvent(eventId)
        EventUseCase->>EventAdapter (DynamoDB): getEvent(eventId)
        Note over EventAdapter (DynamoDB): GetItem by pk=EVENT#{eventId}<br/>sk=METADATA

        alt Event not found
            EventAdapter (DynamoDB)-->>EventUseCase: Empty Mono
            EventUseCase-->>Handler: BusinessException(NOT_FOUND)
            Handler-->>Client: 404 Not Found
        else Event found
            EventAdapter (DynamoDB)-->>EventUseCase: EventDto → Event
            EventUseCase-->>Handler: Event
            Handler-->>Client: 200 OK {event details}
        end
    end

```
#### `GET /api/v1/events` → Consultar todos los eventos existentes <br>
```mermaid
sequenceDiagram
    actor Client
    participant RouterRest
    participant Handler
    participant HandlerValidator
    participant EventUseCase
    participant EventAdapter (DynamoDB)

    Client->>RouterRest: GET /api/v1/events
    RouterRest->>Handler: queryEvents(serverRequest)
    Note over Handler: @CircuitBreaker(QUERY_EVENTS)

    Handler->>HandlerValidator: validateQueryEvents(requestId)
    Note over HandlerValidator: Validates: requestId (UUID)

    alt Validation errors
        HandlerValidator-->>Handler: Errors
        Handler-->>Client: 400 Bad Request
    else Valid request
        HandlerValidator-->>Handler: Empty Errors

        Handler->>EventUseCase: queryEvents()
        EventUseCase->>EventAdapter (DynamoDB): queryEvents()
        Note over EventAdapter (DynamoDB): Query by GSI EntityTypeIndex<br/>pk = "EVENT"

        EventAdapter (DynamoDB)-->>EventUseCase: List[EventDto] → List[Event]
        EventUseCase-->>Handler: List[Event]
        Handler-->>Client: 200 OK [events]
    end

```
#### `GET /api/v1/events/{eventId}/availability` → Consultar disponibilidad de un evento <br>
```mermaid
sequenceDiagram
    actor Client
    participant RouterRest
    participant Handler
    participant HandlerValidator
    participant EventUseCase
    participant EventAdapter (DynamoDB)

    Client->>RouterRest: GET /api/v1/events/{eventId}/availability
    RouterRest->>Handler: queryAvailability(serverRequest)
    Note over Handler: @CircuitBreaker(QUERY_AVAILABILITY)

    Handler->>HandlerValidator: validateQuery(eventId, requestId)
    Note over HandlerValidator: Validates: requestId (UUID),<br/>eventId (UUID)

    alt Validation errors
        HandlerValidator-->>Handler: Errors
        Handler-->>Client: 400 Bad Request
    else Valid request
        HandlerValidator-->>Handler: Empty Errors

        Handler->>EventUseCase: queryEvent(eventId)
        EventUseCase->>EventAdapter (DynamoDB): getEvent(eventId)
        Note over EventAdapter (DynamoDB): GetItem by pk=EVENT#{eventId}<br/>sk=METADATA

        alt Event not found
            EventAdapter (DynamoDB)-->>EventUseCase: Empty Mono
            EventUseCase-->>Handler: BusinessException(NOT_FOUND)
            Handler-->>Client: 404 Not Found
        else Event found
            EventAdapter (DynamoDB)-->>EventUseCase: EventDto → Event
            EventUseCase-->>Handler: Event
            Note over Handler: Maps only event.getAvailability()
            Handler-->>Client: 200 OK {availability}
        end
    end

```

### Orders (Ordenes)
#### `POST /api/v1/orders` → Crear nueva orden de compra sobre un evento, especificando la cantidad de tickets a reservar <br>
```mermaid
sequenceDiagram
    actor Client
    participant RouterRest
    participant Handler
    participant HandlerValidator
    participant OrderUseCase
    participant EventUseCase
    participant EventAdapter (DynamoDB)
    participant OrderAdapter (DynamoDB)
    participant SQSSender (FIFO Queue)

    Client->>RouterRest: POST /api/v1/orders {eventId, quantity}
    RouterRest->>Handler: createOrder(serverRequest)
    Note over Handler: @CircuitBreaker(CREATE_ORDER)

    Handler->>HandlerValidator: validateCreateOrder(request, requestId)
    Note over HandlerValidator: Validates: requestId (UUID),<br/>eventId (UUID), quantity

    alt Validation errors
        HandlerValidator-->>Handler: Errors
        Handler-->>Client: 400 Bad Request
    else Valid request
        HandlerValidator-->>Handler: Empty Errors

        Handler->>OrderUseCase: create(order)
        OrderUseCase->>EventUseCase: queryEvent(eventId)
        EventUseCase->>EventAdapter (DynamoDB): getEvent(eventId)

        alt Event not found
            EventAdapter (DynamoDB)-->>OrderUseCase: Empty Mono
            OrderUseCase-->>Handler: BusinessException(UNPROCESSABLE_CONTENT)
            Handler-->>Client: 422 Unprocessable Content
        else Event status != PUBLISHED
            EventAdapter (DynamoDB)-->>OrderUseCase: Event
            OrderUseCase-->>Handler: BusinessException(CONFLICT)
            Handler-->>Client: 409 Conflict
        else Event found and PUBLISHED
            EventAdapter (DynamoDB)-->>OrderUseCase: Event

            OrderUseCase->>OrderAdapter (DynamoDB): createOrder(order)
            Note over OrderAdapter (DynamoDB): Saves order with<br/>status = PENDING_CONFIRMATION
            OrderAdapter (DynamoDB)-->>OrderUseCase: Order (with generated orderId)

            OrderUseCase->>SQSSender (FIFO Queue): processOrder(order)
            Note over SQSSender (FIFO Queue): Sends to FIFO purchase queue<br/>messageGroupId = eventId<br/>messageDeduplicationId = orderId
            SQSSender (FIFO Queue)-->>OrderUseCase: Void

            OrderUseCase-->>Handler: orderId
            Handler-->>Client: 201 Created {orderId}
        end
    end

```
#### `GET /api/v1/orders/{orderId}` → Consultar el estado de una orden de compra <br>
```mermaid
sequenceDiagram
    actor Client
    participant RouterRest
    participant Handler
    participant HandlerValidator
    participant OrderUseCase
    participant OrderAdapter (DynamoDB)

    Client->>RouterRest: GET /api/v1/orders/{orderId}
    RouterRest->>Handler: queryOrder(serverRequest)
    Note over Handler: @CircuitBreaker(QUERY_ORDER)

    Handler->>HandlerValidator: validateQuery(orderId, requestId)
    Note over HandlerValidator: Validates: requestId (UUID),<br/>orderId (UUID)

    alt Validation errors
        HandlerValidator-->>Handler: Errors
        Handler-->>Client: 400 Bad Request
    else Valid request
        HandlerValidator-->>Handler: Empty Errors

        Handler->>OrderUseCase: queryOrder(orderId)
        OrderUseCase->>OrderAdapter (DynamoDB): getOrder(orderId)
        Note over OrderAdapter (DynamoDB): GetItem by pk=ORDER#{orderId}<br/>sk=METADATA

        alt Order not found
            OrderAdapter (DynamoDB)-->>OrderUseCase: Empty Mono
            OrderUseCase-->>Handler: BusinessException(NOT_FOUND)
            Handler-->>Client: 404 Not Found
        else Order found
            OrderAdapter (DynamoDB)-->>OrderUseCase: OrderDto → Order
            OrderUseCase-->>Handler: Order
            Handler-->>Client: 200 OK {order details}
        end
    end

```
#### `POST /api/v1/orders/{orderId}/pay` → Efectuar el pago de una orden de compra, confirmado la compra de los tickets previamente reservados <br>
```mermaid
sequenceDiagram
    actor Client
    participant RouterRest
    participant Handler
    participant HandlerValidator
    participant OrderUseCase
    participant OrderAdapter (DynamoDB)
    participant TicketAdapter (DynamoDB)

    Client->>RouterRest: POST /api/v1/orders/{orderId}/pay
    RouterRest->>Handler: payOrder(serverRequest)
    Note over Handler: @CircuitBreaker(PAY_ORDER)

    Handler->>HandlerValidator: validateQuery(orderId, requestId)
    Note over HandlerValidator: Validates: requestId (UUID),<br/>orderId (UUID)

    alt Validation errors
        HandlerValidator-->>Handler: Errors
        Handler-->>Client: 400 Bad Request
    else Valid request
        HandlerValidator-->>Handler: Empty Errors

        Handler->>OrderUseCase: payOrder(orderId)
        OrderUseCase->>OrderAdapter (DynamoDB): getOrder(orderId)
        Note over OrderAdapter (DynamoDB): GetItem by pk=ORDER#{orderId}<br/>sk=METADATA

        alt Order not found
            OrderAdapter (DynamoDB)-->>OrderUseCase: Empty Mono
            OrderUseCase-->>Handler: BusinessException(NOT_FOUND)
            Handler-->>Client: 404 Not Found
        else Order status != RESERVED
            OrderAdapter (DynamoDB)-->>OrderUseCase: Order
            OrderUseCase-->>Handler: BusinessException(PRECONDITION_FAILED)
            Handler-->>Client: 412 Precondition Failed
        else Order found and RESERVED
            OrderAdapter (DynamoDB)-->>OrderUseCase: Order

            OrderUseCase->>TicketAdapter (DynamoDB): confirmTickets(eventId, orderId)
            Note over TicketAdapter (DynamoDB): Query tickets by orderId,<br/>then TransactWriteItems:<br/>- Order → CONFIRMED<br/>- Tickets → SOLD<br/>(ConditionExpression: status = RESERVED)

            alt Transaction fails (concurrent modification)
                TicketAdapter (DynamoDB)-->>OrderUseCase: TechnicalException
                OrderUseCase-->>Handler: TechnicalException
                Handler-->>Client: 500 / 503
            else Transaction succeeds
                TicketAdapter (DynamoDB)-->>OrderUseCase: Void
                OrderUseCase-->>Handler: Void
                Handler-->>Client: 202 Accepted
            end
        end
    end

```

### SQS Listener
#### Process Order FIFO - Queue → Gestiona la validacion de disponibilidad, reserva de ticketes y envia mensaje con delay a cola de liberacion de ticketes.
```mermaid
sequenceDiagram
    participant SQS FIFO Queue
    participant FifoListener
    participant SqsFifoProcessor
    participant OrderUseCase
    participant TicketAdapter (DynamoDB)
    participant OrderAdapter (DynamoDB)
    participant SQSSender (Standard Queue)

    loop Parallel polling (boundedElastic threads)
        FifoListener->>SQS FIFO Queue: receiveMessage(maxMessages, waitTime)
        SQS FIFO Queue-->>FifoListener: Message {order JSON}

        FifoListener->>SqsFifoProcessor: apply(message)
        SqsFifoProcessor->>SqsFifoProcessor: deserialize → ProcessOrderMessage

        SqsFifoProcessor->>OrderUseCase: process(order)
        OrderUseCase->>TicketAdapter (DynamoDB): reserveTickets(eventId, orderId, quantity, ttl)
        Note over TicketAdapter (DynamoDB): Query AVAILABLE tickets,<br/>TransactWriteItems:<br/>- Event availableCount -= qty<br/>- Order → RESERVED<br/>- Tickets → RESERVED<br/>(ConditionExpression on each)

        alt Tickets reserved successfully
            TicketAdapter (DynamoDB)-->>OrderUseCase: Void
            OrderUseCase->>SQSSender (Standard Queue): scheduleOrderRelease(orderId, ttl)
            Note over SQSSender (Standard Queue): Publishes to release queue<br/>with delaySeconds = ttl
            SQSSender (Standard Queue)-->>OrderUseCase: Void
            OrderUseCase-->>SqsFifoProcessor: orderId
            SqsFifoProcessor->>FifoListener: Void (success)
            FifoListener->>SQS FIFO Queue: deleteMessage(receiptHandle)
        else Not enough tickets (BusinessException)
            TicketAdapter (DynamoDB)-->>OrderUseCase: BusinessException(UNAVAILABLE_TICKETS)
            OrderUseCase->>OrderAdapter (DynamoDB): updateOrder(orderId, FAILED)
            OrderAdapter (DynamoDB)-->>OrderUseCase: Void
            OrderUseCase-->>SqsFifoProcessor: Void
            SqsFifoProcessor->>FifoListener: Void
            FifoListener->>SQS FIFO Queue: deleteMessage(receiptHandle)
        else Technical error
            TicketAdapter (DynamoDB)-->>OrderUseCase: TechnicalException
            OrderUseCase-->>SqsFifoProcessor: Error
            Note over FifoListener: onErrorContinue — message<br/>stays in queue → DLQ after retries
        end
    end

```

#### Release Order (Expiration) - Standard Queue → Gestiona la liberación de ticketes reservados los cuales no se hayan confirmado con el pago de la orden
```mermaid
sequenceDiagram
    participant SQS Standard Queue
    participant StandardListener
    participant SqsStandardProcessor
    participant OrderUseCase
    participant OrderAdapter (DynamoDB)
    participant TicketAdapter (DynamoDB)

    Note over SQS Standard Queue: Message becomes visible<br/>after delay (ttl expires)

    loop Parallel polling (boundedElastic threads)
        StandardListener->>SQS Standard Queue: receiveMessage(maxMessages, waitTime)
        SQS Standard Queue-->>StandardListener: Message {orderId}

        StandardListener->>SqsStandardProcessor: apply(message)
        SqsStandardProcessor->>OrderUseCase: release(orderId)

        OrderUseCase->>OrderAdapter (DynamoDB): getOrder(orderId)
        OrderAdapter (DynamoDB)-->>OrderUseCase: Order

        alt Order status != RESERVED (already paid/expired)
            OrderUseCase-->>SqsStandardProcessor: Empty Mono (no-op)
            SqsStandardProcessor->>StandardListener: Void
            StandardListener->>SQS Standard Queue: deleteMessage(receiptHandle)
        else Order still RESERVED (not paid in time)
            OrderUseCase->>TicketAdapter (DynamoDB): releaseTickets(eventId, orderId)
            Note over TicketAdapter (DynamoDB): TransactWriteItems:<br/>- Event availableCount += qty<br/>- Order → EXPIRED<br/>- Tickets → AVAILABLE
            TicketAdapter (DynamoDB)-->>OrderUseCase: Void
            OrderUseCase-->>SqsStandardProcessor: Void
            SqsStandardProcessor->>StandardListener: Void
            StandardListener->>SQS Standard Queue: deleteMessage(receiptHandle)
        end
    end

```
