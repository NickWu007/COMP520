package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;

/**
 * Created by NickWu on 2017/3/23.
 */
public class TypeChecker implements Visitor<TypeDenoter, TypeDenoter> {

    ClassDecl currClass;
    AST ast;
    ErrorReporter errorReporter;
    public boolean userDefinedString = false;
    boolean hasMain = false;

    public TypeChecker(AST ast, ErrorReporter errorReporter, boolean userDefinedString) {
        this.ast = ast;
        this.errorReporter = errorReporter;
        this.userDefinedString = userDefinedString;
    }

    public void check() {
        ast.visit(this, null);
    }

    @Override
    public TypeDenoter visitPackage(Package prog, TypeDenoter arg) {
        for (ClassDecl classDecl : prog.classDeclList) {
            classDecl.visit(this, arg);
        }
        return null;
    }

    @Override
    public TypeDenoter visitClassDecl(ClassDecl cd, TypeDenoter arg) {
        currClass = cd;
        for (FieldDecl fieldDecl : cd.fieldDeclList) {
            fieldDecl.visit(this, arg);
        }
        for (MethodDecl methodDecl : cd.methodDeclList) {
            methodDecl.visit(this, arg);
        }
        return null;
    }

    @Override
    public TypeDenoter visitFieldDecl(FieldDecl fd, TypeDenoter arg) {
        fd.type = fd.type.visit(this, arg);
        if (fd.type.typeKind == TypeKind.VOID) {
            errorReporter.reportError("*** Type Checking Error at line " + fd.posn.start + ":Field " + fd.id.spelling + " is of type VOID");
        }
        return fd.type;
    }

    @Override
    public TypeDenoter visitMethodDecl(MethodDecl md, TypeDenoter arg) {
        md.type = md.type.visit(this, arg);
        if (md.id.spelling.equals("main")) {
            if (hasMain) {
                errorReporter.reportError("*** Type Checking Error at line " + md.posn.start + ":can only have ont main method");
            }
            hasMain = true;
            TypeDenoter typeDenoter = md.parameterDeclList.get(0).visit(this, arg);

            // expect to be String[]
            if (!(typeDenoter instanceof ArrayType)) {
                errorReporter.reportError("*** Type Checking Error at line " + md.posn.start + ":main method must have String[] as parameter");
            } else if (((ArrayType) typeDenoter).eltType.typeKind != TypeKind.UNSUPPORTED &&
                    !((ClassType)((ArrayType) typeDenoter).eltType).className.spelling.equals("String")) {
                errorReporter.reportError("*** Type Checking Error at line " + md.posn.start + ":main method must have String[] as parameter");
            }
        }

        md.returnType = null;
        for (ParameterDecl parameterDecl : md.parameterDeclList) {
            parameterDecl.visit(this, arg);
        }

        for (Statement statement : md.statementList) {
            statement.methodDecl = md;
            statement.visit(this, arg);
        }

        if (md.type.typeKind == TypeKind.VOID) {
            if (md.returnType != null && md.returnType.typeKind != TypeKind.VOID) {
                errorReporter.reportError("*** Type Checking Error at line " + md.posn.start + ":returning value in void method: " + md.id.spelling);
            }
            if (md.statementList.size() == 0 || !(md.statementList.get(md.statementList.size() -1) instanceof ReturnStmt)) {
                md.statementList.add(new ReturnStmt(null, new SourcePosition(md.posn.finish, md.posn.finish)));
            }
        } else {
            if (!(md.statementList.get(md.statementList.size() -1) instanceof ReturnStmt)) {
                errorReporter.reportError("*** Type Checking Error at line " + md.posn.finish + ":must have a return statement at end of method: " + md.id.spelling);
            }
            if (md.returnType == null) {
                errorReporter.reportError("*** Type Checking Error at line " + md.posn.start + ":Not returning value for method: " + md.id.spelling);
            } else {
                md.returnType = md.returnType.visit(this, arg);
                checkUnSupported(md.returnType);
                checkError(md.returnType);
                checkUnSupported(md.type);
                checkError(md.type);
                if (!md.type.equals(md.returnType)) {
                    errorReporter.reportError("*** Type Checking Error at line " + md.posn.start + ":Returning value is of wrong type than declared: " + md.id.spelling);
                }
            }
        }
        return md.type;
    }

    @Override
    public TypeDenoter visitParameterDecl(ParameterDecl pd, TypeDenoter arg) {
        pd.type = pd.type.visit(this, arg);

        if (pd.type.typeKind == TypeKind.VOID) {
            errorReporter.reportError("*** Type Checking Error at line " + pd.posn.start + ":variable cannot be of type VOID: " + pd.id.spelling);
        }
        return pd.type;
    }

    @Override
    public TypeDenoter visitVarDecl(VarDecl decl, TypeDenoter arg) {
        decl.type = decl.type.visit(this, arg);

        if (decl.type.typeKind == TypeKind.VOID) {
            errorReporter.reportError("*** Type Checking Error at line " + decl.posn.start + ":variable cannot be of type VOID: " + decl.id.spelling);
        }
        return decl.type;
    }

    @Override
    public TypeDenoter visitBaseType(BaseType type, TypeDenoter arg) {
        return type;
    }

    @Override
    public TypeDenoter visitClassType(ClassType type, TypeDenoter arg) {
        if (type.className.spelling.equals("String")) {
            if (userDefinedString) return type;
            else return new BaseType(TypeKind.UNSUPPORTED, type.posn);
        }
        return type;
    }

    @Override
    public TypeDenoter visitArrayType(ArrayType type, TypeDenoter arg) {
        TypeDenoter typeDenoter = type.eltType.visit(this, arg);
        return new ArrayType(typeDenoter, type.posn);
    }

    @Override
    public TypeDenoter visitBlockStmt(BlockStmt stmt, TypeDenoter arg) {
        for (Statement statement : stmt.sl) {
            statement.methodDecl = stmt.methodDecl;
            statement.visit(this, arg);
        }
        return null;
    }

    @Override
    public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, TypeDenoter arg) {
        TypeDenoter declaredType = stmt.varDecl.visit(this, arg);
        if (stmt.initExp != null) {
            TypeDenoter assignedType = stmt.initExp.visit(this, arg);
            checkUnSupported(assignedType);
            checkError(assignedType);
            if (!declaredType.equals(assignedType)) {
                errorReporter.reportError("*** Type Checking Error at line " + stmt.posn.start + ":Variable value is of incompatible type as declared");
            }
        }
        return null;
    }

    @Override
    public TypeDenoter visitAssignStmt(AssignStmt stmt, TypeDenoter arg) {
        TypeDenoter lhs = stmt.ref.visit(this, arg);
        TypeDenoter rhs = stmt.val.visit(this, arg);

        checkUnSupported(lhs);
        checkError(lhs);
        checkUnSupported(rhs);
        checkError(rhs);
        if (!lhs.equals(rhs)) {
            errorReporter.reportError("*** Type Checking Error at line " + stmt.posn.start + ":Assignment: lhs and rhs are of incompatiable types");
        }
        return null;
    }

    @Override
    public TypeDenoter visitCallStmt(CallStmt stmt, TypeDenoter arg) {
        if (stmt.methodRef.getDecl() instanceof MethodDecl) {
            MethodDecl methodDecl = (MethodDecl) stmt.methodRef.getDecl();
            if (stmt.argList.size() != methodDecl.parameterDeclList.size()) {
                errorReporter.reportError("*** Type Checking Error at line " + methodDecl.posn.start + ": method " + methodDecl.id.spelling + ": wrong number of arugments");
            } else {
                for (int i = 0; i < stmt.argList.size(); i++) {
                    TypeDenoter actualType = stmt.argList.get(i).visit(this, arg);
                    TypeDenoter declaredType = methodDecl.parameterDeclList.get(i).visit(this, arg);
                    if (!declaredType.equals(actualType)) {
                        errorReporter.reportError("*** Type Checking Error at line " + stmt.posn.start + ": method: " + methodDecl.id.spelling + ": #" + (i + 1) + " argument is of wrong type");
                    }
                }
            }
        } else {
            errorReporter.reportError("*** Type Checking Error at line " + stmt.posn.start + ":Calling something that's not a method");
        }

        return null;
    }

    @Override
    public TypeDenoter visitReturnStmt(ReturnStmt stmt, TypeDenoter arg) {
        TypeDenoter type;
        if (stmt.returnExpr == null) {
            type = new BaseType(TypeKind.VOID, stmt.posn);
        } else {
            type = stmt.returnExpr.visit(this, arg);
        }

        if (stmt.methodDecl.returnType != null) {
            errorReporter.reportError("*** Type Checking Error at line " + stmt.posn.start + ":returning different types of value in method " + stmt.methodDecl.id.spelling);
        }else {
            stmt.methodDecl.returnType = type;
        }
        return null;
    }

    @Override
    public TypeDenoter visitIfStmt(IfStmt stmt, TypeDenoter arg) {
        TypeDenoter condType;
        if (stmt.cond != null) {
            condType = stmt.cond.visit(this, arg);
            if (condType.typeKind != TypeKind.BOOLEAN) {
                errorReporter.reportError("*** Type Checking Error at line " + stmt.posn.start + ":Condition is not of Boolean type");
            }
        }

        stmt.thenStmt.methodDecl = stmt.methodDecl;
        stmt.thenStmt.visit(this, arg);

        if (stmt.elseStmt != null) {
            stmt.elseStmt.methodDecl = stmt.methodDecl;
            stmt.elseStmt.visit(this, arg);
        }
        return null;
    }

    @Override
    public TypeDenoter visitWhileStmt(WhileStmt stmt, TypeDenoter arg) {
        TypeDenoter condType;
        if (stmt.cond != null) {
            condType = stmt.cond.visit(this, arg);
            if (condType.typeKind != TypeKind.BOOLEAN) {
                errorReporter.reportError("*** Type Checking Error at line " + stmt.posn.start + ":Condition is not of Boolean type");
            }
        }

        stmt.body.methodDecl = stmt.methodDecl;
        stmt.body.visit(this, arg);
        return null;
    }

    @Override
    public TypeDenoter visitUnaryExpr(UnaryExpr expr, TypeDenoter arg) {
        String op = expr.operator.spelling;
        TypeDenoter typeDenoter = expr.expr.visit(this, arg);
        if (op.equals("-")) {
            if (typeDenoter.typeKind != TypeKind.INT) {
                errorReporter.reportError("*** Type Checking Error at line " + expr.posn.start + ":UnaryExpr: \'-\' is used for INT type only");
                return new BaseType(TypeKind.ERROR, new SourcePosition());
            }
            return new BaseType(TypeKind.INT, new SourcePosition());
        } else if (op.equals("!")) {
            if (typeDenoter.typeKind != TypeKind.BOOLEAN) {
                errorReporter.reportError("*** Type Checking Error at line " + expr.posn.start + ":UnaryExpr: \'-\' is used for BOOLEAN type only");
                return new BaseType(TypeKind.ERROR, new SourcePosition());
            }
            return new BaseType(TypeKind.BOOLEAN, new SourcePosition());
        } else {
            errorReporter.reportError("*** Type Checking Error at line " + expr.posn.start + ":UnaryExpr has a null op");
            return new BaseType(TypeKind.ERROR, new SourcePosition());
        }
    }

    @Override
    public TypeDenoter visitBinaryExpr(BinaryExpr expr, TypeDenoter arg) {
        String op = expr.operator.spelling;
        TypeDenoter leftExpType = expr.left.visit(this, null);
        TypeDenoter rightExpType = expr.right.visit(this, null);
        if (op.equals("||") || op.equals("&&")) {
            if (leftExpType.typeKind == TypeKind.BOOLEAN &&
                    rightExpType.typeKind == TypeKind.BOOLEAN) {
                return new BaseType(TypeKind.BOOLEAN, new SourcePosition());
            } else {
                errorReporter.reportError("*** Type Checking Error at line " + expr.posn.start + ":|| or && are used for BOOLEAN type only");
                return new BaseType(TypeKind.ERROR, new SourcePosition());
            }

        } else if (op.equals("==") || op.equals("!=")) {
            checkUnSupported(leftExpType);
            checkError(leftExpType);
            checkUnSupported(rightExpType);
            checkError(rightExpType);
            if (leftExpType.equals(rightExpType)) { //require the same type
                return new BaseType(TypeKind.BOOLEAN, new SourcePosition());
            } else {
                errorReporter.reportError("*** Type Checking Error at line " + expr.posn.start + ":== or != re used for same type only");
                return new BaseType(TypeKind.ERROR, new SourcePosition());
            }
        } else if (op.equals("<=") || op.equals(">=") || op.equals(">") || op.equals("<")) {
            if (leftExpType.typeKind == TypeKind.INT &&
                    rightExpType.typeKind == TypeKind.INT) {
                return new BaseType(TypeKind.BOOLEAN, new SourcePosition());
            } else {
                errorReporter.reportError("*** Type Checking Error at line " + expr.posn.start + ":>= <= > < are used for INT type only");
                return new BaseType(TypeKind.ERROR, new SourcePosition());
            }
        } else if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
            if (leftExpType.typeKind == TypeKind.INT &&
                    rightExpType.typeKind == TypeKind.INT) {
                return new BaseType(TypeKind.INT, new SourcePosition());
            } else {
                errorReporter.reportError("*** Type Checking Error at line " + expr.posn.start + ":+ - * / are used for INT type only");
                return new BaseType(TypeKind.ERROR, new SourcePosition());
            }
        } else {
            errorReporter.reportError("*** Type Checking Error at line " + expr.posn.start + ":Undefined operator...");
            return new BaseType(TypeKind.ERROR, new SourcePosition());
        }
    }

    @Override
    public TypeDenoter visitRefExpr(RefExpr expr, TypeDenoter arg) {
        if (expr.ref.getDecl() instanceof MethodDecl) {
            errorReporter.reportError("*** Type Checking Error at line " + expr.posn.start + ": Cannot use method name as an expression");
        }
        return expr.ref.visit(this, arg);
    }

    @Override
    public TypeDenoter visitCallExpr(CallExpr expr, TypeDenoter arg) {
        TypeDenoter type = null;
        if (expr.functionRef.getDecl() instanceof MethodDecl) {
            MethodDecl methodDecl = (MethodDecl) expr.functionRef.getDecl();
            if (expr.argList.size() != methodDecl.parameterDeclList.size()) {
                errorReporter.reportError("*** Type Checking Error at line " + expr.posn.start + ":Method " + methodDecl.id.spelling + ": wrong number of arugments");
            } else {
                for (int i = 0; i < expr.argList.size(); i++) {
                    TypeDenoter actualType = expr.argList.get(i).visit(this, arg);
                    TypeDenoter declaredType = methodDecl.parameterDeclList.get(i).visit(this, arg);
                    checkUnSupported(actualType);
                    checkError(actualType);
                    checkUnSupported(declaredType);
                    checkError(declaredType);
                    if (!declaredType.equals(actualType)) {
                        errorReporter.reportError("*** Type Checking Error at line " + expr.posn.start + ":Method: " + methodDecl.id.spelling + ": #" + (i + 1) + " argument is of wrong type");
                    }
                }
            }
            type = methodDecl.type;
        } else {
            errorReporter.reportError("*** Type Checking Error at line " + expr.posn.start + ":Calling something that's not a method");
        }

        return type;
    }

    @Override
    public TypeDenoter visitLiteralExpr(LiteralExpr expr, TypeDenoter arg) {
        return expr.lit.visit(this, arg);
    }

    @Override
    public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, TypeDenoter arg) {
        expr.type = expr.classtype.visit(this, arg);
        return expr.type;
    }

    @Override
    public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, TypeDenoter arg) {
        expr.type = new ArrayType(expr.eltType, expr.posn);
        return expr.type;
    }

    @Override
    public TypeDenoter visitThisRef(ThisRef ref, TypeDenoter arg) {
        return ref.declaration.type;
    }

    @Override
    public TypeDenoter visitIdRef(IdRef ref, TypeDenoter arg) {
    	if (ref.getType() == null) errorReporter.reportError("*** Type Checking Error at line " + ref.posn.start);
        return ref.getType();
    }

    @Override
    public TypeDenoter visitIxIdRef(IxIdRef ref, TypeDenoter arg) {
		TypeDenoter indexType = ref.indexExpr.visit(this, arg);
		if (indexType.typeKind != TypeKind.INT) {
			errorReporter.reportError("*** Type Checking Error at line " + ref.posn.start + ": indexExpr is not of INT type");
		}
    	if (ref.getType() == null) errorReporter.reportError("*** Type Checking Error at line " + ref.posn.start);
        return ref.getType();
    }

    @Override
    public TypeDenoter visitQRef(QRef ref, TypeDenoter arg) {
    	if (ref.getType() == null) errorReporter.reportError("*** Type Checking Error at line " + ref.posn.start);
        return ref.getType();
    }

    @Override
    public TypeDenoter visitIxQRef(IxQRef ref, TypeDenoter arg) {
    	TypeDenoter indexType = ref.ixExpr.visit(this, arg);
		if (indexType.typeKind != TypeKind.INT) {
			errorReporter.reportError("*** Type Checking Error at line " + ref.posn.start + ": indexExpr is not of INT type");
		}
    	if (ref.getType() == null) errorReporter.reportError("*** Type Checking Error at line " + ref.posn.start);
        return ref.getType();
    }

    @Override
    public TypeDenoter visitIdentifier(Identifier id, TypeDenoter arg) {
        return id.decl.type;
    }

    @Override
    public TypeDenoter visitOperator(Operator op, TypeDenoter arg) {
        return null;
    }

    @Override
    public TypeDenoter visitIntLiteral(IntLiteral num, TypeDenoter arg) {
        return new BaseType(TypeKind.INT, num.posn);
    }

    @Override
    public TypeDenoter visitNullLiteral(NullLiteral nullLiteral, TypeDenoter arg) {
        return new BaseType(TypeKind.ANY, nullLiteral.posn);
    }

    @Override
    public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, TypeDenoter arg) {
        return new BaseType(TypeKind.BOOLEAN, bool.posn);
    }

    @Override
    public TypeDenoter visitArrayLengthRef(ArrayLengthRef arrayLengthRef, TypeDenoter arg) {
        return null;
    }

    private void checkUnSupported(TypeDenoter typeDenoter) {
        if (typeDenoter.typeKind == TypeKind.UNSUPPORTED) {
            errorReporter.reportError("*** Type Checking Error at line " + typeDenoter.posn.start + ": Found variable with type UNSUPPORTED");
        }
    }

    private void checkError(TypeDenoter typeDenoter) {
        if (typeDenoter.typeKind == TypeKind.ERROR) {
            errorReporter.reportError("*** Type Checking Error at line " + typeDenoter.posn.start + ": Found variable with type ERROR");
        }
    }
}
