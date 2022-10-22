## _@rename_

```graphql
# Directive used to rename a type or field during registration
directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE

```
See [Usage](../key-concepts/conflict-resolution.md#_rename_-directive-resolution-strategy)


## _@resolver_

```graphql
    # @resolver directive definition
    directive @resolver(field: String, arguments: [ResolverArgument!]) on FIELD_DEFINITION

    # Input type for @resolver arguments.
    input ResolverArgument {
        name : String!
        value : String!
    }
```

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

- argument name.  This should match an argument name of the targetField
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

See [Usage](../key-concepts/field-links.md)
