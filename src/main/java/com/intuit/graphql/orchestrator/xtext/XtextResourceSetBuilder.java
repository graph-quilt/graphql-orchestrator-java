package com.intuit.graphql.orchestrator.xtext;

import static java.util.Objects.requireNonNull;

import com.google.inject.Injector;
import com.intuit.graphql.GraphQLStandaloneSetupGenerated;
import com.intuit.graphql.orchestrator.schema.SchemaParseException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

@Slf4j
/**
 * The Xtext resource set builder.
 */
public class XtextResourceSetBuilder {

  private XtextResourceSet graphqlResourceSet;
  private static Injector GRAPHQL_INJECTOR = new GraphQLStandaloneSetupGenerated().createInjectorAndDoEMFRegistration();
  private Map<String, String> files = new ConcurrentHashMap<>();
  private boolean isFederatedResourceSet = false;

  public static final String FEDERATION_DIRECTIVES = getFederationDirectives();

  private XtextResourceSetBuilder() {
  }

  /**
   * Files xtext resource set builder.
   *
   * @param files the files
   * @return the xtext resource set builder
   */
  public XtextResourceSetBuilder files(Map<String, String> files) {
    this.files.putAll(requireNonNull(files));
    return this;
  }

  /**
   * File xtext resource set builder.
   *
   * @param fileName the file name
   * @param file the file
   * @return the xtext resource set builder
   */
  public XtextResourceSetBuilder file(String fileName, String file) {
    files.put(requireNonNull(fileName), requireNonNull(file));
    return this;
  }

  public XtextResourceSetBuilder isFederatedResourceSet(boolean isFederatedResourceSet) {
    this.isFederatedResourceSet = isFederatedResourceSet;
    return this;
  }

  /**
   * Build xtext resource set.
   *
   * @return the xtext resource set
   */
  public XtextResourceSet build() {
    graphqlResourceSet = GRAPHQL_INJECTOR.getInstance(XtextResourceSet.class);

    if(isFederatedResourceSet) {
      String content = FEDERATION_DIRECTIVES + "\n" + StringUtils.join(files.values(), "\n");

      try {
        createGraphqlResourceFromString(content, "appended_federation");
      } catch (IOException e) {
        throw new SchemaParseException("Unable to parse file: appended federation file", e);
      }
    } else {
      files.forEach((fileName, content) -> {
        try {
          createGraphqlResourceFromString(content, fileName);
        } catch (IOException e) {
          throw new SchemaParseException("Unable to parse file:" + fileName, e);
        }
      });
    }

    List<Issue> issues = validate();
    if (!issues.isEmpty()) {
      throw new SchemaParseException(issues, files.keySet());
    }
    return graphqlResourceSet;
  }

  private XtextResource createResourceFrom(InputStream input, URI uri, Injector injector) throws IOException {
    XtextResource resource = (XtextResource) (injector.getInstance(IResourceFactory.class).createResource(uri));
    resource.load(input, null);
    graphqlResourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
    graphqlResourceSet.getResources().add(resource);
    return resource;
  }

  private XtextResource createGraphqlResourceFromString(String source, String uri) throws IOException {
    return createGraphqlResourceFrom(IOUtils.toInputStream(source), URI.createFileURI(uri));
  }

  private XtextResource createGraphqlResourceFrom(InputStream input, URI uri) throws IOException {
    return createResourceFrom(input, uri, GRAPHQL_INJECTOR);
  }

  private List<Issue> validate() {
    EcoreUtil2.resolveAll(graphqlResourceSet);
    IResourceValidator validator = GRAPHQL_INJECTOR.getInstance(IResourceValidator.class);
    // collect issues
    return graphqlResourceSet.getResources().stream()
        .flatMap(resource -> validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl).stream()
        ).collect(Collectors.toList());

  }

  /**
   * New xtext resource set builder.
   *
   * @return the xtext resource set builder
   */
  public static XtextResourceSetBuilder newBuilder() {
    return new XtextResourceSetBuilder();
  }

  public static XtextResourceSet singletonSet(String fileName, String file) {
    return newBuilder()
        .file(fileName, file)
        .build();
  }

  private static String getFederationDirectives() {
    String directives = "";
    try {
      directives = IOUtils.resourceToString("federation_built_in_directives.graphqls", null, ClassLoader.getSystemClassLoader());
    } catch (IOException ex) {
      log.error("Failed to read resource");
    }

    return directives;
  }
}
