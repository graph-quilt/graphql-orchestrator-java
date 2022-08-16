package com.intuit.graphql.orchestrator.integration.federation

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.testhelpers.MockServiceProvider
import com.intuit.graphql.orchestrator.testhelpers.ServiceProviderMockResponse
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class EntityTypeSpec extends BaseIntegrationTestSpecification {
    ServiceProvider employeeProvider, inventoryProvider, reviewsProvider
    ServiceProvider vehicleProvider, carProvider, reviewProvider
    ExecutionInput vehicleEI, carEI, reviewEI

    String VEHICLE_SCHEMA = """
        type Query {
            getVehicleById(id: String): Vehicle
        }
        type Vehicle @key(fields: "id") {
            id: String
            color: String
            type: String
        }
    """

    String CAR_SCHEMA = """
        type Query {
            getCarInfo(carId: String): CarInfo
        }
        
        type CarInfo {
            carId: String
            numOfWheels: Int
            createdAt: String
            capacity: Int
        }
       
        type Vehicle @key(fields: "id") @extends {
            id: String @external
            model: String
            carInfo: CarInfo
        } 
    """

    String CAR_RESOLVER_FILE = """
        extend type CarInfo {
            reviews: Review @resolver(field: "getReviewsById" arguments: [{name : "id", value: "\$carId"}])
        }
        
        type Review {}
        
        # ================================
        # define this as built-in directive
        directive @resolver(field: String!, arguments: [ResolverArgument!]) on FIELD_DEFINITION
        
        # define this as built-in type
        input ResolverArgument {
            name : String!
            value : String!
        }
    """

    String REVIEW_SCHEMA = """
        type Query {
            getReviewsById(id: String): Review
        }
        
        type Review {
            rating: Int
            comment: String
            submittedBy: String
        }
    """

    String VEHICLE_DOWNSTREAM_QUERY = "query mix_type_providers {getVehicleById(id:\"mockVehicle\") {id color}}"
    String CAR_DOWNSTREAM_QUERY = "query (\$REPRESENTATIONS:[_Any!]!) {_entities(representations:\$REPRESENTATIONS) {... on Vehicle {carInfo {carId capacity reviews {rating comment} __typename}}}}"
    String REVIEWS_DOWNSTREAM_QUERY = "query mix_type_providers_Resolver_Directive_Query {getReviewsById_0:getReviewsById(id:\"test-car\") {rating comment}}"

    @Subject
    def specUnderTest

    def setup() {
        employeeProvider = TestServiceProvider.newBuilder()
                .namespace("Employee")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(TestHelper.getFileMapFromList("top_level/federation/employee.graphqls"))
                .build()

        inventoryProvider = TestServiceProvider.newBuilder()
                .namespace("Inventory")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(TestHelper.getFileMapFromList("top_level/federation/inventory.graphqls"))
                .build()

        reviewsProvider = TestServiceProvider.newBuilder()
                .namespace("Review")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(TestHelper.getFileMapFromList("top_level/federation/review.graphqls"))
                .build()
    }

    def "Federation employee subgraph is stitched"() {
        given:

        when:
        specUnderTest = createGraphQLOrchestrator(employeeProvider)

        then:
        final GraphQLSchema graphQLSchema = specUnderTest?.runtimeGraph?.getExecutableSchema();
        final GraphQLObjectType queryType = specUnderTest?.runtimeGraph?.getOperation(Operation.QUERY);

        graphQLSchema?.getQueryType()?.getFieldDefinitions()?.size() == 2
        queryType?.getFieldDefinition("employeeById") != null
    }

    def "Federation employee and inventory is stitched"() {
        when:
        specUnderTest = createGraphQLOrchestrator([employeeProvider, inventoryProvider])

        then:
        final GraphQLSchema graphQLSchema = specUnderTest?.runtimeGraph?.getExecutableSchema()
        final GraphQLObjectType queryType = specUnderTest?.runtimeGraph?.getOperation(Operation.QUERY)

        graphQLSchema?.getQueryType()?.getFieldDefinitions()?.size() == 4
        queryType.getFieldDefinition("employeeById") != null
        queryType.getFieldDefinition("getSoldProducts") != null
        queryType.getFieldDefinition("getStoreByIdAndName") != null
    }

    def "Federation entities can be extended with extends directive and extend keyword"() {
        when:
        specUnderTest = createGraphQLOrchestrator([employeeProvider, inventoryProvider, reviewsProvider])

        then:
        final GraphQLSchema graphQLSchema = specUnderTest?.runtimeGraph?.getExecutableSchema()
        graphQLSchema.getType("Store")?.getFieldDefinition("review") != null
        graphQLSchema.getType("Employee")?.getFieldDefinition("review") != null
    }

    def "Federation multiple providers can extend entities"() {
        when:
        specUnderTest = createGraphQLOrchestrator([employeeProvider, inventoryProvider, reviewsProvider])

        then:
        final GraphQLSchema graphQLSchema = specUnderTest?.runtimeGraph?.getExecutableSchema()

        graphQLSchema.getType("Employee")?.getFieldDefinition("id") != null
        graphQLSchema.getType("Employee")?.getFieldDefinition("username") != null
        graphQLSchema.getType("Employee")?.getFieldDefinition("password") != null
        graphQLSchema.getType("Employee")?.getFieldDefinition("review") != null
        graphQLSchema.getType("Employee")?.getFieldDefinition("favoriteItem") != null
    }

    def "Federation and resolver providers can stitch together"(){
        given:
        vehicleProvider = TestServiceProvider.newBuilder()
                .namespace("Vehicle")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(ImmutableMap.of("vehicle.graphqls", VEHICLE_SCHEMA))
                .build()

        reviewProvider = TestServiceProvider.newBuilder()
                .namespace("Review")
                .serviceType(ServiceProvider.ServiceType.GRAPHQL)
                .sdlFiles(ImmutableMap.of("review.graphlqs", REVIEW_SCHEMA))
                .build()

        carProvider = TestServiceProvider.newBuilder()
                .namespace("Car")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(ImmutableMap.of("car.graphlqs", CAR_SCHEMA, "resolver.graphqls", CAR_RESOLVER_FILE))
                .build()

        when:
        specUnderTest = createGraphQLOrchestrator([reviewProvider, carProvider, vehicleProvider])

        then:
        final GraphQLSchema graphQLSchema = specUnderTest?.runtimeGraph?.getExecutableSchema()

        GraphQLFieldDefinition vehicleQuery = graphQLSchema.getQueryType().getFieldDefinition("getVehicleById")
        GraphQLFieldDefinition carQuery = graphQLSchema.getQueryType().getFieldDefinition("getCarInfo")
        GraphQLFieldDefinition reviewsQuery = graphQLSchema.getQueryType().getFieldDefinition("getReviewsById")

        vehicleQuery != null
        carQuery != null
        reviewsQuery != null

        GraphQLObjectType vehicleType = vehicleQuery.originalType
        GraphQLObjectType carQueryType = carQuery.originalType
        GraphQLObjectType reviewQueryType = reviewsQuery.originalType

        vehicleType?.getFieldDefinition("id") != null
        vehicleType?.getFieldDefinition("color") != null
        vehicleType?.getFieldDefinition("type") != null
        vehicleType?.getFieldDefinition("model") != null
        vehicleType?.getFieldDefinition("carInfo") != null

        carQueryType?.getFieldDefinition("carId") != null
        carQueryType?.getFieldDefinition("numOfWheels") != null
        carQueryType?.getFieldDefinition("createdAt") != null
        carQueryType?.getFieldDefinition("capacity") != null
        carQueryType?.getFieldDefinition("reviews") != null

        reviewQueryType?.getFieldDefinition("rating") != null
        reviewQueryType?.getFieldDefinition("comment") != null
        reviewQueryType?.getFieldDefinition("submittedBy") != null
    }


    def "Federation and resolver providers can be executed together"(){
        given:
        vehicleEI = ExecutionInput.newExecutionInput().query(VEHICLE_DOWNSTREAM_QUERY).build()
        carEI = ExecutionInput.newExecutionInput().query(CAR_DOWNSTREAM_QUERY).build()
        reviewEI =  ExecutionInput.newExecutionInput().query(REVIEWS_DOWNSTREAM_QUERY).build()

        def vehicleResponse = """
            {
              "data": {
                "getVehicleById": {
                  "id": "test-car",
                  "color": "Purple",
                  "type": "Car"
                }
              }
            }
        """

        vehicleProvider = MockServiceProvider.builder()
                .namespace("Vehicle")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(ImmutableMap.of("vehicle.graphqls", VEHICLE_SCHEMA))
                .mockResponse(
                    ServiceProviderMockResponse.builder()
                    .forExecutionInput(vehicleEI)
                    .expectResponseRaw(vehicleResponse)
                    .build()
                )
                .build()

        def reviewResponse = """
            {
              "data": {
                "getReviewsById_0": {
                  "typename": "Review",
                  "rating": 5,
                  "comment": "test review"
                }
              }
            }
        """

        reviewProvider = MockServiceProvider.builder()
                .namespace("Review")
                .serviceType(ServiceProvider.ServiceType.GRAPHQL)
                .sdlFiles(ImmutableMap.of("review.graphlqs", REVIEW_SCHEMA))
                .mockResponse(
                    ServiceProviderMockResponse.builder()
                    .forExecutionInput(reviewEI)
                    .expectResponseRaw(reviewResponse)
                    .build()
                )
                .build()


        def carResponse = """
            {
                "data": {
                    "_entities": [
                        {
                            "_typename": "Vehicle",
                            "carInfo": {
                                "carId": "test-car",
                                "capacity": 2,
                                "__typename": "Car"
                            }
                        }
                    ]
                }
            }
        """

        carProvider = MockServiceProvider.builder()
                .namespace("Car")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(ImmutableMap.of("car.graphlqs", CAR_SCHEMA, "resolver.graphqls", CAR_RESOLVER_FILE))
                .mockResponse(
                    ServiceProviderMockResponse.builder()
                    .forExecutionInput(carEI)
                    .expectResponseRaw(carResponse)
                    .build()
                )
                .build()

        specUnderTest = createGraphQLOrchestrator([reviewProvider, carProvider, vehicleProvider])

        String queryString = "query mix_type_providers { getVehicleById(id: \"mockVehicle\") { id color carInfo { carId capacity reviews { rating comment} } } }"
        ExecutionInput query = ExecutionInput.newExecutionInput()
                .query(queryString)
                .build();

        when:
        ExecutionResult executionResult = specUnderTest.execute(query).get();

        then:
        executionResult?.errors?.size() == 0
        executionResult?.data?.size() != 0
        LinkedHashMap vehicleById = executionResult?.data.get("getVehicleById")
        vehicleById.get("id") == "test-car"
        vehicleById.get("color") == "Purple"
        LinkedHashMap carEntity = vehicleById.get("carInfo")
        carEntity.get("carId") == "test-car"
        carEntity.get("capacity") == 2
        LinkedHashMap reviewResolver = carEntity.get("reviews")
        reviewResolver.get("rating") == 5
        reviewResolver.get("comment") == "test review"
    }
}