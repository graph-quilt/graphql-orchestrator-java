# GraphQL Query Execution

GraphQL Orchestrator uses the [graphql-java](https://github.com/graphql-java/graphql-java) library for query execution at runtime. In this section, we will discuss how
the Orchestrator breaks up an incoming query into multiple sub-queries to be sent to their respective service providers.

## Deconstructing your query | Rebuilding the response

GraphQL Orchestrator partitions a query into sub-queries, so that downstream services only get queries for the schema that they operate on.

Given a scenario, imagine that `query.person/spouse.firstName` are served by NameService, and `query.person/spouse.address.city` is
served by Address. Here is a query that requests for both pieces of information:

```graphql
query {
    person {
        firstName
        address {
            city
        }
    }
    spouse {
        firstName
        address {
            city
        }
    }
}
```

GraphQL Orchestrator will split this query into two subqueries and call the appropriate providers in parallel:

```graphql
query {
    person {
        firstname
    }
    spouse {
        firstname
    }
}

query {
    person {
        address {
            ciy
        }
    }
    spouse {
        address {
            ciy
        }
    }
}
```

Note that GraphQL Orchestrator will preserve the query hierarchy no matter how deep it must traverse to split the query.

After retrieving the individual responses from separate services, the orchestrator combines both GraphQL errors and data
into a single response for the client.

##  Batching queries

Even though the firstName field is at a different level for person and spouse, they belong to the same provider.
The GraphQL Orchestrator will make sure that the fields that belong to the NameService provider as batched together
before calling the provider. In simple words,  the GraphQL Orchestrator will make sure that the downstream provider
is only called once per request, regardless of the schema hierarchy.

------------------------------
(c) Copyright 2021 Intuit Inc.
