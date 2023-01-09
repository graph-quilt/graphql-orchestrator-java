package com.intuit.graphql.orchestrator.batch

import com.intuit.graphql.orchestrator.utils.GraphQLUtil
import graphql.language.Document
import graphql.language.Field
import spock.lang.Specification

import static graphql.language.OperationDefinition.Operation.QUERY

class QueryOptimizerSpec extends Specification {

    def "test Default Methods"() {
        given:
        QueryOptimizer queryOptimizer = new QueryOptimizer(QUERY)

        String query = '''                                    
               query myQuery {
               a {  b { c } } 
               a {  b { d } }
               }
            '''

        and:
        final Document document = GraphQLUtil.parser.parseDocument(query)
        final def queryOp = document.getOperationDefinition("myQuery").get()
        def transformedSelectionSet = queryOptimizer.getTransformedSelectionSet(queryOp.getSelectionSet())


        expect:
        transformedSelectionSet.selections.size() == 1
        (transformedSelectionSet.selections.get(0) as Field).name == "a"
        (transformedSelectionSet.selections.get(0) as Field).selectionSet.selections.size() == 1
        ((transformedSelectionSet.selections.get(0) as Field).selectionSet.selections.get(0) as Field).name == "b"
        ((transformedSelectionSet.selections.get(0) as Field).selectionSet.selections.get(0) as Field).selectionSet.selections.size() == 2


//        Document documentop = document.transform({ builder ->
//            builder.definitions(
//                    Arrays.asList(queryOp.transform({ b -> b.selectionSet(transformedSelectionSet) })))
//        });
//        println AstPrinter.printAst(documentop)


    }
}
