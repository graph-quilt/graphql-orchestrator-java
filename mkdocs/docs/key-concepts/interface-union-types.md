# Interface or Union Types

An interface is an abstract type that includes a certain set of fields that a type must include.

```graphql
interface Pet {
    id: ID!
}

type Dog implements Pet {
    id: ID!
    price: Int
}

type Cat implements Pet {
    id: ID!
    livesLeft: Int!
}
```
Union types are very similar to interfaces, but they don't get to specify any common fields between the types.

```graphql
type Dog  {
    name: String
    price: Int
}

type Cat {
    id: ID
    livesLeft: Int!
}

union Pet = Dog | Cat 
```
[Learn more](https://graphql.org/learn/schema/#interfaces) on interface and union,

### Problem
When the provider has a field that returns a Pet(union or interface) type in the schema, the result can be of type Dog or Cat. The graphql provider serving the Pet type
needs to implement a type-resolver to resolve Pet to either Cat or Dog at query execution time. When the graphql provider registers with the orchestrator, the orchestrator
gets the types Pet, Cat, and Dog, but does not have any information about the business logic of the type resolver.

### Solution
The orchestrator handles such fields by implementing a [generic type resolver](https://github.intuit.com/data-orchestration/stitching/blob/master/src/main/java/com/intuit/graphql/stitching/schema/transform/ExplicitTypeResolver.java) using the introspection field `__typename`.
It transforms the incoming query by adding the field `__typename` so that the provider returns the type name.

Let's take an example query made by the client to the orchestrator for the interface Pet
```graphql
query {
 allPets {
    id
    ... on Dog {
      price
    }
    ... on Cat {
      livesLeft
    }
 }
}
```  
The query that the orchestrator makes to the Pet provider looks like
```graphql
query {
 allPets {
    __typename
    id
    ... on Dog {
      price
    }
    ... on Cat {
      livesLeft
    }
 }
}
```  

------------------------------
(c) Copyright 2021 Intuit Inc.
