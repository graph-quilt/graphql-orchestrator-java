package com.intuit.graphql.orchestrator.federation;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirective;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirectiveDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.DirectiveDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class Federation2PureGraphQLUtilTest {

  private Directive keyDirective1;

  private Directive keyDirective2;

  private Directive externalDirective;

  private Directive requiresDirective;

  private Directive nonFedDirective1;

  private Directive nonFedDirective2;

  @Before
  public void setup() {
    DirectiveDefinition keyDirectiveDefinition = buildDirectiveDefinition("key");
    DirectiveDefinition externalDirectiveDefinition = buildDirectiveDefinition("external");
    DirectiveDefinition requiresDirectiveDefinition = buildDirectiveDefinition("requires");
    DirectiveDefinition nonFedDirectiveDefinition1 = buildDirectiveDefinition("nonFed");
    DirectiveDefinition nonFedDirectiveDefinition2 = buildDirectiveDefinition("nonFed");

    keyDirective1 = buildDirective(keyDirectiveDefinition, Collections.emptyList());
    keyDirective2 = buildDirective(keyDirectiveDefinition, Collections.emptyList());
    externalDirective = buildDirective(externalDirectiveDefinition, Collections.emptyList());
    requiresDirective = buildDirective(requiresDirectiveDefinition, Collections.emptyList());
    nonFedDirective1 = buildDirective(nonFedDirectiveDefinition1, Collections.emptyList());
    nonFedDirective2 = buildDirective(nonFedDirectiveDefinition2, Collections.emptyList());
  }

  @Test
  public void makeAsPureGraphQL_objectTypeDefinitionAllFedDirective_removesAllDirective() {
    FieldDefinition fieldWithExternalDirective = buildFieldDefinition("fieldWithExternalDirective");
    fieldWithExternalDirective.getDirectives().add(externalDirective);

    FieldDefinition fieldWithRequiresDirective = buildFieldDefinition("fieldWithRequiresDirective");
    fieldWithRequiresDirective.getDirectives().add(requiresDirective);

    ObjectTypeDefinition baseObjectType = buildObjectTypeDefinition("EntityType");
    baseObjectType.getDirectives().add(keyDirective1);
    baseObjectType.getDirectives().add(keyDirective2);
    baseObjectType.getFieldDefinition().add(fieldWithExternalDirective);
    baseObjectType.getFieldDefinition().add(fieldWithRequiresDirective);

    Federation2PureGraphQLUtil.makeAsPureGraphQL(baseObjectType);

    assertThat(baseObjectType.getDirectives()).hasSize(0);
    assertThat(fieldWithExternalDirective.getDirectives()).hasSize(0);
    assertThat(fieldWithRequiresDirective.getDirectives()).hasSize(0);
  }

  @Test
  public void makeAsPureGraphQL_objectTypeDefinitionWithNonFedDirective_retainsNonFedDirective() {
    FieldDefinition fieldWithExternalDirective = buildFieldDefinition("fieldWithExternalDirective");
    fieldWithExternalDirective.getDirectives().add(externalDirective);
    fieldWithExternalDirective.getDirectives().add(nonFedDirective1);

    ObjectTypeDefinition baseObjectType = buildObjectTypeDefinition("EntityType");
    baseObjectType.getDirectives().add(keyDirective1);
    baseObjectType.getDirectives().add(nonFedDirective2);

    baseObjectType.getFieldDefinition().add(fieldWithExternalDirective);

    Federation2PureGraphQLUtil.makeAsPureGraphQL(baseObjectType);

    assertThat(baseObjectType.getDirectives()).hasSize(1);
    assertThat(fieldWithExternalDirective.getDirectives()).hasSize(1);
  }
}
