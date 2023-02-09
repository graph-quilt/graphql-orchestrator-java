package com.intuit.graphql.orchestrator.batch

import com.intuit.graphql.orchestrator.utils.GraphQLUtil
import graphql.language.AstPrinter
import graphql.language.Document
import graphql.language.Field
import graphql.language.FragmentSpread
import graphql.language.InlineFragment
import graphql.language.StringValue
import spock.lang.Specification

import static graphql.language.OperationDefinition.Operation.QUERY

class DownStreamQueryOptimizerSpec extends Specification {

    def "Downstream Query optimizer with fields and fragments"() {
        given:
        DownStreamQueryOptimizer queryOptimizer = new DownStreamQueryOptimizer(QUERY)

        String query = '''                                    
               query myQuery {
               a {  b { c } } 
               a {  b { ...fr } }
               a {  b { ... on F { f } } }
               }
               
               fragment fr on D {  d  } 
            '''

        and:
        final Document document = GraphQLUtil.parser.parseDocument(query)
        final def queryOp = document.getOperationDefinition("myQuery").get()
        def transformedSelectionSet = queryOptimizer.getTransformedSelectionSet(queryOp.getSelectionSet())


        expect:
        String expectedQuery = '''
                query MyQuery {
                a { b { 
                   c .
                   ...fr
                   ... on F { f }  
                }
                
                fragment fr on D {  d  } 
                '''
        transformedSelectionSet.selections.size() == 1
        (transformedSelectionSet.selections.get(0) as Field).name == "a"
        (transformedSelectionSet.selections.get(0) as Field).selectionSet.selections.size() == 1
        ((transformedSelectionSet.selections.get(0) as Field).selectionSet.selections.get(0) as Field).name == "b"
        def selections = ((transformedSelectionSet.selections.get(0) as Field).selectionSet.selections.get(0) as Field).selectionSet.selections
        selections.size() == 3

        selections.stream().anyMatch({ s ->
            (s instanceof Field) ? (s as Field).name == "c" : false
        }
        ) == true

        selections.stream().anyMatch({ s ->
            (s instanceof InlineFragment)
                    ? ((s as InlineFragment).selectionSet.selections.get(0) as Field).name == "f" : false
        }
        ) == true

        selections.stream().anyMatch({ s ->
            (s instanceof FragmentSpread) ? (s as FragmentSpread).name == "fr" : false
        }) == true

    }

    def "Does not merge alias fields with arguments"() {
        given:
        DownStreamQueryOptimizer queryOptimizer = new DownStreamQueryOptimizer(QUERY)

        String query = '''                                    
               query myQuery {
                 f1: taxYear(yr: "2020") { ...rootFr }
                 f2: taxYear(yr: "2021") { ...rootFr }
               }
               
               fragment rootFr on Foo { d }
            '''

        and:
        final Document document = GraphQLUtil.parser.parseDocument(query)
        final def queryOp = document.getOperationDefinition("myQuery").get()
        def transformedSelectionSet = queryOptimizer.getTransformedSelectionSet(queryOp.getSelectionSet())


        expect:
        transformedSelectionSet.selections.size() == 2
        (transformedSelectionSet.selections.get(0) as Field).alias == "f1"
        (transformedSelectionSet.selections.get(0) as Field).name == "taxYear"
        ((transformedSelectionSet.selections.get(0) as Field).arguments.get(0).value as StringValue).value == "2020"

        (transformedSelectionSet.selections.get(1) as Field).alias == "f2"
        (transformedSelectionSet.selections.get(1) as Field).name == "taxYear"
        ((transformedSelectionSet.selections.get(1) as Field).arguments.get(0).value as StringValue).value == "2021"

    }

    def "Does not merge fragments"() {
        //ToDo: Should ideally merge
        given:
        DownStreamQueryOptimizer queryOptimizer = new DownStreamQueryOptimizer(QUERY)

        String query = '''                                    
               query myQuery {
                 f { ...rootFr }
                 f { ...rootFr }
               }
               
               fragment rootFr on Foo { d }
            '''

        and:
        final Document document = GraphQLUtil.parser.parseDocument(query)
        final def queryOp = document.getOperationDefinition("myQuery").get()
        def transformedSelectionSet = queryOptimizer.getTransformedSelectionSet(queryOp.getSelectionSet())

        expect:
        transformedSelectionSet.selections.size() == 1
        (transformedSelectionSet.selections.get(0) as Field).name == "f"

        Document documentop = document.transform({ builder ->
            builder.definitions(
                    Arrays.asList(queryOp.transform({ b -> b.selectionSet(transformedSelectionSet) })))
        });
        println AstPrinter.printAst(documentop)
    }

}
