package com.intuit.graphql.orchestrator.datafetcher;

//public class ServiceDataFetcherTest {
//
//  @Test
//  public void getsDataFromDataLoader() {
//    final ServiceDataFetcher queryFetcher = new ServiceDataFetcher("Query", "topLevelField");
//    final ServiceDataFetcher mutationFetcher = new ServiceDataFetcher("Mutation", "topLevelField");
//
//    final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
//    dataLoaderRegistry
//        .register("Query.topLevelField", new DataLoader<>(__ -> completedFuture(singletonList("qresult"))))
//        .register("Mutation.topLevelField", new DataLoader<>(__ -> completedFuture(singletonList("mresult"))));
//
//    final DataFetchingEnvironment dataFetchingEnvironment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
//        .dataLoaderRegistry(dataLoaderRegistry)
//        .build();
//
//    final Object qresult = queryFetcher.get(dataFetchingEnvironment);
//    final Object mresult = mutationFetcher.get(dataFetchingEnvironment);
//
//    dataLoaderRegistry.dispatchAll();
//
//    assertThat(((CompletableFuture) qresult).getNow("fail")).isEqualTo("qresult");
//    assertThat(((CompletableFuture) mresult).getNow("fail")).isEqualTo("mresult");
//  }
//
//}
//}