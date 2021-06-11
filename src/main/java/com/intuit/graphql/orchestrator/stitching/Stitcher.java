package com.intuit.graphql.orchestrator.stitching;

import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import java.util.List;

/**
 * The interface Stitcher.
 */
public interface Stitcher {

  /**
   * Stitch the service context into a runtime graph.
   *
   * @param serviceProviders the service contexts
   * @return the runtime graph
   */
  RuntimeGraph stitch(List<ServiceProvider> serviceProviders);

}
