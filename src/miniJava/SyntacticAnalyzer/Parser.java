package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

import static miniJava.SyntacticAnalyzer.TokenKind.*;

public class Parser {

    private Scanner scanner;
    private ErrorReporter reporter;
    private Token token;
    private boolean trace = false;
    private SourcePosition prevPos;

    public Parser(Scanner scanner, ErrorReporter reporter) {
        this.scanner = scanner;
        this.reporter = reporter;
        this.prevPos = new SourcePosition();
    }

    /**
     * start parse
     */
    public Package parse() throws SyntaxError{
        token = scanner.scan();
        return parseProgram();
    }

    private Package parseProgram() throws SyntaxError {
        ClassDeclList classes = new ClassDeclList();
        SourcePosition pos = new SourcePosition();
        start(pos);
        while (token.kind != TokenKind.EOT) {
            SourcePosition classPos = new SourcePosition();
            start(classPos);

            FieldDeclList fieldDeclList = new FieldDeclList();
            MethodDeclList methodDeclList = new MethodDeclList();

            accept(TokenKind.CLASS);
            Identifier className = new Identifier(token);
            accept(IDENTIFIER);
            accept(TokenKind.LCBRACKET);
            while (token.kind != TokenKind.RCBRACKET) {
                SourcePosition declPos = new SourcePosition();
                start(declPos);
                boolean isPublic = true;
                if (token.kind == TokenKind.PUBLIC || token.kind == TokenKind.PRIVATE) {
                    if (token.kind == TokenKind.PRIVATE) isPublic = false;
                    acceptIt();
                }
                boolean isStatic = false;
                if (token.kind == TokenKind.STATIC) {
                    isStatic = true;
                    acceptIt();
                }
                if (token.kind == TokenKind.VOID) {
                    TypeDenoter type = new BaseType(TypeKind.VOID, token.posn);
                    acceptIt();
                    Identifier methodName = new Identifier(token);
                    accept(IDENTIFIER);
                    methodDeclList.add(parseMethod(isPublic, isStatic, type, methodName, declPos));
                } else {
                    TypeDenoter type = parseType();
                    Identifier id = new Identifier(token);
                    accept(IDENTIFIER);
                    if (token.kind == TokenKind.SIMICOLON) {
                        acceptIt();
                        finish(declPos);
                        fieldDeclList.add(new FieldDecl(!isPublic, isStatic, type, id, declPos));
                    } else {
                        methodDeclList.add(parseMethod(isPublic, isStatic, type, id, declPos));
                    }
                }
            }
            accept(TokenKind.RCBRACKET);
            finish(classPos);
            classes.add(new ClassDecl(className, fieldDeclList, methodDeclList, classPos));
        }
        accept(TokenKind.EOT);
        finish(pos);
        return new Package(classes, pos);
    }

    private MethodDecl parseMethod(boolean isPublic, boolean isStatic, TypeDenoter type, Identifier id, SourcePosition pos)
            throws SyntaxError {
        ParameterDeclList parameterDeclList = new ParameterDeclList();

        accept(TokenKind.LPAREN);
        if (token.kind == TokenKind.RPAREN) {
            acceptIt();
        } else {
            parameterDeclList = parseParameterList();
            accept(TokenKind.RPAREN);
        }

        StatementList statementList = new StatementList();
        accept(TokenKind.LCBRACKET);
        while (token.kind != TokenKind.RCBRACKET) {
            statementList.add(parseStatement());
        }
        accept(TokenKind.RCBRACKET);
        finish(pos);
        return new MethodDecl(new FieldDecl(!isPublic, isStatic, type, id, pos), parameterDeclList, statementList, pos);
    }

    private Statement parseStatement() throws SyntaxError {
    	SourcePosition pos = new SourcePosition();
    	start(pos);
    	Statement statement = null;
        switch (token.kind) {
            case IF:
                acceptIt();
                accept(TokenKind.LPAREN);
                Expression expression = parseExpression();
                accept(TokenKind.RPAREN);
                Statement thenStatment = parseStatement();
                if (token.kind == TokenKind.ELSE) {
                    acceptIt();
                    Statement elseStatment = parseStatement();
                    finish(pos);
                    statement = new IfStmt(expression, thenStatment, elseStatment, pos);
                    break;
                }
                finish(pos);
                statement = new IfStmt(expression, thenStatment, pos);
                break;
            case RETURN:
                acceptIt();
                if (token.kind == TokenKind.SIMICOLON) {
                    acceptIt();
                    finish(pos);
                    statement = new ReturnStmt(null, pos);
                    break;
                }
                Expression returnExp = parseExpression();
                accept(TokenKind.SIMICOLON);
                finish(pos);
                statement = new ReturnStmt(returnExp, pos);
                break;
            case WHILE:
                acceptIt();
                accept(TokenKind.LPAREN);
                Expression whileExp = parseExpression();
                accept(TokenKind.RPAREN);
                Statement bodyStmt = parseStatement();
                finish(pos);
                statement = new WhileStmt(whileExp, bodyStmt, pos);
                break;
            case LCBRACKET:
                acceptIt();
                StatementList list = new StatementList();
                while (token.kind != TokenKind.RCBRACKET) {
                    list.add(parseStatement());
                }
                accept(TokenKind.RCBRACKET);
                finish(pos);
                statement = new BlockStmt(list, pos);
                break;
            case IDENTIFIER:
                Identifier firstId = new Identifier(token);
                accept(TokenKind.IDENTIFIER);

                boolean isType = false;
                Identifier secondId = null;
                Reference reference = null;
                VarDecl varDecl = null;
                SourcePosition varPos = new SourcePosition();
                //Parse before '=' or '('
                if (token.kind == IDENTIFIER) {
                    // Type id -> id id
                    secondId = new Identifier(token);
                    acceptIt();
                    varPos.start = firstId.posn.start;
                    varPos.finish = secondId.posn.finish;
                    varDecl = new VarDecl(new ClassType(firstId, firstId.posn), secondId, varPos);
                    isType = true;
                } else if (token.kind == LBRACKET) {
                    acceptIt();
                    if (token.kind == RBRACKET) {
                        // Type id -> id[] id
                        acceptIt();
                        secondId = new Identifier(token);
                        accept(IDENTIFIER);
                        varPos.start = firstId.posn.start;
                        varPos.finish = secondId.posn.finish;
                        varDecl = new VarDecl(new ArrayType(new ClassType(firstId, firstId.posn), firstId.posn), secondId, varPos);
                        isType = true;
                    } else {
                        // Ref -> id [ Exp ] (.id([Exp])?)*
                        Expression indexExp = parseExpression();
                        accept(RBRACKET);
                        SourcePosition refPos = new SourcePosition();
                        refPos.start = pos.start;
                        finish(refPos);
                        reference = parseTrailingRef(new IxIdRef(firstId, indexExp, refPos));
                    }
                } else if (token.kind == DOT) {
                    SourcePosition idPos = new SourcePosition();
                    idPos.start = pos.start;
                    finish(idPos);
                    reference = parseTrailingRef(new IdRef(firstId, idPos));
                } else {
                    reference = new IdRef(firstId, firstId.posn);
                }

                // Parse assignment or method invocation
                if (token.kind == ASSIGN) {
                    acceptIt();
                    Expression valueExp = parseExpression();
                    accept(SIMICOLON);
                    finish(pos);
                    if (reference != null) {
                        statement = new AssignStmt(reference, valueExp, pos);
                    } else {
                        statement = new VarDeclStmt(varDecl, valueExp, pos);
                    }
                } else if (!isType && token.kind == LPAREN) {
                    acceptIt();
                    ExprList arguments = new ExprList();
                    if (token.kind != RPAREN) {
                        arguments = parseArgumentList();
                    }
                    accept(RPAREN);
                    accept(SIMICOLON);
                    finish(pos);
                    statement = new CallStmt(reference, arguments, pos);
                } else {
                    parseError("Error trying to parse a statement");
                }
                break;
            case THIS:
                //  Ref -> this (.id([Exp])?)*
                SourcePosition thisPos = token.posn;
                acceptIt();
                Reference thisRef = parseTrailingRef(new ThisRef(thisPos));
                // Parse assignment or method invocation
                if (token.kind == ASSIGN) {
                    acceptIt();
                    Expression assignExp = parseExpression();
                    accept(SIMICOLON);
                    finish(pos);
                    statement = new AssignStmt(thisRef, assignExp, pos);
                } else if (token.kind == LPAREN) {
                    acceptIt();
                    ExprList arguments = new ExprList();
                    if (token.kind != RPAREN) {
                         arguments = parseArgumentList();
                    }
                    accept(RPAREN);
                    accept(SIMICOLON);
                    finish(pos);
                    statement = new CallStmt(thisRef, arguments, pos);
                } else {
                    parseError("Error trying to parse a statement");
                }
                break;
            case INT:case BOOL:
                // Type id
                TypeDenoter type = parseType();
                Identifier id = new Identifier(token);
                accept(IDENTIFIER);
                accept(ASSIGN);
                Expression varDeclExp = parseExpression();
                accept(SIMICOLON);
                finish(pos);
                statement = new VarDeclStmt(new VarDecl(type, id, id.posn), varDeclExp, pos);
                break;
            default:
                parseError("Error trying to parse a statement");
        }
        return statement;
    }

    private Reference parseRef() throws SyntaxError {
        BaseRef baseRef = parseSingleRef();
        return parseTrailingRef(baseRef);
    }

    private BaseRef parseSingleRef() throws SyntaxError {
    	SourcePosition pos = new SourcePosition();
    	start(pos);
    	BaseRef baseRef = null;
        switch (token.kind) {
            case IDENTIFIER:
                Identifier id = new Identifier(token);
                acceptIt();
                if (token.kind == LBRACKET) {
                    // Ref -> id [ Exp ]
                    acceptIt();
                    Expression expression = parseExpression();
                    accept(RBRACKET);
                    finish(pos);
                    baseRef = new IxIdRef(id, expression, pos);
                    break;
                }
                finish(pos);
                baseRef = new IdRef(id, pos);
                break;
            case THIS:
                acceptIt();
                finish(pos);
                baseRef = new ThisRef(pos);
                break;
            default:
                parseError("Error when trying to parse a signle reference.");
        }
        return baseRef;
    }

    private Reference parseTrailingRef(BaseRef baseRef) throws SyntaxError {
    	SourcePosition pos = new SourcePosition();
    	pos.start = baseRef.posn.start;
    	Reference reference = baseRef;
        while (token.kind == DOT) {
            acceptIt();
            Identifier id = new Identifier(token);
            accept(IDENTIFIER);
            if (token.kind == LBRACKET) {
                // Ref -> id [ Exp ]
                acceptIt();
                Expression expression = parseExpression();
                accept(RBRACKET);
                finish(pos);
                reference = new IxQRef(reference, id, expression, pos);
            } else {
                finish(pos);
                reference = new QRef(reference, id, pos);
            }
        }
        return reference;
    }

    private Expression parseExpression() throws SyntaxError {
    	return parseOrExp();
    }

    private Expression parseOrExp() throws SyntaxError {
        Expression expr;
        SourcePosition pos = new SourcePosition();
        start(pos);

        expr = parseAndExp();
        while (token.kind == OR) {
            Operator op = new Operator(token);
            acceptIt();
            Expression rightExpr = parseAndExp();
            finish(pos);
            expr = new BinaryExpr(op, expr, rightExpr, pos);
        }
        return expr;
    }

    private Expression parseAndExp() throws SyntaxError {
        Expression expr;
        SourcePosition pos = new SourcePosition();
        start(pos);

        expr = parseEqualExpression();
        while (token.kind == AND) {
            Operator op = new Operator(token);
            acceptIt();
            Expression rightExpr = parseEqualExpression();
            finish(pos);
            expr = new BinaryExpr(op, expr, rightExpr, pos);
        }
        return expr;
    }

    private Expression parseEqualExpression() throws SyntaxError {
        Expression expr;
        SourcePosition pos = new SourcePosition();
        start(pos);

        expr = parseCompareExpression();
        while (token.kind == EQUAL || token.kind == NOT_EQUAL) {
            Operator op = new Operator(token);
            acceptIt();
            Expression rightExpr = parseCompareExpression();
            finish(pos);
            expr = new BinaryExpr(op, expr, rightExpr, pos);
        }
        return expr;
    }

    private Expression parseCompareExpression() throws SyntaxError {
        Expression expr;
        SourcePosition pos = new SourcePosition();
        start(pos);

        expr = parsePlusMinusExpression();
        while (token.kind == LESS || token.kind == LESS_EQUAL ||
                token.kind == GREATER || token.kind == GREATER_EQUAL) {
            Operator op = new Operator(token);
            acceptIt();
            Expression rightExpr = parsePlusMinusExpression();
            finish(pos);
            expr = new BinaryExpr(op, expr, rightExpr, pos);
        }
        return expr;
    }

    private Expression parsePlusMinusExpression() throws SyntaxError {
        Expression expr;
        SourcePosition pos = new SourcePosition();
        start(pos);

        expr = parseTimesDivideExpression();
        while (token.kind == PLUS || token.kind == MINUS) {
            Operator op = new Operator(token);
            acceptIt();
            Expression rightExpr = parseTimesDivideExpression();
            finish(pos);
            expr = new BinaryExpr(op, expr, rightExpr, pos);
        }
        return expr;
    }
    private Expression parseTimesDivideExpression() throws SyntaxError {
        Expression expr;
        SourcePosition pos = new SourcePosition();
        start(pos);

        expr = parseUnaryExpression();
        while (token.kind == TIMES || token.kind == DIVIDE) {
            Operator op = new Operator(token);
            acceptIt();
            Expression rightExpr = parseUnaryExpression();
            finish(pos);
            expr = new BinaryExpr(op, expr, rightExpr, pos);
        }
        return expr;
    }

    private Expression parseUnaryExpression() throws SyntaxError {
        Expression expr;
        SourcePosition pos = new SourcePosition();
        start(pos);

        if (token.kind == NOT || token.kind == MINUS) {
            Operator op = new Operator(token);
            acceptIt();
            Expression expression = parseUnaryExpression();
            finish(pos);
            expr = new UnaryExpr(op, expression, pos);
        } else {
            expr = parseSingleExpression();
        }
        return expr;
    }

    private Expression parseSingleExpression() throws SyntaxError {
        Expression expression = null;
    	SourcePosition pos = new SourcePosition();
    	start(pos);
        switch (token.kind) {
            case LPAREN:
                acceptIt();
                expression = parseExpression();
                accept(RPAREN);
                break;
            case NULL:
                NullLiteral nullLiteral = new NullLiteral(token);
                acceptIt();
                finish(pos);
                expression = new LiteralExpr(nullLiteral, pos);
                break;
            case NUM:
                IntLiteral intLiteral = new IntLiteral(token);
                acceptIt();
                finish(pos);
                expression = new LiteralExpr(intLiteral, pos);
                break;
            case TRUE: case FALSE:
                BooleanLiteral booleanLiteral = new BooleanLiteral(token);
                acceptIt();
                finish(pos);
                expression = new LiteralExpr(booleanLiteral, pos);
                break;
            case NEW:
                acceptIt();
                if (token.kind == IDENTIFIER) {
                    Identifier id = new Identifier(token);
                    acceptIt();
                    if (token.kind == LPAREN) {
                        acceptIt();
                        accept(RPAREN);
                        finish(pos);
                        expression = new NewObjectExpr(new ClassType(id, id.posn), pos);
                    } else if (token.kind == LBRACKET) {
                        acceptIt();
                        Expression exp = parseExpression();
                        accept(RBRACKET);
                        finish(pos);
                        expression = new NewArrayExpr(new ClassType(id, id.posn), exp, pos);
                    }
                } else if (token.kind == INT) {
                    SourcePosition intPos = token.posn;
                    acceptIt();
                    accept(LBRACKET);
                    Expression exp = parseExpression();
                    accept(RBRACKET);
                    finish(pos);
                    expression = new NewArrayExpr(new BaseType(TypeKind.INT, intPos), exp, pos);
                } else {
                    parseError("Error when trying to parse single expression.");
                }
                break;
            default:
                // Assume this a Ref
                Reference reference = parseRef();
                if (token.kind == LPAREN) {
                    acceptIt();
                    ExprList arguments = new ExprList();
                    if (token.kind != RPAREN) {
                        arguments = parseArgumentList();
                    }
                    accept(RPAREN);
                    finish(pos);
                    expression = new CallExpr(reference, arguments, pos);
                    break;
                }
                finish(pos);
                expression = new RefExpr(reference, pos);
                break;
        }

        return expression;
    }

    private ParameterDeclList parseParameterList() throws SyntaxError {
    	ParameterDeclList parameterDeclList = new ParameterDeclList();
    	SourcePosition paraPos = new SourcePosition();
    	start(paraPos);
        TypeDenoter type = parseType();
        Identifier pname = new Identifier(token);
        accept(IDENTIFIER);
        finish(paraPos);
        parameterDeclList.add(new ParameterDecl(type, pname, paraPos));

        while (token.kind == TokenKind.COMMA) {
            acceptIt();
            start(paraPos);
            type = parseType();
            pname = new Identifier(token);
            accept(IDENTIFIER);
            finish(paraPos);
            parameterDeclList.add(new ParameterDecl(type, pname, paraPos));
        }
        return parameterDeclList;
    }

    private ExprList parseArgumentList() throws SyntaxError {
        ExprList expressions = new ExprList();
        expressions.add(parseExpression());

        while (token.kind == TokenKind.COMMA) {
            acceptIt();
            expressions.add(parseExpression());
        }
        return expressions;
    }

    private TypeDenoter parseType() throws SyntaxError {
    	SourcePosition pos = new SourcePosition();
    	start(pos);
        switch (token.kind) {
            case INT:
                acceptIt();
                if (token.kind == TokenKind.LBRACKET) {
                    acceptIt();
                    accept(TokenKind.RBRACKET);
                    finish(pos);
                    return new ArrayType(new BaseType(TypeKind.INT, pos), pos);
                }
                return new BaseType(TypeKind.INT, pos);
            case BOOL:
                acceptIt();
                finish(pos);
                return new BaseType(TypeKind.BOOLEAN, pos);
            case IDENTIFIER:
                Token id = token;
                acceptIt();
                if (token.kind == TokenKind.LBRACKET) {
                    acceptIt();
                    accept(TokenKind.RBRACKET);
                    finish(pos);
                    return new ArrayType(new ClassType(new Identifier(id), pos), pos);
                }
                finish(pos);
                return new ClassType(new Identifier(id), pos);
            default:
                parseError("Error parsing: " + token.spelling);
                finish(pos);
                return new BaseType(TypeKind.ERROR, pos);
        }
    }

    private boolean isBinOp(TokenKind kind) {
        return (kind == LESS || kind == LESS_EQUAL
                || kind == GREATER || kind == GREATER_EQUAL
                || kind == EQUAL || kind == NOT_EQUAL
                || kind == AND || kind == OR
                || kind == PLUS || kind == MINUS
                || kind == TIMES || kind == DIVIDE);
    }

    /**
     * accept current token and advance to next token
     */
    private void acceptIt() throws SyntaxError {
    	prevPos = token.posn;
        accept(token.kind);
    }

    /**
     * verify that current token in input matches expected token and advance to next token
     * @param expectedTokenKind
     * @throws SyntaxError  if match fails
     */
    private void accept(TokenKind expectedTokenKind) throws SyntaxError {
        if (token.kind == expectedTokenKind) {
            if (trace)
                pTrace();
            prevPos = token.posn;
            token = scanner.scan();
        } else
            parseError("expecting '" + expectedTokenKind +
                    "' but found '" + token.kind + "'");
    }
    
    void start(SourcePosition position) {
		position.start = token.posn.start;
	}

	void finish(SourcePosition position) {
		position.finish = prevPos.finish;
	}

    /**
     * report parse error and unwind call stack to start of parse
     * @param e  string with error detail
     * @throws SyntaxError
     */
    private void parseError(String e) throws SyntaxError {
        reporter.reportError("Parse error: " + e);
        throw new SyntaxError();
    }

    // show parse stack whenever terminal is  accepted
    private void pTrace() {
        StackTraceElement[] stl = Thread.currentThread().getStackTrace();
        for (int i = stl.length - 1; i > 0; i--) {
            if (stl[i].toString().contains("parse"))
                System.out.println(stl[i]);
        }
        System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
        System.out.println();
    }

}
