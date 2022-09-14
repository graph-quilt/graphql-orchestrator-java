package com.intuit.graphql.orchestrator.schema

import graphql.ErrorType
import spock.lang.Specification

class RawGraphQLErrorSpec extends Specification {

    Map<String, Number> location
    Map<String, Object> extensions
    Map<String, Object> rawGraphQLError

    List<Object> path
    List<Map> locations

    def setup() {
        this.rawGraphQLError = new HashMap<>()
        this.extensions = new HashMap<>()
        this.locations = new ArrayList<>()
        this.location = new HashMap<>()
        this.path = new ArrayList<>()
    }

    def "should extract everything"() {
        given:
        location.put("line", 2)
        location.put("column", 3L)
        locations.add(location)

        path.addAll(Arrays.asList("some", "path", 1, 2L, "field"))

        extensions.put("detailed", "message")

        rawGraphQLError.put("message", "boom")
        rawGraphQLError.put("locations", locations)
        rawGraphQLError.put("path", path)
        rawGraphQLError.put("extensions", extensions)

        def final rawGraphQLError = new RawGraphQLError(this.rawGraphQLError)

        expect:
        rawGraphQLError.getMessage() == "boom"
        rawGraphQLError.getLocations().size() == 1

        def firstLocation = rawGraphQLError.getLocations().get(0)
        firstLocation.getLine() == 2
        firstLocation.getColumn() == 3

        def path = rawGraphQLError.getPath()
        path.size() == 5
        path.containsAll("some", "path", 1, 2L, "field")

        rawGraphQLError.getExtensions().get("detailed") == "message"
        rawGraphQLError.getErrorType() == ErrorType.DataFetchingException
    }

    def "should Provide Default Values For Missing Error Fields"() {
        given:
        final RawGraphQLError rawGraphQLError = new RawGraphQLError(this.rawGraphQLError)

        expect:
        rawGraphQLError.getMessage() == "Unknown error"
        rawGraphQLError.getErrorType() == ErrorType.DataFetchingException
        rawGraphQLError.getLocations() == null
        rawGraphQLError.getPath() == null
        rawGraphQLError.getExtensions() == null
    }

}
