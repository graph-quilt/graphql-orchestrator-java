package com.intuit.graphql.orchestrator.federation;

import static com.intuit.graphql.orchestrator.utils.FederationUtils.isFederationDirective;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import java.util.List;
import java.util.stream.Collectors;

public class Federation2PureGraphQLUtil {

  public static void makeAsPureGraphQL(TypeDefinition typeDefinition) {
    List<Directive> directives = typeDefinition.getDirectives();
    removeFederationDirectives(directives);

    List<FieldDefinition> fieldDefinitions = getFieldDefinitions(typeDefinition);
    fieldDefinitions.forEach(Federation2PureGraphQLUtil::makeAsPureGraphQL);
  }

  private static void makeAsPureGraphQL(FieldDefinition fieldDefinition) {
    List<Directive> directives = fieldDefinition.getDirectives();
    removeFederationDirectives(directives);
  }

  private static void removeFederationDirectives(List<Directive> directives) {
    List<Directive> nonFederationDirectives =
        directives.stream()
            .filter(directive -> !isFederationDirective(directive))
            .collect(Collectors.toList());
    directives.clear();
    directives.addAll(nonFederationDirectives);
  }

}
