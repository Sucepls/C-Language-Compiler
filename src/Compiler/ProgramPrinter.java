package Compiler;

import gen.CListener;
import gen.CParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;


import java.util.*;

public class ProgramPrinter implements CListener {

    private int indent_level;

    private final ArrayList<String> ErorrList = new ArrayList<>();

    private final ArrayList<Integer> return_line = new ArrayList<>();
    private final ArrayList<Scope> scopes;
    private int nested_layer;
    int counter;

    public ProgramPrinter() {
        this.scopes = new ArrayList<>();
        this.indent_level = 0;
        this.nested_layer = 0;
    }

    public void printScopes() {
        for (Scope scope : scopes
        ) {
            System.out.println(scope.toString());
            System.out.println("================================================================");
            System.out.println();
        }
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

    private Scope getScope(String name, int line) {
        for (Scope scope : scopes
        ) {
            if (scope.getName().equals(name) && scope.getScope_number() == line) {
                return scope;
            }
        }
        return null;
    }

    String getParamType(CParser.ParameterDeclarationContext ctx) {

        String par_type = ctx.declarationSpecifiers().getText();
        var paramLength = ctx.declarator().directDeclarator().Constant().size();

        if (paramLength > 0) {
            var length = ctx.declarator().directDeclarator().Constant().get(0).getText();
            return String.format("array_%s, length= %s", par_type, length);
        } else {
            return String.format("%s", par_type);
        }
    }

    @Override
    public void enterExternalDeclaration(CParser.ExternalDeclarationContext ctx) {
        System.out.println("program start {");
        indent_level++;

        Scope scope = new Scope("program", ctx.start.getLine(), Type.PROGRAM, ctx.getRuleIndex());
        scope.setParent(null);
        scopes.add(scope);

        //main method declaration
        String main_method_name = "Method_main";
        String main_method_value = String.format("Method (name : main) (return type: %s)", ctx.mainfunctionDefinition().declarationSpecifiers().declarationSpecifier(0).typeSpecifier().getText());
        SymbolTableItem main_symbolTableItem = new SymbolTableItem(ctx.mainfunctionDefinition().start.getLine(), main_method_value);
        scope.insert(main_method_name, main_symbolTableItem);

        //normal method declaration
        for (int i = 0; i < ctx.functionDefinition().size(); i++) {
            String normal_method_name = ctx.functionDefinition().get(i).declarator().directDeclarator().directDeclarator().Identifier().getText();
            String normal_method_type = ctx.functionDefinition().get(i).declarationSpecifiers().declarationSpecifier(0).typeSpecifier().getText();
            String normal_method_value = String.format("Method (name : %s) (return type: %s)", normal_method_name, normal_method_type);
            if (ctx.functionDefinition().get(i).declarator().directDeclarator().parameterTypeList() != null) {
                String paramerters = "";
                var parameters_list = ctx.functionDefinition().get(i).declarator().directDeclarator().parameterTypeList().parameterList().parameterDeclaration();
                for (int j = 0; j < parameters_list.size(); j++) {
                    String type = parameters_list.get(j).declarationSpecifiers().declarationSpecifier(0).getText();
                    if (parameters_list.get(j).declarator().directDeclarator().LeftBracket().size() != 0) {
                        type += " array";
                    }
                    paramerters += "[type: " + type + ", index: " + j + "]";
                    if (j < parameters_list.size() - 1) {
                        paramerters += ", ";
                    }
                }
                normal_method_value += paramerters;
            }
            if (!scope.Contain("Method_" + normal_method_name)) {
                scope.insert("Method_" + normal_method_name, new SymbolTableItem(ctx.start.getLine(), normal_method_value));
            } else {
                ErorrList.add(String.format("Error102 : in line [%d:%d] , method [%s] has been defined already", ctx.functionDefinition().get(i).start.getLine(), ctx.functionDefinition().get(i).start.getCharPositionInLine(), normal_method_name));
                scope.insert("Method_" + normal_method_name + "_" + ctx.functionDefinition().get(i).start.getLine() + "_" + ctx.functionDefinition().get(i).start.getCharPositionInLine(), new SymbolTableItem(ctx.functionDefinition().get(i).start.getLine(), normal_method_value));
            }
        }
    }

    @Override
    public void exitExternalDeclaration(CParser.ExternalDeclarationContext ctx) {
        System.out.println("}");
        System.out.println();
        printScopes();
        for (String s : ErorrList) {
            System.out.println(s);
            System.out.println();
        }
    }

    @Override
    public void enterPostfixExpression(CParser.PostfixExpressionContext ctx) {
        if (ctx.primaryExpression().Identifier() != null && ctx.LeftParen().isEmpty()) {
            if (!scopes.get(scopes.size() - 1).Contain("Field_" + ctx.primaryExpression().getText())) {
                ErorrList.add(String.format("Error106 : in line [%d:%d], Can not find Variable [%s]", ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.primaryExpression().getText()));
            }
        }
        if (ctx.LeftParen().size() != 0 && ctx.RightParen().size() != 0 && !ctx.primaryExpression().isEmpty() && !ctx.getText().contains("printf")) {
            indentation();
            if (!scopes.get(scopes.size() - 1).Contain("Field_" + ctx.primaryExpression().getText())) {
                ErorrList.add(String.format("Error106 : in line [%d:%d], Can not find Variable [%s]", ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.primaryExpression().getText()));
            } else {
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
        if (ctx.primaryExpression().Identifier() != null && !ctx.LeftParen().isEmpty()) {
            Scope scope = null;
            int arg2 = 0;
            boolean checked = true;
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
        if (ctx.initDeclaratorList() != null && !ctx.initDeclaratorList().isEmpty()) {
            for (int i = 0; i < ctx.initDeclaratorList().initDeclarator().size(); i++) {
                indentation();
                String name = ctx.initDeclaratorList().initDeclarator(i).declarator().directDeclarator().Identifier().getText();
                String type = ctx.declarationSpecifiers().getText();
                int length = 0;
                if (ctx.initDeclaratorList().initDeclarator(i).declarator().directDeclarator().Constant().size() > 0) {
                    length = Integer.parseInt(ctx.initDeclaratorList().initDeclarator(i).declarator().directDeclarator().Constant().get(0).toString());
                }
                String result = "field: " + name + "/ type: " + type;
                if (length > 0) {
                    result += "/ length: " + length;
                }
                System.out.println(result);
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
        for (int i = scopes.size() - 2; i >= 0; i--) {
            if (scopes.get(i).getScope_type() == Type.PROGRAM) {
                scope.setParent(scopes.get(i));
                scopes.get(i).getChildern().add(scope);
                break;
            }
        }
        scopes.add(scope);

        //fields
        try {
            var param = ctx.declarator().directDeclarator().parameterTypeList().parameterList().parameterDeclaration();
            String name = null;
            String type = null;
            if (param != null) {
                for (CParser.ParameterDeclarationContext parameterDeclarationContext : param) {
                    name = parameterDeclarationContext.declarator().directDeclarator().Identifier().getText();
                    type = getParamType(parameterDeclarationContext);
                    String value = String.format("methodParamField(name: %s) (type: %s)", name, type);
                    if (!scope.Contain("Field_" + name)) {
                        SymbolTableItem symbolTableItem = new SymbolTableItem(ctx.start.getLine(), value);
                        scope.insert("Field_" + name, symbolTableItem);
                    } else {
                        ErorrList.add(String.format("Error104 : in line [%d:%d] , field [%s] has been defined already", ctx.start.getLine(), ctx.start.getCharPositionInLine(), "Field_" + name));
                        scope.insert("Field_" + name + "_" + ctx.start.getLine() + "_" + ctx.start.getCharPositionInLine(), new SymbolTableItem(ctx.start.getLine(), value));
                    }
                }
            }
        }catch (Exception e){
            System.out.println();
        }

        //method fields
        String field_name = null;
        String field_type = null;
        int field_length = 0;
        try {
            var block_list = ctx.compoundStatement().blockItemList().blockItem();
            for (CParser.BlockItemContext blockItemContext : block_list) {
                field_length = 0;
                if (blockItemContext.declaration() != null) {
                    var declaration = blockItemContext.declaration().declarationSpecifiers().declarationSpecifier();
                    if (blockItemContext.declaration().initDeclaratorList() != null) {
                        var init_declaration = blockItemContext.declaration().initDeclaratorList().initDeclarator();
                        field_name = init_declaration.get(0).declarator().directDeclarator().Identifier().getText();
                        if (init_declaration.get(0).declarator().directDeclarator().LeftBracket().size() > 0) {
                            field_length = Integer.parseInt(init_declaration.get(0).declarator().directDeclarator().Constant().get(0).getText());
                        }
                    }
                    else if (blockItemContext.declaration().declarationSpecifiers().declarationSpecifier(1).typeSpecifier().typedefName().getText() != null) {
                        field_name = blockItemContext.declaration().declarationSpecifiers().declarationSpecifier(1).typeSpecifier().typedefName().getText();
                    }

//                    find type
                    for (CParser.DeclarationSpecifierContext declarationSpecifierContext : declaration) {
                        var type_specifier = declarationSpecifierContext.typeSpecifier();
                        field_type = type_specifier.getText();
                        break;
                    }
                }

                String method_field_value;
                if (field_length == 0)
                    method_field_value = String.format("methodField(name: %s) (type: %s)", field_name, field_type);
                else
                    method_field_value = String.format("methodField(name: %s) (type: %s array, length= %d)", field_name, field_type, field_length);

                if (!scope.Contain("Field_" + field_name)) {
                    SymbolTableItem symbolTableItem = new SymbolTableItem(ctx.start.getLine(), method_field_value);
                    scope.insert("Field_" + field_name, symbolTableItem);
                } else {
                    ErorrList.add(String.format("Error104 : in line [%d:%d] , field [%s] has been defined already", ctx.start.getLine(), ctx.start.getCharPositionInLine(), "Field_" + field_name));
                    scope.insert("Field_" + field_name + "_" + ctx.start.getLine() + "_" + ctx.start.getCharPositionInLine(), new SymbolTableItem(ctx.start.getLine(), method_field_value));
                }
            }
        }catch (Exception e){
            System.out.println();
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
        //set program as the parent of main
        for (Scope s : scopes
        ) {
            if (s.getScope_number() == 1) {
                scope.setParent(s);
                scopes.get(1).getChildern().add(scope);
                break;
            }
        }
        scopes.add(scope);
        var param = ctx.compoundStatement().blockItemList().blockItem();
        String name = null;
        String type = null;
        int length = 0;
        if (param != null) {
            for (int i = 0; i < param.size(); i++) {
                if (param.get(i).statement() != null && param.get(i).statement().expressionStatement() != null) {
                    name = param.get(i).statement().expressionStatement().start.getText();
                }
                if (param.get(i).declaration() != null) {
                    type = param.get(i).declaration().declarationSpecifiers().declarationSpecifier(0).typeSpecifier().getText();
                    if (param.get(i).declaration().initDeclaratorList() != null) {
                        var paramdec = param.get(i).declaration().initDeclaratorList().initDeclarator();
                        name = param.get(i).declaration().initDeclaratorList().initDeclarator().get(0).declarator().directDeclarator().Identifier().getText();
                    }
                    if (param.get(i).declaration().initDeclaratorList() != null && param.get(i).declaration().initDeclaratorList().initDeclarator(0).declarator().directDeclarator().LeftBracket().size() > 0
                            && param.get(i).declaration().initDeclaratorList().initDeclarator(0).declarator().directDeclarator().Constant().size() > 0) {
//                        length = Integer.parseInt(param.get(i).declaration().initDeclaratorList().initDeclarator(0).declarator().directDeclarator().Constant().get(0).getText());
                        length = Integer.parseInt(param.get(i).declaration().initDeclaratorList().initDeclarator(0).declarator().directDeclarator().Constant(0).getText());
                    }
                }
                String value = null;
                if (length == 0)
                    value = String.format("methodField(name: %s) (type: %s)", name, type);
                else
                    value = String.format("methodField(name: %s) (type: %s array, length= %d)", name, type, length);

                if (!scope.Contain("Field_" + name)) {
                    SymbolTableItem symbolTableItem = new SymbolTableItem(ctx.start.getLine(), value);
                    scope.insert("Field_" + name, symbolTableItem);
                } else {
                    ErorrList.add(String.format("Error104 : in line [%d:%d] , field [%s] has been defined already", ctx.start.getLine(), ctx.start.getCharPositionInLine(), "Field_" + name));
                    scope.insert("Field_" + name + "_" + ctx.start.getLine() + "_" + ctx.start.getCharPositionInLine(), new SymbolTableItem(ctx.start.getLine(), value));
                }
                length = 0;
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
        if (counter >= 1) {
            Scope scope = new Scope("nested", ctx.start.getLine(), Type.NESTED, ctx.getRuleIndex());
            int k;
            if (nested_layer == 0)
                k = scopes.size() - 1;
            else
                k = scopes.size() - 2;
            for (; k >= 0; k--) {
                if (scopes.get(k).getScope_type() == Type.FUNCTION || scopes.get(k).getScope_type() == Type.MAIN) {
                    scope.setParent(scopes.get(k));
                    scopes.get(k).getChildern().add(scope);
                    break;
                }
            }
            scopes.add(scope);

            String field_name = null;
            String field_type = null;
            int field_length = 0;
            try {
                var statements = ctx.statement();
                if (statements != null) {
                    for (int i = 0; i < statements.size(); i++) {
                        var item = statements.get(i).compoundStatement().blockItemList().blockItem();
                        for (CParser.BlockItemContext blockItemContext : item) {
//                        System.out.println("*******" + item.get(j).getText());
                            field_type = blockItemContext.start.getText();
//                    field_name=item.get(j).children.get(0).getChild(1).getText();
                            if (blockItemContext.declaration() != null) {
                                //find details of method fields
                                var declaration = item.get(i).declaration().declarationSpecifiers().declarationSpecifier();
                                if (item.get(i).declaration().initDeclaratorList() != null) {
                                    var init_declaration = item.get(i).declaration().initDeclaratorList().initDeclarator();
                                    field_name = init_declaration.get(0).declarator().directDeclarator().Identifier().getText();
                                    if (init_declaration.get(0).declarator().directDeclarator().LeftBracket().size() > 0) {
                                        field_length = Integer.parseInt(init_declaration.get(0).declarator().directDeclarator().Constant().get(0).getText());
                                    }
                                }
                                //find type
                                for (CParser.DeclarationSpecifierContext declarationSpecifierContext : declaration) {
                                    var type_specifier = declarationSpecifierContext.typeSpecifier();
                                    field_type = type_specifier.getText();
                                    break;
                                }
                                String key = "Field_" + field_name;
                                String value = "";
                                if (field_length == 0)
                                    value = String.format("methodField(name: %s) (type: %s)", field_name, field_type);
                                else
                                    value = String.format("methodField(name: %s) (type: %s array, length= %d)", field_name, field_type, field_length);
                                if (!scope.Contain(key)) {
                                    scope.insert(key, new SymbolTableItem(ctx.start.getLine(), value));
                                } else {
                                    ErorrList.add(String.format("Error102 : in line [%d:%d] , field [%s] has been defined already", ctx.start.getLine(), ctx.start.getCharPositionInLine(), key));
                                    scope.insert(key + "_" + ctx.start.getLine() + "_" + ctx.start.getCharPositionInLine(), new SymbolTableItem(ctx.start.getLine(), value));
                                }
                                field_length = 0;
                            }
                        }
                    }
            }
                nested_layer++;
                if (nested_layer == 1) {
                    startNestedStatement();
                }
            }catch (Exception e){
                System.out.println();
            }
        }
        counter++;
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
        if (counter >= 1) {
            Scope scope = new Scope("nested", ctx.start.getLine(), Type.NESTED, ctx.getRuleIndex());
            int k;
            if (nested_layer == 0)
                k = scopes.size() - 1;
            else
                k = scopes.size() - 2;
            for (; k >= 0; k--) {
                if (scopes.get(k).getScope_type() == Type.FUNCTION || scopes.get(k).getScope_type() == Type.MAIN) {
                    scope.setParent(scopes.get(k));
                    scopes.get(k).getChildern().add(scope);
                    break;
                }
            }
            scopes.add(scope);

            String field_name = null;
            String field_type = null;
            int field_length = 0;
            var list = ctx.statement().compoundStatement().blockItemList().blockItem();
            if (list != null) {
                for (CParser.BlockItemContext blockItemContext : list)
                    if (blockItemContext.declaration() != null) {
                        //find details of method fields
                        var declaration = blockItemContext.declaration().declarationSpecifiers().declarationSpecifier();
                        if (blockItemContext.declaration().initDeclaratorList() != null) {
                            var init_declaration = blockItemContext.declaration().initDeclaratorList().initDeclarator();
                            field_name = init_declaration.get(0).declarator().directDeclarator().Identifier().getText();
                            if (init_declaration.get(0).declarator().directDeclarator().LeftBracket().size() > 0) {
                                field_length = Integer.parseInt(init_declaration.get(0).declarator().directDeclarator().Constant().get(0).getText());
                            }
                        }
                        //find type
                        for (CParser.DeclarationSpecifierContext declarationSpecifierContext : declaration) {
                            var type_specifier = declarationSpecifierContext.typeSpecifier();
                            field_type = type_specifier.getText();
                            break;
                        }

                        String key = "Field_" + field_name;
                        String value = "";
                        if (field_length == 0)
                            value = String.format("methodField(name: %s) (type: %s)", field_name, field_type);
                        else
                            value = String.format("methodField(name: %s) (type: %s array, length= %d)", field_name, field_type, field_length);
                        if (!scope.Contain(key)) {
                            scope.insert(key, new SymbolTableItem(ctx.start.getLine(), value));
                        } else {
                            ErorrList.add(String.format("Error102 : in line [%d:%d] , field [%s] has been defined already", ctx.start.getLine(), ctx.start.getCharPositionInLine(), key));
                            scope.insert(key + "_" + ctx.start.getLine() + "_" + ctx.start.getCharPositionInLine(), new SymbolTableItem(ctx.start.getLine(), value));
                        }
                        field_length = 0;
                    }
            }

            nested_layer++;
            if (nested_layer == 1) {
                startNestedStatement();
            }
        }
        counter++;
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
