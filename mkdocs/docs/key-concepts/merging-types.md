# Merging types

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

The orchestrator starts the stitching process at the very top-level i.e the `Query` type. It merges the fields of
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

Nested types are also merged according to the strategy above.

```graphql
type Query {
    person: PersonType
}

type PersonType {
    firstName: String
    lastName: String
}
```
```graphql
type Query {
    person: PersonType
}

type PersonType {
    address: AddressType
}

type AddressType {...}
```

```graphql
type Query {
    person: PersonType
}

type PersonType {
    firstName: String
    lastName: String
    address: AddressType # address type is merged
}

type AddressType {...}
```

## Directives and Arguments

The recursive merging strategy does not apply if the nested type being merged has an arguments or directives. The strategy's purpose
is to have some structure to the final unified schema so that it is easy to explore.

------------------------------
(c) Copyright 2021 Intuit Inc.