package com.intuit.graphql.orchestrator.federationdirectives.requiredirective;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.TestServiceProvider;
import com.intuit.graphql.orchestrator.federation.exceptions.IncorrectDirectiveArgumentSizeException;
import com.intuit.graphql.orchestrator.schema.transform.RequireTransformer;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirective;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirectiveDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.FEDERATION_REQUIRES_DIRECTIVE;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequireTransformerTest {

    @Mock
    XtextGraph xtextGraphMock;


    RequireTransformer requireTransformer = new RequireTransformer();

    @Test
    public void transformerDoesNotTryToValidateNonFederatedProviders(){
        ServiceProvider serviceProvider = TestServiceProvider
                .newBuilder()
                .serviceType(ServiceProvider.ServiceType.REST)
                .namespace("Rest Provider")
                .build();

        when(xtextGraphMock.getServiceProvider()).thenReturn(serviceProvider);

        requireTransformer.transform(xtextGraphMock);
        verify(xtextGraphMock, never()).getTypes();
    }

    @Test
    public void transformerForwardsExceptionWhenValidatorThrowsIt(){
        ServiceProvider serviceProvider = TestServiceProvider
                .newBuilder()
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .namespace("Fed Provider")
                .build();

        HashMap<String, TypeDefinition> definitionHashMap = new HashMap<>();
        Directive directiveMock = buildDirective(buildDirectiveDefinition(FEDERATION_REQUIRES_DIRECTIVE), new ArrayList<>());
        FieldDefinition fieldDefinitionMock = buildFieldDefinition("fakeField", Arrays.asList(directiveMock));
        TypeDefinition typeDefinitionMock  = buildObjectTypeDefinition("EntityType", Arrays.asList(fieldDefinitionMock));
        definitionHashMap.put("EntityType", typeDefinitionMock);

        XtextGraph xtextGraph = XtextGraph.newBuilder()
                .serviceProvider(serviceProvider)
                .types(definitionHashMap)
                .xtextResourceSet(XtextResourceSetBuilder.newBuilder().build())
                .build();

        requireTransformer.transform(xtextGraph);
    }
}
