package com.intuit.graphql.orchestrator.utils;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeExtensionDefinition;
import com.intuit.graphql.graphQL.ObjectTypeExtensionDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.exceptions.EmptyFieldsArgumentFederationDirective;
import com.intuit.graphql.orchestrator.federation.exceptions.InvalidFieldSetReferenceException;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.parser.Parser;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class FederationUtils {
    public static final String FEDERATION_KEY_DIRECTIVE = "key";
    public static final String FEDERATION_EXTERNAL_DIRECTIVE = "external";
    public static final String FEDERATION_EXTENDS_DIRECTIVE = "extends";
    public static final String FEDERATION_REQUIRES_DIRECTIVE = "requires";
    public static final String FEDERATION_PROVIDES_DIRECTIVE = "provides";

  public static final Set<String> FED_TYPE_DIRECTIVES_NAMES_SET =
      new HashSet<>(Arrays.asList(FEDERATION_KEY_DIRECTIVE, FEDERATION_EXTENDS_DIRECTIVE));

  public static final Set<String> FED_FIELD_DIRECTIVE_NAMES_SET =
      new HashSet<>(
          Arrays.asList(
              FEDERATION_EXTERNAL_DIRECTIVE,
              FEDERATION_REQUIRES_DIRECTIVE,
              FEDERATION_PROVIDES_DIRECTIVE));

    private FederationUtils(){}

    public static void checkFieldSetValidity(XtextGraph sourceGraph, TypeDefinition typeDefinition, String fieldSet, String originatingDirective) {
        Objects.requireNonNull(sourceGraph);
        Objects.requireNonNull(typeDefinition);
        if(StringUtils.isBlank(fieldSet)) {
            throw new EmptyFieldsArgumentFederationDirective(typeDefinition.getName(), originatingDirective);
        }

        if(!fieldSet.startsWith("{")) {
            fieldSet = StringUtils.join(StringUtils.SPACE, "{", fieldSet, "}");
        }

        //Throws InvalidSyntaxException if fieldSet is incorrect
        Document fieldSetDocument = Parser.parse(fieldSet);

        List<OperationDefinition> definitions = fieldSetDocument.getDefinitions().stream()
                .map(OperationDefinition.class::cast).collect(Collectors.toList());

        List<FieldDefinition> typeFieldDefinitions = getFieldDefinitions(typeDefinition);

        for( final OperationDefinition definition : definitions) {
            List<Field> fields = definition.getSelectionSet().getSelections().stream().map(Field.class::cast).collect(Collectors.toList());
            for(Field field : fields) {
                checkFieldReferenceRecursively(sourceGraph,typeDefinition.getName(), typeFieldDefinitions, field);
            }
        }
    }

    private static void checkFieldReferenceRecursively(XtextGraph sourceGraph, String typeName, List<FieldDefinition> declaredDefinitions, Field fieldToCheck) {
        String fieldName = fieldToCheck.getName();
        Optional<FieldDefinition> optionalFieldDefinition = declaredDefinitions.stream()
                .filter(fieldDefinition -> fieldDefinition.getName().equals(fieldName))
                .findFirst();

        if(optionalFieldDefinition.isPresent()) {
            if(CollectionUtils.isNotEmpty(fieldToCheck.getChildren())) {
                FieldDefinition fieldDefinition = optionalFieldDefinition.get();
                TypeDefinition childType = sourceGraph.getType(fieldDefinition.getNamedType());

                //should only be 1 element, but put in loop just in case something changes later
                for(SelectionSet childSelectionSet: fieldToCheck.getChildren().stream().map(SelectionSet.class::cast).collect(Collectors.toList())) {
                    for(Field childField : getFieldsFromSelectionSet(childSelectionSet)) {
                        checkFieldReferenceRecursively(sourceGraph, childType.getName(), getFieldDefinitions(childType), childField);
                    }
                }
            }
        } else {
            throw new InvalidFieldSetReferenceException(fieldName, typeName);
        }
    }

    private static List<Field> getFieldsFromSelectionSet(SelectionSet selectionSet) {
        return selectionSet.getSelections().stream().map(Field.class::cast).collect(Collectors.toList());
    }

    public static boolean isFederationDirective(Directive directive) {
        String directiveName = directive.getDefinition().getName();
        return FED_TYPE_DIRECTIVES_NAMES_SET.contains(directiveName)
            || FED_FIELD_DIRECTIVE_NAMES_SET.contains(directiveName);
    }

    public static boolean isBaseType(TypeDefinition entityDefinition) {
        return !(entityDefinition instanceof ObjectTypeExtensionDefinition || entityDefinition instanceof InterfaceTypeExtensionDefinition) &&
                entityDefinition.getDirectives().stream()
                .map(directive -> directive.getDefinition().getName())
                .noneMatch(name -> StringUtils.equals(FEDERATION_EXTENDS_DIRECTIVE, name));
    }

}