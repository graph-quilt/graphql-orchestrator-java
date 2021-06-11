# High Level Algorithm

![High Level Algorithm](../img/arch/algorithm.png)

The orchestrator merges the schema using a very simple merging algorithm. It uses Xtext to parse the schema files into 
an Abstract Syntax Tree(AST) representation. The Xtext internally uses Antlr, but provides some additional features like
cross-referencing, custom validations, etc. that has been very helpful to operate on the graph.
The Algorithm consists of 3 phases - 

### Pre-Merge Transformers 

This phase consists of performing a set of transformations to the individual providers schemas. Such transformations enrich 
the xtext AST. Some examples include attaching namespace to the description of all the types of the provider, marking if the 
schema contains union or interfaces etc. The top level fields inside Query type are also marked with appropriate DataFetcher
context to prepare the graphql-java code registry.

### Merging 

The individual schemas are merged to form a single unified schema using a [recursive strategy](merging-types.md). The output is a Xtext AST
that can further be enriched by applying post merge transformations. During this phase, the graphql-java code registry is 
also modified as per the strategy. 

### Post-Merge Transformers

The post merge transformers applies to the final unified schema. One such transformation converts the Xtext AST to an
executable GraphQLSchema which can be used to execute GraphQL queries on the orchestrator. 
    
------------------------------
(c) Copyright 2021 Intuit Inc.
