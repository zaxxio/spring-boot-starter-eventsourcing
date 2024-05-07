# Eventa (Infrastructure Ready)

## Overview

This library provides a robust infrastructure for implementing the Command Query Responsibility Segregation (CQRS) pattern along with Event Sourcing in Java applications. CQRS separates the responsibility of handling commands (write operations) from queries (read operations), while Event Sourcing ensures that changes to the application state are captured as a sequence of events.

## Key Features

- **Command Handling:** Easily define command handlers to process incoming commands for creating, updating, or deleting domain entities.
- **Event Sourcing:** Implement event sourcing to persist and replay domain events, ensuring a full audit trail of state changes.
- **Aggregate Root Support:** Simplify the management of domain aggregates with built-in support for Aggregate Roots.
- **Annotation-Based Configuration:** Configure command handlers and event sourcing using annotations, reducing boilerplate code and promoting clean architecture.
- **Scalable and Extensible:** Designed to scale with the complexity of the application and easily extend functionality with custom components.
- **Asynchronous Processing:** Support for asynchronous processing of commands and events for improved performance and responsiveness.

## Usage

1. **Define Commands and Events:** Create classes to represent commands and events that encapsulate changes to domain entities.
2. **Implement Command Handlers:** Define command handler methods to process incoming commands and update the state of aggregates.
3. **Apply Event Sourcing:** Utilize event sourcing to persist domain events and rebuild aggregate state from event streams.
4. **Configure Infrastructure:** Set up infrastructure components such as command buses, event stores, and event processors to manage command and event handling.
5. **Integrate with Application:** Integrate the library with your application to leverage its CQRS and Event-Sourcing capabilities seamlessly.

## Sample Aggregate

```java
@Aggregate
@NoArgsConstructor
public class ProductAggregate extends AggregateRoot { // AggregateRoot Extends

    private String productName;
    private Double quantity;
    private Double price;

    @CommandHandler(constructor = true) // to create first aggregate
    public void handle(CreateProductCommand createProductCommand) { // You command to create
        apply(
                ProductCreatedEvent.builder()
                        .id(createProductCommand.getId())
                        .productName(createProductCommand.getProductName())
                        .quantity(createProductCommand.getQuantity())
                        .price(createProductCommand.getPrice())
                        .build()
        );
    }

    @EventSourcingHandler
    public void on(ProductCreatedEvent productCreatedEvent) { // apply event-sourcing to build the current state
        super.id = productCreatedEvent.getId();
        this.productName = productCreatedEvent.getProductName();
        this.price = productCreatedEvent.getPrice();
        this.quantity = productCreatedEvent.getQuantity();
    }

    @CommandHandler
    public void handle(UpdateProductCommand updateProductCommand) { // modifying on existing aggregate
        apply(
                ProductUpdatedEvent.builder()
                        .id(updateProductCommand.getId())
                        .productName(updateProductCommand.getProductName())
                        .quantity(updateProductCommand.getQuantity())
                        .price(updateProductCommand.getPrice())
                        .build()
        );
    }

    @EventSourcingHandler
    public void on(ProductUpdatedEvent productUpdatedEvent) { // apply event-sourcing to build the current state
        super.id = productUpdatedEvent.getId();
        this.productName = productUpdatedEvent.getProductName();
        this.price = productUpdatedEvent.getPrice();
        this.quantity = productUpdatedEvent.getQuantity();
    }
}
```
## Sample Projection Group
```java
@Log4j2
@Service
@ProjectionGroup
public class ProductProjection {

    @EventHandler
    public void on(ProductCreatedEvent productCreatedEvent) {
        System.out.println("Product " + productCreatedEvent);
    }

    @EventHandler
    public void on(ProductUpdatedEvent productUpdatedEvent) {
        System.out.println("Product " + productUpdatedEvent);
    }

    @QueryHandler
    public List<Integer> handle(FindByProductIdQuery findByProductIdQuery) {
        return List.of(1, 2, 3, 4, 5);
    }

}

```
##  Command Dispatcher
```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductCommandController {

    private final CommandDispatcher commandDispatcher;

    @PostMapping
    public ResponseEntity<?> createProduct(ProductDTO productDTO) throws Exception {

        final CreateProductCommand createProductCommand = CreateProductCommand.builder()
                .id(productDTO.getId())
                .productName(productDTO.getProductName())
                .quantity(productDTO.getQuantity())
                .price(productDTO.getPrice())
                .build();

        commandDispatcher.send(createProductCommand);
        return ResponseEntity.ok("");
    }


    @PutMapping
    public ResponseEntity<?> updateProduct(ProductDTO productDTO) throws Exception {

        final UpdateProductCommand updateProductCommand = UpdateProductCommand.builder()
                .id(productDTO.getId())
                .productName(productDTO.getProductName())
                .quantity(productDTO.getQuantity())
                .price(productDTO.getPrice())
                .build();

        commandDispatcher.send(updateProductCommand);
        return ResponseEntity.ok("");
    }

}

```

## Query Dispatcher

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class QueryCommandHandler {
    private final QueryDispatcher queryDispatcher;

    @GetMapping
    public ResponseEntity<?> getExample(UUID id) throws Exception {
        FindByProductIdQuery findByProductIdQuery = FindByProductIdQuery
                .builder()
                .productId(id)
                .build();
        List<Integer> result = queryDispatcher.dispatch(findByProductIdQuery, ResponseType.multipleInstancesOf(Integer.class));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/items")
    public ResponseEntity<?> getItem(UUID id) throws Exception {
        ItemQuery itemQuery = ItemQuery.builder().build();
        List<Integer> result = queryDispatcher.dispatch(itemQuery, ResponseType.multipleInstancesOf(Integer.class));
        return ResponseEntity.ok(result);
    }
}
```

## Infrastructure Dependency
```yaml
spring:
  application:
    name: spring-boot-app
  # Kafka Bootstrap Server's
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.UUIDSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: eventa-consumer-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.UUIDDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring:
          json:
            trusted:
              packages: '*'
    listener:
      missing-topics-fatal: false
      ack-mode: manual
  # Mongo DB
  data:
    mongodb:
      repositories:
        type: imperative
      authentication-database: admin
      auto-index-creation: true
      database: eventstore
      username: username
      password: password
      port: 27017
      host: localhost
```

## Copyright 
```text
Copyright 2022 Partha Sutradhar

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```