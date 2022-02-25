package com.intuit.graphql.orchestrator.federationdirectives.requiredirective;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.federation.exceptions.IncorrectDirectiveArgumentSizeException;
import com.intuit.graphql.orchestrator.schema.transform.RequireTransformer;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirective;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirectiveDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.FEDERATION_REQUIRES_DIRECTIVE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class RequireTransformerTest {

    @Mock
    XtextGraph xtextGraphMock;

    RequireTransformer requireTransformer = new RequireTransformer();

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void transformerDoesNotTryToValidateNonFederatedProviders(){
        ServiceProvider serviceProviderMock = mock(ServiceProvider.class);

        when(xtextGraphMock.getServiceProvider()).thenReturn(serviceProviderMock);
        when(serviceProviderMock.isFederationProvider()).thenReturn(false);


        requireTransformer.transform(xtextGraphMock);
        verify(xtextGraphMock, never()).getTypes();
    }

    @Test(expected = IncorrectDirectiveArgumentSizeException.class)
    public void transformerForwardsExceptionWhenValidatorThrowsIt(){
        ServiceProvider serviceProviderMock = mock(ServiceProvider.class);
        when(xtextGraphMock.getServiceProvider()).thenReturn(serviceProviderMock);
        when(serviceProviderMock.isFederationProvider()).thenReturn(true);

        HashMap<String, TypeDefinition> definitionHashMap = new HashMap<>();
        Directive directiveMock = buildDirective(buildDirectiveDefinition(FEDERATION_REQUIRES_DIRECTIVE), new ArrayList<>());
        FieldDefinition fieldDefinitionMock = buildFieldDefinition("fakeField", Arrays.asList(directiveMock));
        TypeDefinition typeDefinitionMock  = buildObjectTypeDefinition("EntityType", Arrays.asList(fieldDefinitionMock));
        definitionHashMap.put("EntityType", typeDefinitionMock);

        when(xtextGraphMock.getTypes()).thenReturn(definitionHashMap);

        requireTransformer.transform(xtextGraphMock);
    }
}
