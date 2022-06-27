package com.intuit.graphql.orchestrator.integration

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.*
import graphql.ExecutionInput
import helpers.BaseIntegrationTestSpecification

class MutationSpec extends BaseIntegrationTestSpecification {

    private BooksService bookService = new BooksService(GraphQLOrchestratorSpec.BOOK_ASSERTS)

    private UserRestService userService = new UserRestService(GraphQLOrchestratorSpec.USER_ASSERTS)

    void testMutationOnTopLevelCombinedSchema() {
        given:
        ServiceProvider[] services = [ bookService, new MutablePetsService(), userService ]
        final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(services)

        assert orchestrator.getSchema().isSupportingMutations()

        Map<String, Object> newpetMap = new HashMap<>()
        newpetMap.put("id", "pet-9")
        newpetMap.put("name", "Krypto")
        newpetMap.put("age", "1")
        newpetMap.put("weight", 5)
        newpetMap.put("purebred", Boolean.FALSE)
        newpetMap.put("tag", "DOG")

        Map<String, Object> newUserMap = new HashMap<>()
        newUserMap.put("id", "user-9")
        newUserMap.put("username", "naomi")
        newUserMap.put("password", "iamnaomi")
        newUserMap.put("firstName", "Naomi")
        newUserMap.put("lastName", "Connelly")

        when:
        // Add Pet
        ExecutionInput addPetEI = ExecutionInput
                .newExecutionInput()
                .query('''
                    mutation AddNewPetAndUser($newpet: InputPet!, $newuser: NewUserInput!) {
                        addPet(pet: $newpet) 
                        @merge (if: true) { 
                            id name 
                        }
                        addUser(newUser: $newuser) {
                            id firstName
                        }
                    }
                ''')
                .variables(ImmutableMap.of("newpet", newpetMap, "newuser", newUserMap))
                .build()

        then:
        Map<String, Object> executionResult = orchestrator.execute(addPetEI).get().toSpecification()
        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.keySet().contains("addPet")
        ((Map<String, Object>) dataValue.get("addPet")).entrySet().stream().filter({ it.key == "id" && it.value == "pet-9"}).findAny().isPresent()
        ((Map<String, Object>) dataValue.get("addPet")).entrySet().stream().filter({ it.key == "name" && it.value == "Krypto"}).findAny().isPresent()

        dataValue.keySet().contains("addUser")
        ((Map<String, Object>) dataValue.get("addUser")).entrySet().stream().filter({ it.key == "id" && it.value == "user-9"}).findAny().isPresent()
        ((Map<String, Object>) dataValue.get("addUser")).entrySet().stream().filter({ it.key == "firstName" && it.value == "Naomi"}).findAny().isPresent()
    }

}
