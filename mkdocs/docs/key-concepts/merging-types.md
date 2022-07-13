# Recursive Schema Stitching

## Operation type merging

GraphQL Orchestrator combines schema from multiple data providers into a unified schema at runtime. When a data consumer
makes a graphql query to the orchestrator, the orchestrator is able to split the query to appropriate providers, execute
the query and combine the results accurately. The orchestrator performs recursive merging of types using the stitching
algorithm.

Take an example of two providers:

Pet Service
```graphql
type Query {
    allPets: [PetType]
}

type PetType{ ... }
```

User Service
```graphql
type Query {
    user(id: ID!): UserType!
}

type UserType{ ... }
```

The orchestrator starts the stitching process at the Operation i.e the `Query` type. It merges the fields of
provider 1 with provider 2 and creates the final `Query` type.

```graphql
type Query {
    allPets: [PetType]
    user(id: ID!): UserType!
}

type UserType { ... }

type PetType { ... }
```

## Recursive merging

Nested Object types are also merged according to the strategy above as long as the field name and the type name matches. 
Take a look at the example below.

Person Name Service
```graphql
type Query {
    person(id:ID!): PersonType
}

type PersonType {
    firstName: String
    lastName: String
}
```

Person Address Service
```graphql
type Query {
    person(id:ID!): PersonType
}

type PersonType {
    address: AddressType
}

type AddressType {...}
```

GraphQL Orchestrator Schema
```graphql
type Query {
    person(id:ID!): PersonType
}

type PersonType {
    firstName: String
    lastName: String
    address: AddressType # address type is merged
}

type AddressType {...}
```

In the above example, type `Person` is merged because the field name `person`, 
the type name `Person` and the argument list matches.

## Directives and Arguments

The recursive merging strategy does not apply 

* if the nested type (`PersonType`) being merged has directives.
* if the nested type (`PersonType`) being merged has different argument name or type.

Note: The strategy's sole purpose is to have some structure to the final unified schema so that it is easy to explore.

------------------------------
(c) Copyright 2021 Intuit Inc.