# Getting Started

## Pre-requisites
- Java 8 

## Dependency 

```xml
<dependency>
    <groupId>com.intuit.graphql</groupId>
    <artifactId>graphql-orchestrator-java</artifactId>
    <version>${graphql.orchestrator.version}</version>
</dependency>
```

## Usage in code

* Implement the ServiceProvider interface
```java
class TemplateServiceProvider implements ServiceProvider {

  public static final String TEMPLATE = "type Query { nested: Nested } type Nested { %s: String}";
  private String field;

  public TemplateServiceProvider(String field) { this.field = field; }

  @Override
  public String getNameSpace() { return field; }

  @Override
  public Map<String, String> sdlFiles() {
    return ImmutableMap.of(field + ".graphqls", String.format(TEMPLATE, field));
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(final ExecutionInput executionInput, final GraphQLContext context) {
    //{'data':{'nested':{'%s':'%s'}}}"
    Map<String, Object> data = ImmutableMap
        .of("data", ImmutableMap.of("nested", ImmutableMap.of(field, field)));
    return CompletableFuture.completedFuture(data);
  }
}
```

* Create an instance of Orchestrator and execute the query as below.
```java
    // create a runtimeGraph by stitching service providers
    RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder()
        .service(new TemplateServiceProvider("foo"))   
        .service(new TemplateServiceProvider("bar"))  
        .build()
        .stitchGraph();

    // pass the runtime graph to GraphQLOrchestrator
    GraphQLOrchestrator graphQLOrchestrator = GraphQLOrchestrator.newOrchestrator()
        .runtimeGraph(runtimeGraph).build();
    
    //Execute the query 
    CompletableFuture<ExecutionResult> execute = graphQLOrchestrator
        .execute(ExecutionInput.newExecutionInput().query("query {nested {foo bar}}").build());

    ExecutionResult executionResult = execute.get();
    System.out.println(executionResult.getData().toString());
    // Output: {nested={foo=foo, bar=bar}}
```

------------------------------
(c) Copyright 2021 Intuit Inc.