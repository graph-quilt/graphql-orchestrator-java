# Field Links

## Overview

There will be instances that data from different **_Data Providers_** are linked, like how tables are linked in 
relational databases.  For example, let’s look at this GraphQL Schemas from two different services which are simplified
for illustration purposes:

Prediction Service
```
type Query {
    topThreeCustomers: TopThreeCustomers
}

type TopThreeCustomers {
    top1: RatedCustomer
    top2: RatedCustomer
    top3: RatedCustomer
}

type RatedCustomer {
    customerId: String
    ratingResult: Int
}
```

Customer Service
```
type Query {
    customerById(id: String): Customer
}

type Customer {
    id: String
    name: String
}
```


## The problem

Let’s say an Application wants to display the top three customers including the name of each customer.  Without 
the linking feature, there will be several calls to GraphQL Orchestrator:

1.  Query prediction service: 
    ```
    query {
        topThreeCustomers: {
            top1: {
                customerId
                ratingResult
            }
            top2: {
                customerId
                ratingResult
            }
            top3: {
                customerId
                ratingResult
            }
        }
    }
    ```
2.  Query top 1 customer:  ` customerById(id: $CustomerId) `
3.  Query top 2 customer:  ` customerById(id: $CustomerId) `
4.  Query top 3 customer:  ` customerById(id: $CustomerId) `

The application needs multiple calls and defeats the purpose of having a single call to a graphql endpoint.

## Solution

```
extend type RatedCustomer {
    # $customerId is a reference to a sibling field
    customerInfo : Customer @resolver(field: "customerById" arguments: [{name : "id", value: "$customerId"}])    
}

# @resolver directive definition 
directive @resolver(field: String, arguments: [ResolverArgument!]) on FIELD_DEFINITION

# Input type for @resolver arguments.
input ResolverArgument {
    name : String!
    value : String!
}
```

[@resolver directive](#resolver-directive) section further explains each element in detail.

With the extended `RatedCustomer` type, the application can make **SINGLE CALL** to achieve the objective:

```
query {
    topThreeCustomers: {
        top1: {
            customerId
            ratingResult
            customerInfo: { name }
        }
        top2: {
            customerId
            ratingResult
            customerInfo: { name }
        }
        top3: {
            customerId
            ratingResult
            customerInfo: { name }
        }
    }
}
```

Can you guess what happen under the hood? 

1. For field `topThreeCustomers.top1`, orchestrator will call the Prediction Service first which shall return customerId and ratingResult
2. Then it will resolve `topThreeCustomers.top1.customerInfo` by executing the `@resolver` directive which basically calls the
   Customer Service passing the value of `$customerId`.  

Same thing happens for `topThreeCustomers.top2` and `topThreeCustomers.top3`.


## @resolver directive

resolver directive on field definition defines how to resolve the a Field Link.

### Directive Definition

Following is the directive definition for @resolver. As of writing, this must be explicitly defined
in the SDL together with the extended type.

    # @resolver directive definition 
    directive @resolver(field: String, arguments: [ResolverArgument!]) on FIELD_DEFINITION
    
    # Input type for @resolver arguments.
    input ResolverArgument {
        name : String!
        value : String!
    }

It defines the target field, its arguments and argument values.  Under the hood, this 
will call the backend service that owns the target field.

#### field

This parameter defines the path of the target field.   The target field should be owned by
a data provider that is already registered in GraphQL Orchestrator.  It has the following syntax

`{rootField}.{childField1}.{childField2}...{targetField}`

where it forms a path from rootField to targetField.

#### arguments

the targetField can have zero or more arguments.  This is an array of `ResolverArgument` where 
each defines

- argumentName.  This should match an argument name of the targetField
- argument value.  The value should conform to [Input Values](https://spec.graphql.org/June2018/#sec-Input-Values) syntax.

#### Examples

1.  target field without parameters

        extend type User {
            allPets : Pet @resolver(field: "allPets")
        }


2.  target field with scalar type argument

        extend type User {
            pet : Pet @resolver(field: "petById" arguments: [{name : "id", value: "$petId"}])
        }

    where $petId is a variable reference to `petId` field in type `User`.

    </br>Note:</br> During execution, parent field will be resolved first.  In this example, will call 
    User service.  The User object/field will be resolved first before resolving the child fields. Thus 
    guarantees that petId will have value.
    

3.  target field with object argument

    Let's say that the target field argument is

        type Query {
            petById(petId: PetId): Pet
        }
        input PetId {
            id: ID
        }

    One can define @resolver like

        extend type User {
            petById : Pet @resolver(field: "petById" arguments: [{name : "petId", value: "{id : \"$petId\"}"}])
        }

    where `$petId` is a variable reference to a field of type `User`
