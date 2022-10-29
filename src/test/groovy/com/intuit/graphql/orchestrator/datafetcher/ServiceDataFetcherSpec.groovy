
package com.intuit.graphql.orchestrator.datafetcher

//class ServiceDataFetcherTest extends Specification {
//
//  def "gets Data From Data Loader"() {
//    given:
//    final ServiceDataFetcher queryFetcher = new ServiceDataFetcher("Query", "topLevelField")
//    final ServiceDataFetcher mutationFetcher = new ServiceDataFetcher("Mutation", "topLevelField")
//
//    final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry()
//    dataLoaderRegistry
//        .register("Query.topLevelField", new DataLoader<>(__ -> completedFuture(singletonList("qresult"))))
//        .register("Mutation.topLevelField", new DataLoader<>(__ -> completedFuture(singletonList("mresult"))))
//
//    final DataFetchingEnvironment dataFetchingEnvironment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
//        .dataLoaderRegistry(dataLoaderRegistry)
//        .build()
//
//    final Object qresult = queryFetcher.get(dataFetchingEnvironment)
//    final Object mresult = mutationFetcher.get(dataFetchingEnvironment)
//
//    when:
//    dataLoaderRegistry.dispatchAll()
//
//    then:
//    ((CompletableFuture) qresult).getNow("fail") == "qresult"
//    ((CompletableFuture) mresult).getNow("fail") == "mresult"
//  }
//
//}
//}