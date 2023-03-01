package com.intuit.graphql.orchestrator.visitors.queryVisitors;

import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.Definition;
import graphql.language.Directive;
import graphql.language.DirectiveDefinition;
import graphql.language.DirectiveLocation;
import graphql.language.Document;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValue;
import graphql.language.EnumValueDefinition;
import graphql.language.Field;
import graphql.language.FieldDefinition;
import graphql.language.FloatValue;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.IntValue;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.ListType;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.NonNullType;
import graphql.language.NullValue;
import graphql.language.ObjectField;
import graphql.language.ObjectTypeDefinition;
import graphql.language.ObjectValue;
import graphql.language.OperationDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.language.Type;
import graphql.language.TypeDefinition;
import graphql.language.TypeName;
import graphql.language.UnionTypeDefinition;
import graphql.language.Value;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.HashMap;
import java.util.Map;

public class QueryVisitorStub extends NodeVisitorStub {

    public TraversalControl visitArgument(Argument node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitArrayValue(ArrayValue node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitBooleanValue(BooleanValue node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitDirective(Directive node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitDirectiveDefinition(DirectiveDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitDirectiveLocation(DirectiveLocation node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitDocument(Document node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitEnumTypeDefinition(EnumTypeDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitEnumValue(EnumValue node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitEnumValueDefinition(EnumValueDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitField(Field node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitFieldDefinition(FieldDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitFloatValue(FloatValue node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitFragmentDefinition(FragmentDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitFragmentSpread(FragmentSpread node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitInputObjectTypeDefinition(InputObjectTypeDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitInputValueDefinition(InputValueDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitIntValue(IntValue node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitInterfaceTypeDefinition(InterfaceTypeDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitListType(ListType node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitNonNullType(NonNullType node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitNullValue(NullValue node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitObjectField(ObjectField node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitObjectTypeDefinition(ObjectTypeDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitObjectValue(ObjectValue node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitOperationDefinition(OperationDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitOperationTypeDefinition(OperationTypeDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitScalarTypeDefinition(ScalarTypeDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitSchemaDefinition(SchemaDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitSelectionSet(SelectionSet node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitStringValue(StringValue node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitTypeName(TypeName node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitUnionTypeDefinition(UnionTypeDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitVariableDefinition(VariableDefinition node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public TraversalControl visitVariableReference(VariableReference node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    protected TraversalControl visitValue(Value<?> node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    protected TraversalControl visitDefinition(Definition<?> node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    protected TraversalControl visitTypeDefinition(TypeDefinition<?> node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    protected TraversalControl visitSelection(Selection<?> node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    protected TraversalControl visitType(Type<?> node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    protected TraversalControl visitNode(Node node, TraverserContext<Node> context) {
        return TraversalControl.CONTINUE;
    }

    public Map<String, Object> getSharedTraversalContext() {
        return new HashMap<>();
    }

    public Map<String, Object> getResults() {
        return new HashMap<>();
    }
}
