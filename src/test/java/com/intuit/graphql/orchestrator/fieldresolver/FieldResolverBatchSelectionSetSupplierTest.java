package com.intuit.graphql.orchestrator.fieldresolver;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InputObjectTypeDefinition;
import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FieldResolverBatchSelectionSetSupplierTest {

    private FieldResolverBatchSelectionSetSupplier subject;

    private final List<DataFetchingEnvironment> dataFetchingEnvironments = new ArrayList<>();

    @Mock
    private DataFetchingEnvironment mockDataFetchingEnvironment;

    @Mock
    private Field dfeField;

    @Mock
    private SelectionSet dfeFieldSelectionSet;

    @Mock
    private ObjectTypeDefinition mockUserTypeDefinition;

    @Mock
    private FieldDefinition mockPetFieldDefinitionMock;

    private ResolverDirectiveDefinition resolverDirectiveDefinition;

    @Before
    public void setup() {
        /*  SCENARIO
        type Query {
            petByObjectId(petId: PetId): Pet
        }

        input PetId {
            id: String
        }

        type User {
            name: String
            petId: String
        }

        extend type User {
            pet : Pet @resolver(field: "petById" arguments: [{name : "petId", value: "{ id : \"$petId\" }"}])
        }
        */
        dataFetchingEnvironments.add(mockDataFetchingEnvironment);
    }

    @Test
    public void testWithObjectArgument() {
        // GIVEN
        Map<String, Object> dataSource = new HashMap<>();
        dataSource.put("petId","pet-901");
        when(mockDataFetchingEnvironment.getSource()).thenReturn(dataSource);
        when(mockDataFetchingEnvironment.getField()).thenReturn(dfeField);
        when(dfeField.getSelectionSet()).thenReturn(dfeFieldSelectionSet);

        InputObjectTypeDefinition petIdType = GraphQLFactoryDelegate.createInputObjectTypeDefinition();
        petIdType.setName("PetId");

        ObjectType objectType = GraphQLFactoryDelegate.createObjectType();
        objectType.setType(petIdType);

        List<ResolverArgumentDefinition> resolverArgumentDefinitions = new ArrayList<>();
        ResolverArgumentDefinition resolverArgument = new ResolverArgumentDefinition("name", "{ id : \"$petId\" }", objectType);
        resolverArgumentDefinitions.add(resolverArgument);
        resolverDirectiveDefinition = new ResolverDirectiveDefinition("petByObjectId", resolverArgumentDefinitions);

        FieldResolverContext fieldResolverContext = FieldResolverContext.builder()
                .parentTypeDefinition(mockUserTypeDefinition)
                .fieldDefinition(mockPetFieldDefinitionMock)
                .requiresTypeNameInjection(true)
                .serviceNamespace("PETSTORE")
                .resolverDirectiveDefinition(resolverDirectiveDefinition)
                .build();

        String[] resolverSelectedFields = new String[] {"petById"};

        // GIVEN
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments, fieldResolverContext);

        // THEN
        SelectionSet actual = subject.get();

        assertThat(actual).isNotNull();
    }

}

