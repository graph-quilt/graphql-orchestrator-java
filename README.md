<div align="center">

  ![graphql-orchestrator-java](./logo.png)

</div>

A powerful Java library for aggregating and executing GraphQL operations from multiple microservices using a single unified schema.

![Master Build](https://github.com/graph-quilt/graphql-orchestrator-java/actions/workflows/main.yml/badge.svg)
[Builds](https://circleci.com/gh/graph-quilt/graphql-orchestrator-java)

### Introduction

**graphql-orchestrator-java** simplifies the process of accessing data from various GraphQL microservices by providing a unified GraphQL schema. 
This enables you to query multiple microservices through a single endpoint, reducing complexity and improving performance.

The library supports two strategies to aggregate and combine the schemas from multiple microservices
* Schema Stitching with [a recursive strategy](https://graph-quilt.github.io/docs/merging-types/)
* [Apollo Federation Style](https://netflix.github.io/dgs/federation/) Schema Composition. (_Currently, it supports `@key, @requires, @extends, and @external` directives)

At query execution time, it orchestrates the GraphQL queries to the appropriate micro-services, using the popular [graphql-java](https://github.com/graphql-java/graphql-java) 
library as the execution engine.

### Features 

* [Apollo Federation Style Schema Composition](https://netflix.github.io/dgs/federation/)
* [Recursive Stitching](https://graph-quilt.github.io/docs/merging-types/)
* [Query Batching](https://graph-quilt.github.io/docs/graphql-query-execution/)
* [Type Conflict Resolution Strategy](https://graph-quilt.github.io/docs/conflict-resolution/)
* [Pluggable `graphql-java` Instrumentation](https://github.com/graph-quilt/graphql-authorization-java)


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

* Implement the ServiceProvider interface. You will need a new instance for each GraphQL Service.

Consider the following 2 service providers below

```java
class PersonNameService implements ServiceProvider {

  public static final String schema = 
        "type Query { person: Person } " 
      + "type Person { firstName : String lastName: String }";
  
  @Override
  public String getNameSpace() { return "PERSON_NAME"; }

  @Override
  public Map<String, String> sdlFiles() { return ImmutableMap.of("schema.graphqls", schema); }

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

  @Override
  public String getNameSpace() { return "PERSON_ADDRESS";}

  @Override
  public Map<String, String> sdlFiles() { return ImmutableMap.of("schema.graphqls", schema);}

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
The Orchestrator uses a recursive algorithm to combine the schemas from multiple services and generate the following unified schema.

```graphql
type Query {
    person: Person 
}

type Person { 
    firstName: String  
    lastName: String
    address: Address
} 

type Address { 
    city: String 
    state: String 
    zip: String
}
```

### Graph Quilt Gateway

[Graph Quilt Gateway](https://github.com/graph-quilt/graphql-gateway-java) is a SpringBoot application that uses the graphql-orchestrator-java.

### Documentation

Detailed [Documentation](https://graph-quilt.github.io/graphql-orchestrator-java/) can be found here

### Contributing
If you are interested in contributing to this project, please read the [CONTRIBUTING.md](.github/CONTRIBUTING.md) file for details on our code of conduct, and the process for submitting pull requests to us.

### License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### Acknowledgments
- Thanks to the contributors of the [graphql-java](https://github.com/graphql-java/graphql-java) library.
- Thanks to the contributors of this project for their hard work and dedication.

