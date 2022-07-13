## Type Conflict Resolution

In a distributed GraphQL world, your GraphQL schema is developed, parsed, and validated independently of other
GraphQL schemas. Your schema is eventually integrated with other GraphQL schemas in the orchestrator, where 
we merge and stitch schemas together to produce a single executable schema.

The orchestrator performs validations to ensure the merged schema conforms to the GraphQL specification, and ensures that your
schema can be correctly merged into the single schema. Your schema could be validated against hundreds of other schemas
that already exist in the orchestrator! Thus, how you define your types becomes important because you could be clashing with
many different providers all with similar type names and schema structures.

With many different providers producing GraphQL types, you are bound to run into collisions when trying to merge schemas
together.

### Resolution Strategies

Let's say you have an object-type `Profile` which is conflicting with the orchestrator.
When we say a type conflicts with the orchestrator, it really means that it is conflicting with some other provider that 
has already registered type `Profile`. You can contact the provider and discuss the conflicting type with them and see if it qualifies
for type extension. But let's say, the type `Profile` is *semantically* different from the other providers `Profile` type.
Below are some of the strategies you can use to resolve conflicts.

### Type Renaming Resolution Strategy

Suppose you are a User service trying to expose a `Profile` type representing a user's profile.

```graphql
type Query {
  userProfile: Profile
}

type Profile {
  userId: ID
  firstName: String
  lastName: String
  age: Int
}
```

Looks good on paper, but when you try to integrate with the orchestrator, it rejects your schema because Profile is conflicting
with another type `Profile` defined by another service!

```graphql
type Query {
  financialProfile: Profile
}

type Profile {
  income: Float
  debt: Float
}
```

**How do we fix this?**

Be as specific as possible when building your schema types. For example, a `Profile` type can be made more specific by
answering the following questions:

* What specific profile is this (Finance, User)?
* Does your type belong to a specific product (TurboTax, Mint)?

The results of answering the questions above and applying it to the `Profile` type could be:

* `TurboTaxUserProfile`
* `MintUserProfile`

### _@rename_ Directive Resolution Strategy

This resolution strategy is useful for initial on-boarding purposes where you have clients that already call your
service's GraphQL endpoint, but you need to rename conflicting types in order to register with the orchestrator.

Suppose that you need to change your `Profile` type to `TurboTaxUserProfile`. However, you have existing clients that
query your profile with fragments and fragment definitions which require the actual type name:

```graphql
query {
  profile {
    ...userProfile
  }
}

fragment userProfile on Profile {
  userId
  firstName
  lastName
}
```

You cannot rename the type `Profile` to something else because the new type will cause the queries of existing clients
with these fragments to fail!

**How do we fix this?**

With **_@rename_** directive, you can expose type `Profile` in your service (and accept queries that specify the
`Profile` type), but expose that type as something else through the orchestrator. For example, an introspection on your
service would yield `Profile`, but an introspection on the orchestrator would yield `Finance_Profile`


```graphql
type Query {
  financialProfile: Profile
}

type Profile @rename(to: "Financial_Profile"){
  income: Float
  debt: Float
}
```

In the example configuration below, types `Profile` will become `Financial_Profile` in the orchestrator schema, 
but will remain `Profile` in your service. Subsequently, at runtime, when the orchestrator calls your service to resolve `Financial_Profile`, 
the namespace information will be stripped away, and your service will receive queries for `Profile`.

## Field Conflict Resolution
When the number of providers increase, you might run into a scenario where 2 providers provide
the same field names. To get past it, you can use the _@rename_ directive at the field level and resolve 
such field level naming conflicts. 






