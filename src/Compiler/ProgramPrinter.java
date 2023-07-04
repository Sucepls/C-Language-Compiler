package Compiler;

import gen.CListener;
import gen.CParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

public class ProgramPrinter implements CListener {

    private int indent_level;
    private final ArrayList<Scope> scopes;
    private int nested_layer;

    public ProgramPrinter() {
        this.scopes = new ArrayList<>();
        this.indent_level = 0;
        this.nested_layer = 0;
    }

    public ArrayList<Scope> getScopes() {
        return scopes;
    }

    private void indentation() {
        for (int i = 0; i < indent_level; i++) {
            System.out.print("    ");
        }
    }

    private void startNestedStatement() {
        indentation();
        indent_level++;
        System.out.println("nested statement: {");
    }

    private void stopNestedStatement() {
        indent_level--;
        indentation();
        System.out.println("}");
    }

    //must be completed
    private Scope getParentScope(ParserRuleContext ctx) {
        Scope parent = null;
        return parent;
    }

    String getParamType(CParser.ParameterDeclarationContext ctx){

        String par_type = ctx.declarationSpecifiers().getText();
        var paramLength = ctx.declarator().directDeclarator().Constant().size();

        if(paramLength > 0) {
            var length = ctx.declarator().directDeclarator().Constant().get(0).getText();
            return String.format("array_%s, length= %s" , par_type , length);
        }
        else {
            return String.format("%s" , par_type);
        }
    }

    @Override
    public void enterExternalDeclaration(CParser.ExternalDeclarationContext ctx) {
        System.out.println("program start {");
        indent_level++;

        Scope scope = new Scope("program", ctx.start.getLine(), Type.PROGRAM, ctx.getRuleIndex());
        scope.setParent(null);
        scopes.add(scope);
    }

    @Override
    public void exitExternalDeclaration(CParser.ExternalDeclarationContext ctx) {
        System.out.println("}");
    }

    @Override
    public void enterPostfixExpression(CParser.PostfixExpressionContext ctx) {
        if (ctx.LeftParen().size() != 0 && ctx.RightParen().size() != 0 && !ctx.primaryExpression().isEmpty() && !ctx.getText().contains("printf")) {
            indentation();
            System.out.print("function call: name: " + ctx.primaryExpression().getText());
            //check arguments
            if (!ctx.argumentExpressionList().isEmpty()) {
                System.out.print("/ params: ");
                List<CParser.AssignmentExpressionContext> arguments = ctx.argumentExpressionList().get(0).assignmentExpression();
                for (int i = 0; i < arguments.size(); i++) {
                    String parameter = arguments.get(i).getText();
                    System.out.print(parameter + "(index: " + i + ") ");
                }
            }
        }
    }

    @Override
    public void exitPostfixExpression(CParser.PostfixExpressionContext ctx) {
        if (ctx.LeftParen().size() != 0 && ctx.RightParen().size() != 0 && !ctx.primaryExpression().isEmpty()) {
            System.out.println();
        }
    }

    @Override
    public void enterDeclaration(CParser.DeclarationContext ctx) {
        if (!ctx.initDeclaratorList().isEmpty()) {
            for (int i = 0; i < ctx.initDeclaratorList().initDeclarator().size(); i++) {
                indentation();
                String name = ctx.initDeclaratorList().initDeclarator(i).declarator().directDeclarator().Identifier().getText();
                String type = ctx.declarationSpecifiers().getText();
                int length = Integer.parseInt(ctx.initDeclaratorList().initDeclarator(i).declarator().directDeclarator().Constant().get(0).toString());
                System.out.print("field: " + name + "/ type: " + type);
                if (length > 0) {
                    System.out.println("/ length: " + length);
                } else {
                    System.out.println();
                }
            }
        }
    }

    @Override
    public void enterParameterTypeList(CParser.ParameterTypeListContext ctx) {
        indentation();
        System.out.print("parameters list: (");
    }

    @Override
    public void exitParameterTypeList(CParser.ParameterTypeListContext ctx) {
        System.out.println(")");
    }

    @Override
    public void enterParameterList(CParser.ParameterListContext ctx) {
        for (int i = 0; i < ctx.parameterDeclaration().size(); i++) {
            String type = ctx.parameterDeclaration(i).getChild(0).getText();
            String name = ctx.parameterDeclaration(i).getChild(1).getText();
            if (name.contains("[")) {
                String[] str = ctx.parameterDeclaration(i).getChild(1).getText().split("\\[");
                name = str[0];
            }
            System.out.print(type + " " + name);
            if (i != ctx.parameterDeclaration().size() - 1) {
                System.out.print(", ");
            }
        }
    }

    @Override
    public void enterFunctionDefinition(CParser.FunctionDefinitionContext ctx) {
        indentation();
        String func_name = ctx.declarator().directDeclarator().directDeclarator().getText();
        Scope scope = new Scope(func_name, ctx.start.getLine(), Type.FUNCTION, ctx.getRuleIndex());
        scopes.add(scope);
        var param = ctx.declarator().directDeclarator().parameterTypeList().parameterList().parameterDeclaration();
        String name = null;
        String type = null;
        if (param != null){
            for (int i=0; i<param.size(); i++){
                name = param.get(i).declarator().directDeclarator().Identifier().getText();
                type = getParamType(param.get(i));
                String value = String.format("methodParamField(name: %s) (type: %s)", name, type);
                scope.insert("Field_" + name, new SymbolTableItem(ctx.start.getLine(), value));
            }
        }
        System.out.println("normal method: name: " + func_name + "/ return type: " + ctx.getChild(0).getText() + " {");
        indent_level++;

    }

    @Override
    public void exitFunctionDefinition(CParser.FunctionDefinitionContext ctx) {
        indent_level--;
        indentation();
        System.out.println("}");
    }

    @Override
    public void enterMainfunctionDefinition(CParser.MainfunctionDefinitionContext ctx) {
        indentation();
        indent_level++;
        Scope scope = new Scope("main", ctx.start.getLine(), Type.MAIN, ctx.getRuleIndex());
        scopes.add(scope);
        var param = ctx.compoundStatement().blockItemList().blockItem();
        String name = null;
        String type = null;
        int length = 0;
        if (param != null){
            for (int i=0; i<param.size(); i++){
                if (ctx.compoundStatement().blockItemList().blockItem(i).declaration() != null){
                    type = ctx.compoundStatement().blockItemList().blockItem(i).declaration().declarationSpecifiers().declarationSpecifier(i).typeSpecifier().getText();
                    if (ctx.compoundStatement().blockItemList().blockItem(i).declaration().initDeclaratorList() != null){
                        var paramdec =ctx.compoundStatement().blockItemList().blockItem(i).declaration().initDeclaratorList().initDeclarator();
                        name = ctx.compoundStatement().blockItemList().blockItem(i).declaration().initDeclaratorList().initDeclarator().get(0).declarator().directDeclarator().Identifier().getText();
                    }
                    if (ctx.compoundStatement().blockItemList().blockItem(i).declaration().initDeclaratorList().initDeclarator(0).declarator().directDeclarator().LeftBracket().size() > 0) {
                        length = Integer.parseInt(ctx.compoundStatement().blockItemList().blockItem(i).declaration().initDeclaratorList().initDeclarator(0).declarator().directDeclarator().Constant().get(0).getText());
                    }
                    String key = "Field_" + name;
                    String value = null;
                    if (length == 0)
                        value = String.format("methodField(name: %s) (type: %s)", name, type);
                    else
                        value = String.format("methodField(name: %s) (type: %s array, length= %d)", name, type, length);
                    scope.insert(key, new SymbolTableItem(ctx.compoundStatement().blockItemList().blockItem(i).start.getLine(), value));
                    length = 0;

                }
            }
        }
        System.out.print("main method: return type: " + ctx.getStart().getText());
        if (ctx.getStart().getText().equals("void")) {
            System.out.print("(no return)");
        }
        System.out.println(" {");
    }

    @Override
    public void exitMainfunctionDefinition(CParser.MainfunctionDefinitionContext ctx) {
        indent_level--;
        indentation();
        System.out.println("}");
    }

    @Override
    public void enterSelectionStatement(CParser.SelectionStatementContext ctx) {
        Scope scope = new Scope("Field", ctx.start.getLine(), Type.NESTED, ctx.getRuleIndex());
        Scope parent = getParentScope(ctx);
        scope.setParent(parent);
        //error
//        if (scopes.contains(scope)) {
//            System.out.println("Error104 : in line " + scope.getScope_number() + ":" + scope.getIndex() + ", field " + scope.getName() + " has been defined already");
//        }
        nested_layer++;
        if (nested_layer == 1) {
            startNestedStatement();
        }
    }

    @Override
    public void exitSelectionStatement(CParser.SelectionStatementContext ctx) {
        nested_layer--;
        if (nested_layer == 1) {
            stopNestedStatement();
        }
    }

    @Override
    public void enterIterationStatement(CParser.IterationStatementContext ctx) {
        nested_layer++;
        if (nested_layer == 1) {
            startNestedStatement();
        }
    }

    @Override
    public void exitIterationStatement(CParser.IterationStatementContext ctx) {
        nested_layer--;
        if (nested_layer == 1) {
            stopNestedStatement();
        }
    }

    @Override
    public void enterPrimaryExpression(CParser.PrimaryExpressionContext ctx) {

    }

    @Override
    public void exitPrimaryExpression(CParser.PrimaryExpressionContext ctx) {

    }

    @Override
    public void enterArgumentExpressionList(CParser.ArgumentExpressionListContext ctx) {

    }

    @Override
    public void exitArgumentExpressionList(CParser.ArgumentExpressionListContext ctx) {

    }

    @Override
    public void enterUnaryExpression(CParser.UnaryExpressionContext ctx) {

    }

    @Override
    public void exitUnaryExpression(CParser.UnaryExpressionContext ctx) {

    }

    @Override
    public void enterUnaryOperator(CParser.UnaryOperatorContext ctx) {

    }

    @Override
    public void exitUnaryOperator(CParser.UnaryOperatorContext ctx) {

    }

    @Override
    public void enterCastExpression(CParser.CastExpressionContext ctx) {

    }

    @Override
    public void exitCastExpression(CParser.CastExpressionContext ctx) {

    }

    @Override
    public void enterMultiplicativeExpression(CParser.MultiplicativeExpressionContext ctx) {

    }

    @Override
    public void exitMultiplicativeExpression(CParser.MultiplicativeExpressionContext ctx) {

    }

    @Override
    public void enterAdditiveExpression(CParser.AdditiveExpressionContext ctx) {

    }

    @Override
    public void exitAdditiveExpression(CParser.AdditiveExpressionContext ctx) {

    }

    @Override
    public void enterShiftExpression(CParser.ShiftExpressionContext ctx) {

    }

    @Override
    public void exitShiftExpression(CParser.ShiftExpressionContext ctx) {

    }

    @Override
    public void enterRelationalExpression(CParser.RelationalExpressionContext ctx) {

    }

    @Override
    public void exitRelationalExpression(CParser.RelationalExpressionContext ctx) {

    }

    @Override
    public void enterEqualityExpression(CParser.EqualityExpressionContext ctx) {

    }

    @Override
    public void exitEqualityExpression(CParser.EqualityExpressionContext ctx) {

    }

    @Override
    public void enterAndExpression(CParser.AndExpressionContext ctx) {

    }

    @Override
    public void exitAndExpression(CParser.AndExpressionContext ctx) {

    }

    @Override
    public void enterExclusiveOrExpression(CParser.ExclusiveOrExpressionContext ctx) {

    }

    @Override
    public void exitExclusiveOrExpression(CParser.ExclusiveOrExpressionContext ctx) {

    }

    @Override
    public void enterInclusiveOrExpression(CParser.InclusiveOrExpressionContext ctx) {

    }

    @Override
    public void exitInclusiveOrExpression(CParser.InclusiveOrExpressionContext ctx) {

    }

    @Override
    public void enterLogicalAndExpression(CParser.LogicalAndExpressionContext ctx) {

    }

    @Override
    public void exitLogicalAndExpression(CParser.LogicalAndExpressionContext ctx) {

    }

    @Override
    public void enterLogicalOrExpression(CParser.LogicalOrExpressionContext ctx) {

    }

    @Override
    public void exitLogicalOrExpression(CParser.LogicalOrExpressionContext ctx) {

    }

    @Override
    public void enterConditionalExpression(CParser.ConditionalExpressionContext ctx) {

    }

    @Override
    public void exitConditionalExpression(CParser.ConditionalExpressionContext ctx) {

    }

    @Override
    public void enterAssignmentExpression(CParser.AssignmentExpressionContext ctx) {

    }

    @Override
    public void exitAssignmentExpression(CParser.AssignmentExpressionContext ctx) {

    }

    @Override
    public void enterAssignmentOperator(CParser.AssignmentOperatorContext ctx) {

    }

    @Override
    public void exitAssignmentOperator(CParser.AssignmentOperatorContext ctx) {

    }

    @Override
    public void enterExpression(CParser.ExpressionContext ctx) {

    }

    @Override
    public void exitExpression(CParser.ExpressionContext ctx) {

    }

    @Override
    public void enterConstantExpression(CParser.ConstantExpressionContext ctx) {

    }

    @Override
    public void exitConstantExpression(CParser.ConstantExpressionContext ctx) {

    }

    @Override
    public void exitDeclaration(CParser.DeclarationContext ctx) {

    }

    @Override
    public void enterDeclarationSpecifiers(CParser.DeclarationSpecifiersContext ctx) {

    }

    @Override
    public void exitDeclarationSpecifiers(CParser.DeclarationSpecifiersContext ctx) {
    }

    @Override
    public void enterDeclarationSpecifiers2(CParser.DeclarationSpecifiers2Context ctx) {

    }

    @Override
    public void exitDeclarationSpecifiers2(CParser.DeclarationSpecifiers2Context ctx) {

    }

    @Override
    public void enterDeclarationSpecifier(CParser.DeclarationSpecifierContext ctx) {

    }

    @Override
    public void exitDeclarationSpecifier(CParser.DeclarationSpecifierContext ctx) {

    }

    @Override
    public void enterInitDeclaratorList(CParser.InitDeclaratorListContext ctx) {

    }

    @Override
    public void exitInitDeclaratorList(CParser.InitDeclaratorListContext ctx) {

    }

    @Override
    public void enterInitDeclarator(CParser.InitDeclaratorContext ctx) {
    }

    @Override
    public void exitInitDeclarator(CParser.InitDeclaratorContext ctx) {

    }

    @Override
    public void enterStorageClassSpecifier(CParser.StorageClassSpecifierContext ctx) {

    }

    @Override
    public void exitStorageClassSpecifier(CParser.StorageClassSpecifierContext ctx) {

    }

    @Override
    public void enterTypeSpecifier(CParser.TypeSpecifierContext ctx) {

    }

    @Override
    public void exitTypeSpecifier(CParser.TypeSpecifierContext ctx) {

    }

    @Override
    public void enterStructOrUnionSpecifier(CParser.StructOrUnionSpecifierContext ctx) {

    }

    @Override
    public void exitStructOrUnionSpecifier(CParser.StructOrUnionSpecifierContext ctx) {

    }

    @Override
    public void enterStructOrUnion(CParser.StructOrUnionContext ctx) {

    }

    @Override
    public void exitStructOrUnion(CParser.StructOrUnionContext ctx) {

    }

    @Override
    public void enterStructDeclarationList(CParser.StructDeclarationListContext ctx) {

    }

    @Override
    public void exitStructDeclarationList(CParser.StructDeclarationListContext ctx) {

    }

    @Override
    public void enterStructDeclaration(CParser.StructDeclarationContext ctx) {

    }

    @Override
    public void exitStructDeclaration(CParser.StructDeclarationContext ctx) {

    }

    @Override
    public void enterSpecifierQualifierList(CParser.SpecifierQualifierListContext ctx) {

    }

    @Override
    public void exitSpecifierQualifierList(CParser.SpecifierQualifierListContext ctx) {

    }

    @Override
    public void enterStructDeclaratorList(CParser.StructDeclaratorListContext ctx) {

    }

    @Override
    public void exitStructDeclaratorList(CParser.StructDeclaratorListContext ctx) {

    }

    @Override
    public void enterStructDeclarator(CParser.StructDeclaratorContext ctx) {

    }

    @Override
    public void exitStructDeclarator(CParser.StructDeclaratorContext ctx) {

    }

    @Override
    public void enterEnumSpecifier(CParser.EnumSpecifierContext ctx) {

    }

    @Override
    public void exitEnumSpecifier(CParser.EnumSpecifierContext ctx) {

    }

    @Override
    public void enterEnumeratorList(CParser.EnumeratorListContext ctx) {

    }

    @Override
    public void exitEnumeratorList(CParser.EnumeratorListContext ctx) {

    }

    @Override
    public void enterEnumerator(CParser.EnumeratorContext ctx) {

    }

    @Override
    public void exitEnumerator(CParser.EnumeratorContext ctx) {

    }

    @Override
    public void enterEnumerationConstant(CParser.EnumerationConstantContext ctx) {

    }

    @Override
    public void exitEnumerationConstant(CParser.EnumerationConstantContext ctx) {

    }

    @Override
    public void enterTypeQualifier(CParser.TypeQualifierContext ctx) {

    }

    @Override
    public void exitTypeQualifier(CParser.TypeQualifierContext ctx) {

    }

    @Override
    public void enterDeclarator(CParser.DeclaratorContext ctx) {

    }

    @Override
    public void exitDeclarator(CParser.DeclaratorContext ctx) {

    }

    @Override
    public void enterMaindeclarator(CParser.MaindeclaratorContext ctx) {

    }

    @Override
    public void exitMaindeclarator(CParser.MaindeclaratorContext ctx) {

    }

    @Override
    public void enterDirectDeclarator(CParser.DirectDeclaratorContext ctx) {

    }

    @Override
    public void exitDirectDeclarator(CParser.DirectDeclaratorContext ctx) {

    }

    @Override
    public void enterMaindirectDeclarator(CParser.MaindirectDeclaratorContext ctx) {

    }

    @Override
    public void exitMaindirectDeclarator(CParser.MaindirectDeclaratorContext ctx) {

    }

    @Override
    public void enterNestedParenthesesBlock(CParser.NestedParenthesesBlockContext ctx) {

    }

    @Override
    public void exitNestedParenthesesBlock(CParser.NestedParenthesesBlockContext ctx) {

    }

    @Override
    public void enterPointer(CParser.PointerContext ctx) {

    }

    @Override
    public void exitPointer(CParser.PointerContext ctx) {

    }

    @Override
    public void enterTypeQualifierList(CParser.TypeQualifierListContext ctx) {

    }

    @Override
    public void exitTypeQualifierList(CParser.TypeQualifierListContext ctx) {

    }

    @Override
    public void exitParameterList(CParser.ParameterListContext ctx) {

    }

    @Override
    public void enterParameterDeclaration(CParser.ParameterDeclarationContext ctx) {

    }

    @Override
    public void exitParameterDeclaration(CParser.ParameterDeclarationContext ctx) {

    }

    @Override
    public void enterIdentifierList(CParser.IdentifierListContext ctx) {

    }

    @Override
    public void exitIdentifierList(CParser.IdentifierListContext ctx) {

    }

    @Override
    public void enterTypeName(CParser.TypeNameContext ctx) {

    }

    @Override
    public void exitTypeName(CParser.TypeNameContext ctx) {

    }

    @Override
    public void enterTypedefName(CParser.TypedefNameContext ctx) {

    }

    @Override
    public void exitTypedefName(CParser.TypedefNameContext ctx) {

    }

    @Override
    public void enterInitializer(CParser.InitializerContext ctx) {

    }

    @Override
    public void exitInitializer(CParser.InitializerContext ctx) {

    }

    @Override
    public void enterInitializerList(CParser.InitializerListContext ctx) {

    }

    @Override
    public void exitInitializerList(CParser.InitializerListContext ctx) {

    }

    @Override
    public void enterDesignation(CParser.DesignationContext ctx) {

    }

    @Override
    public void exitDesignation(CParser.DesignationContext ctx) {

    }

    @Override
    public void enterDesignatorList(CParser.DesignatorListContext ctx) {

    }

    @Override
    public void exitDesignatorList(CParser.DesignatorListContext ctx) {

    }

    @Override
    public void enterDesignator(CParser.DesignatorContext ctx) {

    }

    @Override
    public void exitDesignator(CParser.DesignatorContext ctx) {

    }

    @Override
    public void enterStatement(CParser.StatementContext ctx) {

    }

    @Override
    public void exitStatement(CParser.StatementContext ctx) {

    }

    @Override
    public void enterLabeledStatement(CParser.LabeledStatementContext ctx) {

    }

    @Override
    public void exitLabeledStatement(CParser.LabeledStatementContext ctx) {

    }

    @Override
    public void enterCompoundStatement(CParser.CompoundStatementContext ctx) {

    }

    @Override
    public void exitCompoundStatement(CParser.CompoundStatementContext ctx) {

    }

    @Override
    public void enterBlockItemList(CParser.BlockItemListContext ctx) {

    }

    @Override
    public void exitBlockItemList(CParser.BlockItemListContext ctx) {

    }

    @Override
    public void enterBlockItem(CParser.BlockItemContext ctx) {

    }

    @Override
    public void exitBlockItem(CParser.BlockItemContext ctx) {

    }

    @Override
    public void enterExpressionStatement(CParser.ExpressionStatementContext ctx) {

    }

    @Override
    public void exitExpressionStatement(CParser.ExpressionStatementContext ctx) {

    }

    @Override
    public void enterForCondition(CParser.ForConditionContext ctx) {

    }

    @Override
    public void exitForCondition(CParser.ForConditionContext ctx) {

    }

    @Override
    public void enterForDeclaration(CParser.ForDeclarationContext ctx) {

    }

    @Override
    public void exitForDeclaration(CParser.ForDeclarationContext ctx) {

    }

    @Override
    public void enterForExpression(CParser.ForExpressionContext ctx) {

    }

    @Override
    public void exitForExpression(CParser.ForExpressionContext ctx) {

    }

    @Override
    public void enterJumpStatement(CParser.JumpStatementContext ctx) {

    }

    @Override
    public void exitJumpStatement(CParser.JumpStatementContext ctx) {

    }


    @Override
    public void enterDeclarationList(CParser.DeclarationListContext ctx) {

    }

    @Override
    public void exitDeclarationList(CParser.DeclarationListContext ctx) {

    }

    @Override
    public void visitTerminal(TerminalNode terminalNode) {

    }

    @Override
    public void visitErrorNode(ErrorNode errorNode) {

    }

    @Override
    public void enterEveryRule(ParserRuleContext parserRuleContext) {

    }

    @Override
    public void exitEveryRule(ParserRuleContext parserRuleContext) {

    }
}
