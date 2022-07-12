# Field Links

## Overview

There will be instances where data from different **_Service Providers_** are related, like how tables are related in 
relational databases.  As an example, let’s look at these GraphQL Schemas from two different services:

Prediction Service
```
type Query {
    topThreeCustomers: [RatedCustomer]
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

Let’s say an Application wants to display the top three customers including the name of each customer.
There will be several calls to GraphQL Orchestrator:

1.  Query prediction service: 
    ```
    query {
        topThreeCustomers: {
            customerId
            ratingResult
        }
    }
    ```
2.  Query customer service for each rated customer:  ` customerById(id: $CustomerId) `


The application needs multiple calls and defeats the purpose of having a single call to a graphql endpoint.

## Solution

```
extend type RatedCustomer {
    # $customerId is a reference to a sibling field
    customerInfo : Customer @resolver(field: "customerById" arguments: [{name : "id", value: "$customerId"}])    
}
```

[@resolver directive](#resolver-directive) section further explains each element in detail.

With the extended `RatedCustomer` type, the application can make **SINGLE CALL** to achieve the objective:

```
query {
    topThreeCustomers: {
        customerId
        ratingResult
        customerInfo: { name }
    }
}
```

Can you guess what happen under the hood? 

1. For field `topThreeCustomers`, orchestrator will call the Prediction Service first which shall return a list of RatedCustomer
2. Then it will resolve `topThreeCustomers.customerInfo` by executing the `@resolver` directive which basically calls the
   Customer Service passing the value of `$customerId`.  Even if the list is of size 3, the orchestrator will only make one
   call to the Customer Service and batch the list together using an alias as shown below
   
```
query {
    id0_O: customerInfo: { name }
    id01_1: customerInfo: { name }
    id02_2: customerInfo: { name }
}
```

## @resolver directive

resolver directive on field definition defines how to resolve a field that belongs to a different provider.


