package com.intuit.graphql.orchestrator.xtext

import com.intuit.graphql.graphQL.ObjectTypeDefinition
import com.intuit.graphql.graphQL.SchemaDefinition
import com.intuit.graphql.orchestrator.schema.Operation
import helpers.BaseIntegrationTestSpecification

import static com.intuit.graphql.orchestrator.TestHelper.toXtextResourceSet

class GraphQLResourceSetSpec extends BaseIntegrationTestSpecification {

    private static GraphQLResourceSet SCHEMA
    private static GraphQLResourceSet TYPE
    private static GraphQLResourceSet SCHEMA_QUERY
    private static GraphQLResourceSet TYPE_QUERY

    static {
        SCHEMA = new GraphQLResourceSet(toXtextResourceSet('''
            schema { query: Int }
        '''))

        SCHEMA_QUERY = new GraphQLResourceSet(toXtextResourceSet('''
            schema { query: foo }
            type foo { bar: Int}
        '''))

        TYPE_QUERY = new GraphQLResourceSet(toXtextResourceSet('''
            type query { foo: FooType }
            type FooType { bar: Int}
        '''))

        TYPE = new GraphQLResourceSet(toXtextResourceSet('''
            type foo { query: Int }
        '''))
    }

    void getsSchemaDefinitionWhenPresent() {
        given:
        Optional<SchemaDefinition> sd = SCHEMA.findSchemaDefinition()

        expect:
        sd.isPresent()
        sd.get().getOperationTypeDefinition().size() == 1
        sd.get().getOperationTypeDefinition().get(0).getOperationType().toString() == "query"
    }

    void doesNotGetSchemaDefinitionWhenNotPresent() {
        given:
        GraphQLResourceSet set = new GraphQLResourceSet(
            XtextResourceSetBuilder.newBuilder().file("foo", '''
                type abc { 
                    foo: String 
                }
            ''').build())
        Optional<SchemaDefinition> sd = set.findSchemaDefinition()

        expect:
        !sd.isPresent()
    }

    void doesNotGetOperationFromSchemaDefinitionWhenNotPresent() {
        given:
        Optional<SchemaDefinition> sd = SCHEMA.findSchemaDefinition()

        expect:
        sd.isPresent()

        //Operation not of type object
        SCHEMA.getOperationType(Operation.QUERY) == null
        SCHEMA.getOperationType(Operation.MUTATION) == null
        SCHEMA.getOperationType(Operation.SUBSCRIPTION) == null
    }

    void getsOperationFromSchemaDefinitionWhenPresent() {
        given:
        Optional<SchemaDefinition> sd = SCHEMA_QUERY.findSchemaDefinition()

        expect:
        sd.isPresent()

        //Operation not of type object
        ObjectTypeDefinition operation = SCHEMA_QUERY.getOperationType(Operation.QUERY)
        operation.getName() == "foo"

        operation.getFieldDefinition().size() == 1
        operation.getFieldDefinition().get(0).getName() == "bar"
    }

    void doesNotGetOperationFromSetWhenNotPresent() {
        expect:
        TYPE.getOperationType(Operation.QUERY) == null
        TYPE_QUERY.getOperationType(Operation.MUTATION) == null
        SCHEMA_QUERY.getOperationType(Operation.SUBSCRIPTION) == null
    }

    void getsOperationFromSetWhenPresent() {
        given:
        ObjectTypeDefinition operation = TYPE_QUERY.getOperationType(Operation.QUERY)

        expect:
        operation.getFieldDefinition().size() == 1
        operation.getFieldDefinition().get(0).getName() == "foo"

        ObjectTypeDefinition operation1 = SCHEMA_QUERY.getOperationType(Operation.QUERY)
        operation1.getName() == "foo"
    }

    void getsObjectFromSetWhenPresent() {
        given:
        final ObjectTypeDefinition queryType = TYPE_QUERY.getObjectType(Operation.QUERY.getName())

        expect:
        queryType.getFieldDefinition().size() == 1
        queryType.getFieldDefinition().get(0).getName() == "foo"

        SCHEMA_QUERY.getObjectType("foo") != null

        TYPE_QUERY.getObjectType("FooType") != null
    }

    void doesntGetObjectFromSetWhenNotPresent() {
        expect:
        TYPE_QUERY.getObjectType(Operation.MUTATION.getName()) == null
        SCHEMA_QUERY.getObjectType(Operation.SUBSCRIPTION.getName()) == null
        TYPE_QUERY.getObjectType("BarType") == null
    }

}