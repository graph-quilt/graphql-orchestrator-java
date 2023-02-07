package com.intuit.graphql.orchestrator.utils;

import com.google.common.annotations.VisibleForTesting;
import graphql.ExecutionInput;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MultiEIGenerator {

    private List<ExecutionInput> eis = new ArrayList<>();
    private Integer numOfEIs = null;
    private final static String EMPTY_QUERY ="";

    @VisibleForTesting
    private long timeProcessedSplit = 0;

    public MultiEIGenerator(ExecutionInput ei) {
        this.eis.add(ei);
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
                    this.eis.addAll(MultipartUtil.splitMultipartExecutionInput(emittedEI));
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