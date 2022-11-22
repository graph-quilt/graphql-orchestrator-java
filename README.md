<div align="center">

  ![graphql-orchestrator-java](./logo.png)

</div>

[![CircleCI](https://dl.circleci.com/status-badge/img/gh/graph-quilt/graphql-orchestrator-java/tree/master.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/graph-quilt/graphql-orchestrator-java/tree/master)

![Master Build](https://github.com/graph-quilt/graphql-orchestrator-java/actions/workflows/main.yml/badge.svg)



[Builds](https://circleci.com/gh/graph-quilt/graphql-orchestrator-java)


**graphql-orchestrator-java** is a library that combines the schema from various GraphQL microservices into a single unified GraphQL schema.
It uses [a recursive strategy](./mkdocs/docs/key-concepts/merging-types.md) to aggregate and combine the schemas from these micro-services 
and [orchestrates the graphql queries](./mkdocs/docs/key-concepts/graphql-query-execution.md) to the appropriate services
at runtime.

It also supports Apollo Federation directives for schema composition. Currently, it supports `@key, @requires, @extends, and @external` directives.

It uses the [graphql-java](https://github.com/graphql-java/graphql-java) library as the runtime execution engine for the unified schema.

## Getting Started

### Dependency

```xml
<dependency>
    <groupId>com.intuit.graphql</groupId>
    <artifactId>graphql-orchestrator-java</artifactId>
    <version>${graphql.orchestrator.version}</version>
</dependency>
```

### Usage in code

* Implement the ServiceProvider interface. You will a new instance for each GraphQL Service.
Consider the following 2 services below

```java
class PersonNameService implements ServiceProvider {

  public static final String schema = 
        "type Query { person: Person } " 
      + "type Person { firstName : String lastName: String }";
  
  // Unique namespace for the service
  @Override
  public String getNameSpace() { return "PERSON_NAME"; }

  // GraphQL Schema
  @Override
  public Map<String, String> sdlFiles() {
    return ImmutableMap.of("schema.graphqls", schema);
  }

  // Query execution at runtime, the response needs to have data and error objects as per GraphQL Spec
  @Override
  public CompletableFuture<Map<String, Object>> query(final ExecutionInput executionInput, 
      final GraphQLContext context) {
    //{'data':{'person':{'firstName':'GraphQL Orchestrator', 'lastName': 'Java'}}}"
    Map<String, Object> data = ImmutableMap
        .of("data", ImmutableMap.of("person", ImmutableMap.of("firstName", "GraphQL Orchestrator", "lastName", "Java")));
    return CompletableFuture.completedFuture(data);
  }
}
```

```java
class PersonAddressService implements ServiceProvider {

  public static final String schema = 
        "type Query { person: Person }"
      + "type Person { address : Address }"
      + "type Address { city: String state: String zip: String}";

  // Unique namespace for the service
  @Override
  public String getNameSpace() { return "PERSON_ADDRESS";}

  // GraphQL Schema
  @Override
  public Map<String, String> sdlFiles() { return ImmutableMap.of("schema.graphqls", schema);}

  // Query execution at runtime, the response needs to have data and error objects as per GraphQL Spec
  @Override
  public CompletableFuture<Map<String, Object>> query(final ExecutionInput executionInput,
      final GraphQLContext context) {
    //{'data':{'person':{'address':{ 'city' 'San Diego', 'state': 'CA', 'zip': '92129' }}}}"
    Map<String, Object> data = ImmutableMap
        .of("data", ImmutableMap.of("person", ImmutableMap.of("address", ImmutableMap.of("city","San Diego", "state","CA", "zip","92129"))));
    return CompletableFuture.completedFuture(data);
  }
}
```

* Create an instance of Orchestrator and execute the query as below.
```java

    // create a runtimeGraph by stitching service providers
    RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder()
        .service(new PersonNameService())   
        .service(new PersonAddressService())  
        .build()
        .stitchGraph();

    // pass the runtime graph to GraphQLOrchestrator
    GraphQLOrchestrator graphQLOrchestrator = GraphQLOrchestrator.newOrchestrator()
        .runtimeGraph(runtimeGraph).build();
    
    //Execute the query 
    CompletableFuture<ExecutionResult> execute = graphQLOrchestrator
        .execute(
            ExecutionInput.newExecutionInput()
              .query("query {person {firstName lastName address {city state zip}}}")
              .build()
        );

    ExecutionResult executionResult = execute.get();
    System.out.println(executionResult.getData().toString());
    // Output: 
   // {person={firstName=GraphQL Orchestrator, lastName=Java, address={city=San Diego, state=CA, zip=92129}}}
```

------------------------------

### Graph Quilt Gateway

[Graph Quilt Gateway](https://github.com/graph-quilt/graph-quilt-gateway) is a GraphQL SpringBoot application that uses this library as an orchestrator.

### Documentation

Detailed [Documentation](https://graph-quilt.github.io/graphql-orchestrator-java/) can be found here

### Contributing

Read the [Contribution guide](./.github/CONTRIBUTING.md)

