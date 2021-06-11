# graphql-orchestrator-java

### Overview

**GraphQL Orchestrator** is a library that exposes data from various data providers using a single unified GraphQL schema.
It aggregates and combines the schemas from these data providers and orchestrates the graphql queries to the appropriate services.
It uses the [graphql-java](https://github.com/graphql-java/graphql-java) library as the runtime execution engine on the unified schema.

### Getting Started

Requirement:
* Java 8
* Maven 3

1.  Add this library to your project  
    ```
    <dependency>
      <groupId>com.intuit.graphql</groupId>
      <artifactId>stitching</artifactId>
      <version>${version}</version>
    </dependency>
    
2.  Implement the [ServiceProvider](https://github.intuit.com/data-orchestration/stitching/blob/master/src/main/java/com/intuit/graphql/stitching/ServiceProvider.java) interface for the services you want to stitch.
    
3.  Use the library in your code.  Example:
    ```
    // Build the stitcher with multiple services. Service interface in progress
    final GraphQLOrchestrator orchestrator = GraphQLOrchestrator.newOrchestrator()
            .instrumentations(Collections.emptyList())
            .services(Collections.emptyList())   // Implemented in Step 2
            .executionIdProvider(ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER)
            .queryExecutionStrategy(new AsyncExecutionStrategy())
            .mutationExecutionStrategy(new AsyncExecutionStrategy())
            .build();
            
    //execute the request
    orchestrator.execute(executionInput);      
    
    ```

### Documentation

Coming soon...

### Contributing

Read the [Contribution guide](./.github/CONTRIBUTING.md)

