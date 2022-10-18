package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.ServiceProvider.ServiceType.REST;
import static com.intuit.graphql.orchestrator.xtext.DataFetcherContext.STATIC_DATAFETCHER_CONTEXT;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.SchemaTransformationException;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;
import com.intuit.graphql.utils.XtextTypeUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * This class is responsible for checking each graph for any adapter directives. If there are adapter directives on any
 * field, this class will attach a REST data fetcher to that field and then attach a static DF until the root.
 */
public class GraphQLAdapterTransformer implements Transformer<UnifiedXtextGraph, UnifiedXtextGraph> {

  public static final String FIELD_NULL_ERROR = "Field %s cannot be found for adapter transformation";

  @Override
  public UnifiedXtextGraph transform(final UnifiedXtextGraph source) {
    Map<FieldContext, DataFetcherContext> adapterCodeRegistry = new HashMap<>();
    source.getCodeRegistry().forEach((fieldContext, dataFetcherContext) -> {
      if (dataFetcherContext.getServiceType() == REST) {
        Operation operation = source.getOperation(fieldContext.getParentType());
        ObjectTypeDefinition parent = null;
        if (operation != null) {
          //Only do it for Query and Mutation
          if (operation == Operation.QUERY || operation == Operation.MUTATION) {
            parent = source.getOperationType(operation);
          }
        } else {
          parent = (ObjectTypeDefinition) source
              .getTypes().get(fieldContext.getParentType());
        }
        if (parent != null) {
          FieldDefinition field = parent.getFieldDefinition().stream()
              .filter(f -> StringUtils.equals(f.getName(), fieldContext.getFieldName()))
              .findFirst()
              .orElseThrow(() -> new SchemaTransformationException(
                  String.format(FIELD_NULL_ERROR, fieldContext.getFieldName())));
          // Move Rest DF down if the directive is present.
          adapterCodeRegistry.putAll(new AdapterDirectiveVisitor(dataFetcherContext)
              .adapterCodeRegistry(parent, field, field.getNamedType())
          );
        }
      }
    });
    source.getCodeRegistry().putAll(adapterCodeRegistry);
    return source;
  }

  static class AdapterDirectiveVisitor {

    public static final String ADAPTER_DIRECTIVE_NAME = "adapter";
    public static final String ADAPTER_DIRECTIVE_ARGUMENT = "service";
    public static final String ERROR_MSG = "Adapter directive should have a `service` argument";

    private final DataFetcherContext dataFetcherContext;

    AdapterDirectiveVisitor(DataFetcherContext dataFetcherContext) {
      this.dataFetcherContext = dataFetcherContext;
    }

    private boolean hasAdapterDirective(FieldDefinition fieldDefinition) {
      for (Directive directive : fieldDefinition.getDirectives()) {
        if (StringUtils.equals(directive.getDefinition().getName(), (ADAPTER_DIRECTIVE_NAME))) {
          if (directive.getArguments().stream()
              .noneMatch(argument -> StringUtils.equals(argument.getName(), ADAPTER_DIRECTIVE_ARGUMENT))) {
            throw new SchemaTransformationException(ERROR_MSG);
          }
          return true;
        }
      }
      return false;
    }

    /***
     * This function is a core piece of the transformation. It starts at root and tries to
     * find the adapter directive. Once it finds the directive, it sets a REST data-fetcher
     * at the field level & back tracks upto the root by setting everything to
     * static datafetcher along the way.
     *
     * Ex
     *   Query { consumer { finance { category @adapter {...} }}}
     * Output
     *   Query.consumer & consumer.finance -> Static df
     *   finance.category -> Rest df
     *
     * @param parent
     * @param parentField
     * @param namedType
     * @return
     */
    public Map<FieldContext, DataFetcherContext> adapterCodeRegistry(ObjectTypeDefinition parent,
        FieldDefinition parentField,
        NamedType namedType) {

      if (!XtextUtils.isObjectType(namedType) || hasAdapterDirective(parentField)) {
        return Collections.emptyMap();
      }

      final Map<FieldContext, DataFetcherContext> codeRegistry = new HashMap<>();
      ObjectTypeDefinition object = (ObjectTypeDefinition) XtextTypeUtils.getObjectType(namedType);
      for (FieldDefinition field : object.getFieldDefinition()) {
        // Found adapter
        if (hasAdapterDirective(field)) {
          codeRegistry.put(new FieldContext(object.getName(), field.getName()), dataFetcherContext);
        } else {
          codeRegistry.putAll(adapterCodeRegistry(object, field, field.getNamedType()));
        }
      }
      // Make sure to set the datafetchers above to static DF.
      if (!codeRegistry.isEmpty() && parent != null && parentField != null) {
        FieldContext fieldContext = new FieldContext(parent.getName(), parentField.getName());
        codeRegistry.put(fieldContext, STATIC_DATAFETCHER_CONTEXT);
      }
      return codeRegistry;
    }
  }
}
