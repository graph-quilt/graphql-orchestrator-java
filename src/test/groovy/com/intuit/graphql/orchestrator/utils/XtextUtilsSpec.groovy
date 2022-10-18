package com.intuit.graphql.orchestrator.utils

import com.intuit.graphql.graphQL.*
import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder
import org.apache.commons.collections4.CollectionUtils
import org.eclipse.xtext.resource.XtextResourceSet
import spock.lang.Specification

import java.util.stream.Collectors
import java.util.stream.Stream

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.createNamedType
import static com.intuit.graphql.orchestrator.utils.XtextUtils.definitionContainsDirective
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getDirectivesWithNameFromDefinition
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.*

class XtextUtilsSpec extends Specification {

    private static XtextResourceSet SCHEMA
    private static XtextResourceSet TYPE
    private static XtextResourceSet SCHEMA_QUERY
    private static XtextResourceSet TYPE_QUERY

    static {
        SCHEMA = TestHelper.toXtextResourceSet("schema { query: Int }")
        SCHEMA_QUERY = TestHelper.toXtextResourceSet("schema { query: foo } \n"
                + "type foo { bar: Int}")
        TYPE_QUERY = TestHelper.toXtextResourceSet("type Query { foo: FooType } \n"
                + "type FooType { bar: Int}")
        TYPE = TestHelper.toXtextResourceSet("type foo { query: Int }")
    }

    def "gets Schema Definition When Present"() {
        given:
        Optional<SchemaDefinition> sd = XtextUtils.findSchemaDefinition(SCHEMA)

        expect:
        sd.isPresent()
        sd.get().getOperationTypeDefinition().size() == 1
        sd.get().getOperationTypeDefinition().get(0).getOperationType().toString() == "query"
    }

    def "does Not Get Schema Definition When Not Present"() {
        given:
        XtextResourceSet set = XtextResourceSetBuilder.newBuilder().file("foo", "type abc { foo: String }").build()
        Optional<SchemaDefinition> sd = XtextUtils.findSchemaDefinition(set)

        when:
        XtextUtils.findSchemaDefinition(null)

        then:
        def exception = thrown(NullPointerException)
        exception in NullPointerException

        !sd.isPresent()
    }

    def "does Notget Operation From Schema Definition When Not Present"() {
        given:
        Optional<SchemaDefinition> sd = XtextUtils.findSchemaDefinition(SCHEMA)
        assert sd.isPresent()

        //Operation not of type object
        Optional<ObjectTypeDefinition> operation = XtextUtils.findOperationType(Operation.QUERY, sd.get())
        assert !operation.isPresent()

        //Operation absent
        Optional<ObjectTypeDefinition> operation1 = XtextUtils.findOperationType(Operation.MUTATION, sd.get())
        assert !operation1.isPresent()

        Optional<ObjectTypeDefinition> operation2 = XtextUtils.findOperationType(Operation.SUBSCRIPTION, sd.get())
        assert !operation2.isPresent()

        when:
        XtextUtils.findOperationType(Operation.QUERY, (SchemaDefinition) null)

        then:
        thrown(NullPointerException)
    }

    def "gets Operation From Schema Definition When Present"() {
        given:
        Optional<SchemaDefinition> sd = XtextUtils.findSchemaDefinition(SCHEMA_QUERY)
        assert sd.isPresent()

        //Operation not of type object
        Optional<ObjectTypeDefinition> operation = XtextUtils.findOperationType(Operation.QUERY, sd.get())
        assert operation.isPresent()
        assert operation.get().getName() == "foo"

        assert operation.get().getFieldDefinition().size() == 1
        assert operation.get().getFieldDefinition().get(0).getName() == "bar"
    }

    def "does Notget Operation From Set When Not Present"() {
        given:
        //Operation not of type object
        Optional<ObjectTypeDefinition> operation = XtextUtils.findOperationType(Operation.QUERY, TYPE)
        assert !operation.isPresent()

        //Operation absent
        Optional<ObjectTypeDefinition> operation1 = XtextUtils.findOperationType(Operation.MUTATION, TYPE_QUERY)
        assert !operation1.isPresent()

        Optional<ObjectTypeDefinition> operation2 = XtextUtils.findOperationType(Operation.SUBSCRIPTION, SCHEMA_QUERY)
        assert !operation2.isPresent()

        when:
        XtextUtils.findOperationType(Operation.QUERY, (XtextResourceSet) null)

        then:
        thrown(NullPointerException)
    }

    def "gets Operation From Set When Present"() {
        given:
        Optional<ObjectTypeDefinition> operation = XtextUtils.findOperationType(Operation.QUERY, TYPE_QUERY)
        assert operation.isPresent()
        operation.get().getName().toLowerCase() == Operation.QUERY.getName().toLowerCase()

        assert operation.get().getFieldDefinition().size() == 1
        operation.get().getFieldDefinition().get(0).getName() == "foo"

        Optional<ObjectTypeDefinition> operation1 = XtextUtils.findOperationType(Operation.QUERY, SCHEMA_QUERY)
        assert operation1.isPresent()
        assert operation1.get().getName().toLowerCase() == "foo"
    }

    def "gets Object From Set When Present"() {
        given:
        Optional<ObjectTypeDefinition> operation = XtextUtils.findObjectType(Operation.QUERY.getName(), TYPE_QUERY)
        assert operation.isPresent()
        assert operation.get().getName().toLowerCase() == Operation.QUERY.getName().toLowerCase()

        assert operation.get().getFieldDefinition().size() == 1
        assert operation.get().getFieldDefinition().get(0).getName() == "foo"

        Optional<ObjectTypeDefinition> operation1 = XtextUtils.findObjectType("foo", SCHEMA_QUERY)
        assert operation1.isPresent()
        assert operation1.get().getName().toLowerCase() == "foo"

        Optional<ObjectTypeDefinition> operation2 = XtextUtils.findObjectType("FooType", TYPE_QUERY)
        assert operation2.isPresent()
        assert operation2.get().getName().toLowerCase() == "FooType".toLowerCase()
    }

    def "doesnt Get Object From Set When Not Present"() {
        given:
        Optional<ObjectTypeDefinition> operation = XtextUtils.findObjectType(Operation.MUTATION.getName(), TYPE_QUERY)
        assert !operation.isPresent()

        Optional<ObjectTypeDefinition> operation1 = XtextUtils
                .findObjectType(Operation.SUBSCRIPTION.getName(), SCHEMA_QUERY)
        assert !operation1.isPresent()

        Optional<ObjectTypeDefinition> operation2 = XtextUtils.findObjectType("BarType", TYPE_QUERY)
        assert !operation2.isPresent()

        when:
        XtextUtils.findObjectType(Operation.QUERY.getName(), (XtextResourceSet) null)

        then:
        thrown(NullPointerException)
    }

    def "parse String From Value With Variable"() {
        given:
        ValueWithVariable valueWithVariable = createValueWithVariable()
        valueWithVariable.setStringValue("\"test\"")

        ValueWithVariable noQuotes = createValueWithVariable()
        noQuotes.setStringValue("test")

        expect:
        XtextUtils.parseString(valueWithVariable) == "test"
        XtextUtils.parseString(noQuotes) == "test"
    }

    def "parse String From Value"() {
        given:
        Value description = createValue()
        description.setStringValue("\"\"\"test_description\"\"\"")
        Value string = createValue()
        string.setStringValue("\"test_string\"")
        Value no_quotes = createValue()
        no_quotes.setStringValue("test_no_quotes")

        expect:
        XtextUtils.parseString(description) == "test_description"
        XtextUtils.parseString(string) == "test_string"
        XtextUtils.parseString(no_quotes) == "test_no_quotes"
    }

    def "get Directives With Name From Definitions Type Def Returns Empty When Not Found"() {
        given:
        TypeDefinition typeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()

        Directive fooDirective = GraphQLFactoryDelegate.createDirective()
        Directive barDirective = GraphQLFactoryDelegate.createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)

        typeDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective))

        List<Directive> result = getDirectivesWithNameFromDefinition(typeDefinition, "Bad")

        expect:
        CollectionUtils.isEmpty(result)
    }

    def "get Directives With Name From Definitions Typed Def Returns Directives"() {
        given:
        TypeDefinition typeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()

        Directive fooDirective = GraphQLFactoryDelegate.createDirective()
        Directive barDirective = GraphQLFactoryDelegate.createDirective()
        Directive bar2Directive = GraphQLFactoryDelegate.createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)
        bar2Directive.setDefinition(barDirectiveDefinition)

        typeDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective, bar2Directive))

        List<Directive> result = getDirectivesWithNameFromDefinition(typeDefinition, "Bar")

        expect:
        CollectionUtils.isNotEmpty(result)
        result.size() == 2
        result.get(0).getDefinition().getName().equals("Bar")
    }

    def "definition Contains Directive Type Def Returns False When Not Found"() {
        given:
        TypeDefinition typeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()

        Directive fooDirective = GraphQLFactoryDelegate.createDirective()
        Directive barDirective = GraphQLFactoryDelegate.createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)

        typeDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective))

        expect:
        !definitionContainsDirective(typeDefinition, "Bad")
    }

    def "definition Contains Directive Typed Def Returns True When Exists"() {
        given:
        TypeDefinition typeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()

        Directive fooDirective = GraphQLFactoryDelegate.createDirective()
        Directive barDirective = GraphQLFactoryDelegate.createDirective()
        Directive bar2Directive = GraphQLFactoryDelegate.createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)
        bar2Directive.setDefinition(barDirectiveDefinition)

        typeDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective, bar2Directive))

        expect:
        definitionContainsDirective(typeDefinition, "Foo")
    }

    def "get Directives With Name From Definitions Field Def Returns Empty When Not Found"() {
        given:
        FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition()

        Directive fooDirective = GraphQLFactoryDelegate.createDirective()
        Directive barDirective = GraphQLFactoryDelegate.createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)

        fieldDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective))

        List<Directive> result = getDirectivesWithNameFromDefinition(fieldDefinition, "Bad")

        expect:
        CollectionUtils.isEmpty(result)
    }

    def "get Directives With Name From Definitions Field Def Returns Directives"() {
        given:
        FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition()

        Directive fooDirective = GraphQLFactoryDelegate.createDirective()
        Directive barDirective = GraphQLFactoryDelegate.createDirective()
        Directive bar2Directive = GraphQLFactoryDelegate.createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)
        bar2Directive.setDefinition(barDirectiveDefinition)

        fieldDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective, bar2Directive))

        List<Directive> result = getDirectivesWithNameFromDefinition(fieldDefinition, "Bar")

        expect:
        CollectionUtils.isNotEmpty(result)
        result.size() == 2
        result.get(0).getDefinition().getName().equals("Bar")
    }

    def "definition Contains Directive Field Def Returns False When Not Found"() {
        given:
        FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition()

        Directive fooDirective = GraphQLFactoryDelegate.createDirective()
        Directive barDirective = GraphQLFactoryDelegate.createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)

        fieldDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective))

        expect:
        !definitionContainsDirective(fieldDefinition, "Bad")
    }

    def "definition Contains Directive Field Def Returns True When Exists"() {
        given:
        FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition()

        Directive fooDirective = GraphQLFactoryDelegate.createDirective()
        Directive barDirective = GraphQLFactoryDelegate.createDirective()
        Directive bar2Directive = GraphQLFactoryDelegate.createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)
        bar2Directive.setDefinition(barDirectiveDefinition)

        fieldDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective, bar2Directive))

        expect:
        definitionContainsDirective(fieldDefinition, "Foo")
    }

    def "get Directives With Name From Definitions Type Ext Def Returns Empty When Not Found"() {
        given:
        ObjectTypeExtensionDefinition typeExtensionDefinition = GraphQLFactoryDelegate.createObjectTypeExtensionDefinition()

        Directive fooDirective = GraphQLFactoryDelegate.createDirective()
        Directive barDirective = GraphQLFactoryDelegate.createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)

        typeExtensionDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective))

        List<Directive> result = getDirectivesWithNameFromDefinition(typeExtensionDefinition, "Bad")

        expect:
        CollectionUtils.isEmpty(result)
    }

    def "get Directives With Name From Definitions Type Ext Def Returns Directives"() {
        given:
        ObjectTypeExtensionDefinition typeExtensionDefinition = GraphQLFactoryDelegate.createObjectTypeExtensionDefinition()

        Directive fooDirective = GraphQLFactoryDelegate.createDirective()
        Directive barDirective = GraphQLFactoryDelegate.createDirective()
        Directive bar2Directive = GraphQLFactoryDelegate.createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)
        bar2Directive.setDefinition(barDirectiveDefinition)

        typeExtensionDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective, bar2Directive))

        List<Directive> result = getDirectivesWithNameFromDefinition(typeExtensionDefinition, "Bar")

        expect:
        CollectionUtils.isNotEmpty(result)
        result.size() == 2
        result.get(0).getDefinition().getName().equals("Bar")
    }

    def "definition Contains Directive Type Ext Def Returns False When Not Found"() {
        given:
        TypeExtensionDefinition typeExtensionDefinition = GraphQLFactoryDelegate.createObjectTypeExtensionDefinition()

        Directive fooDirective = GraphQLFactoryDelegate.createDirective()
        Directive barDirective = GraphQLFactoryDelegate.createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)

        typeExtensionDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective))

        expect:
        !definitionContainsDirective(typeExtensionDefinition, "Bad")
    }

    def "definition Contains Directive Type Ext Def Returns True When Exists"() {
        given:
        TypeExtensionDefinition typeExtensionDefinition = GraphQLFactoryDelegate.createObjectTypeExtensionDefinition()

        Directive fooDirective = GraphQLFactoryDelegate.createDirective()
        Directive barDirective = GraphQLFactoryDelegate.createDirective()
        Directive bar2Directive = GraphQLFactoryDelegate.createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)
        bar2Directive.setDefinition(barDirectiveDefinition)

        typeExtensionDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective, bar2Directive))

        expect:
        definitionContainsDirective(typeExtensionDefinition, "Foo")
    }

    def "getDescriptiveString for an object named type"() {
        given:
        EnumTypeDefinition enumTypeDefinition = GraphQLFactoryDelegate.createEnumTypeDefinition()
        enumTypeDefinition.setName("Arg1")
        ObjectType objectType = GraphQLFactoryDelegate.createObjectType()
        objectType.setType(enumTypeDefinition)
        String res = XtextUtils.toDescriptiveString(objectType)

        expect:
        res == "[name:Arg1, type:EnumTypeDefinition, description:null]"
    }

    def "getDescriptiveString for a list named type"() {
        given:
        ListType listType = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition.setName("Arg1")
        listType.setType(createNamedType(objectTypeDefinition))
        String res = XtextUtils.toDescriptiveString(listType)

        expect:
        res == "[name:Arg1, type:, description:]"
    }


    def "get Directives With Name From Definitions Type Ext Def and DirectiveNames Returns Empty When Not Found"() {
        given:
        ObjectTypeExtensionDefinition typeExtensionDefinition = GraphQLFactoryDelegate.createObjectTypeExtensionDefinition()

        Directive fooDirective = GraphQLFactoryDelegate.createDirective()
        Directive barDirective = GraphQLFactoryDelegate.createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)

        typeExtensionDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective))
        List<String> directiveNames = new ArrayList<>()
        directiveNames.add("Not Bar")
        List<Directive> result = getDirectivesWithNameFromDefinition(typeExtensionDefinition, directiveNames)

        expect:
        CollectionUtils.isEmpty(result)
    }


    def "get Directives With Name From Definitions Typed Def and DirectiveNames arguments Returns Directives"() {
        given:
        TypeDefinition typeDefinition = createObjectTypeDefinition()

        Directive fooDirective = createDirective()
        Directive barDirective = createDirective()
        Directive bar2Directive = createDirective()

        DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition()
        DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition()

        fooDirectiveDefinition.setName("Foo")
        fooDirective.setDefinition(fooDirectiveDefinition)

        barDirectiveDefinition.setName("Bar")
        barDirective.setDefinition(barDirectiveDefinition)
        bar2Directive.setDefinition(barDirectiveDefinition)

        typeDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective, bar2Directive))
        List<String> directiveNames = new ArrayList<>()
        directiveNames.add("Bar")
        List<Directive> result = getDirectivesWithNameFromDefinition(typeDefinition, directiveNames)

        expect:
        CollectionUtils.isNotEmpty(result)
        result.size() == 2
        result.get(0).getDefinition().getName().equals("Bar")
    }

    def "getAllTypeExtensions returns stream of typeExtensionDefinition"() {
        given:
        Stream<TypeExtensionDefinition> typeExtensionDefinitionStream = XtextUtils.getAllTypeExtensions(TYPE)
        expect:
        typeExtensionDefinitionStream.collect(Collectors.toList()).size() == 0

    }
}
