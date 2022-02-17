package com.intuit.graphql.orchestrator.federation;

import static com.intuit.graphql.orchestrator.federation.FederationConstants.FED_FIELD_DIRECTIVE_NAMES_SET;
import static com.intuit.graphql.orchestrator.federation.FederationConstants.FED_TYPE_DIRECTIVES_NAMES_SET;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

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
    if (CollectionUtils.isNotEmpty(nonFederationDirectives)) {
      directives.clear();
      directives.addAll(nonFederationDirectives);
    }
  }

  // TODO move somewhere if not yet present
  public static boolean isFederationDirective(Directive directive) {
    String directiveName = directive.getDefinition().getName();
    return FED_TYPE_DIRECTIVES_NAMES_SET.contains(directiveName)
        || FED_FIELD_DIRECTIVE_NAMES_SET.contains(directiveName);
  }
}
