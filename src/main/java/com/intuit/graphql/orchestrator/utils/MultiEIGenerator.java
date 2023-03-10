package com.intuit.graphql.orchestrator.utils;

import com.google.common.annotations.VisibleForTesting;
import com.intuit.graphql.orchestrator.deferDirective.DeferOptions;
import com.intuit.graphql.orchestrator.visitors.queryVisitors.DeferQueryExtractor;
import graphql.ExecutionInput;
import graphql.analysis.QueryTransformer;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLSchema;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.parser;

@Slf4j
public class MultiEIGenerator {
    private final List<ExecutionInput> eis = new ArrayList<>();
    private final DeferOptions deferOptions;
    private final GraphQLSchema schema;
    private Integer numOfEIs = null;

    @VisibleForTesting
    private long timeProcessedSplit = 0;

    public MultiEIGenerator(ExecutionInput ei, DeferOptions deferOptions, GraphQLSchema schema) {
        this.eis.add(ei);
        this.deferOptions = deferOptions;
        this.schema = schema;
    }

    public Flux<ExecutionInput> generateEIs() {
        return Flux.generate(() -> 0, (indexToProcess, sink) -> {
            ExecutionInput emittedEI = null;
            int nextIndex = indexToProcess + 1;

            //Only emit if it is the initial index or a valid index
            if(this.numOfEIs == null || nextIndex <= this.numOfEIs) {
                emittedEI = this.eis.get(indexToProcess);
                //emit the index that needs to be processed
                sink.next(emittedEI);
            }

            //if null/first iteration then proceed to split ei and add to list of eis needing to be emitted
            if(this.numOfEIs == null) {
                this.timeProcessedSplit = System.currentTimeMillis();
                //Adds elements to list of eis that need to be processed
                try {
                    Document rootDocument = parser.parseDocument(emittedEI.getQuery());
                    Map<String, FragmentDefinition> fragmentDefinitionMap = rootDocument.getDefinitionsOfType(FragmentDefinition.class)
                            .stream()
                            .collect(Collectors.toMap(FragmentDefinition::getName , Function.identity()));

                    ExecutionInput finalEmittedEI = emittedEI;
                    AtomicReference<OperationDefinition> operationDefinitionReference = new AtomicReference<>();
                    rootDocument.getDefinitionsOfType(OperationDefinition.class)
                    .stream()
                    .peek(operationDefinitionReference::set)
                    .map(OperationDefinition::getSelectionSet)
                    .map(SelectionSet::getSelections)
                    .flatMap(List::stream)
                    .forEach(selection -> {
                        QueryTransformer transformer = QueryTransformer.newQueryTransformer()
                                .schema(this.schema)
                                .root(selection)
                                .rootParentType(this.schema.getQueryType())
                                .fragmentsByName(fragmentDefinitionMap)
                                .variables(finalEmittedEI.getVariables())
                                .build();

                        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
                                .deferOptions(deferOptions)
                                .originalEI(finalEmittedEI)
                                .rootNode(rootDocument)
                                .operationDefinition(operationDefinitionReference.get())
                                .fragmentDefinitionMap(fragmentDefinitionMap)
                                .build();

                        transformer.transform(visitor);

                        this.eis.addAll(visitor.getExtractedEIs());
                    });
                }
                catch (Exception ex) {
                    sink.error(ex);
                    sink.complete();
                }
                //sets the number of expected eis which is also the number of responses expected
                this.numOfEIs = this.eis.size();
            } else if(nextIndex > this.numOfEIs) {
                //index reached the end of all the eis that need to be processed
                sink.complete();
            }

            //Call generator with the next index to process
            return nextIndex;
        });
    }

    public Integer getNumOfEIs() {
        return this.numOfEIs;
    }
}