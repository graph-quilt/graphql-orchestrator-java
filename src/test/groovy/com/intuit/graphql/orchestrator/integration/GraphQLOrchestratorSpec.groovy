package com.intuit.graphql.orchestrator.integration

import graphql.ExecutionInput
import graphql.GraphQLContext
import graphql.language.Document
import helpers.BaseIntegrationTestSpecification

import java.util.function.BiConsumer

import static graphql.language.AstPrinter.printAstCompact

class GraphQLOrchestratorSpec extends BaseIntegrationTestSpecification {

    public static final BiConsumer<ExecutionInput, GraphQLContext> USER_ASSERTS = { executionInput, _context ->
        String query = printAstCompact((Document) executionInput.getRoot())
        if (query.contains("userFragment")) {
            //TODO: When RestExecutorBatchLoader supports Fragments
            assert executionInput.getQuery().contains("fragment", "userFragment", "on", "User")
            assert executionInput.getQuery().doesNotContain("bookFragment")
            assert executionInput.getQuery().doesNotContain("petFragment")
        }
    }

    public static final BiConsumer<ExecutionInput, GraphQLContext> PET_ASSERTS = { executionInput, _context ->
        if (executionInput.getQuery().contains("petFragment")) {
            assert executionInput.getQuery().contains("fragment petFragment on Pet")
            assert executionInput.getQuery().doesNotContain("bookFragment")
        }
    }

    public static final BiConsumer<ExecutionInput, GraphQLContext> BOOK_ASSERTS = { executionInput, _context ->
        if (executionInput.getQuery().contains("bookFragment")) {
            assert executionInput.getQuery().contains("fragment bookFragment on Book")
            assert executionInput.getQuery().doesNotContain("Pet")
        }
    }

}
