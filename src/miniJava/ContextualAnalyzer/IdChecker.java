package miniJava.ContextualAnalyzer;

import miniJava.*;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

import java.sql.Ref;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by NickWu on 2017/3/19.
 */
public class IdChecker implements Visitor<Integer, Object> {

    private AST ast;
    public ErrorReporter errorReporter;
    public IdTable idTable;

    private ClassDecl currClass;
    private boolean inStatic;
    private String currDeclaringVar;
    public boolean userDefinedString = false;
    public boolean hasMain;
    public HashMap<ClassDecl, Integer> classIndex;
    private HashSet<String> localVars;

    public IdChecker(AST ast, ErrorReporter reporter) {
        this.ast = ast;
        this.errorReporter = reporter;
        this.idTable = new IdTable();
    }

    public AST check() {
    	hasMain = false;
        ast.visit(this, 0);
        if (!hasMain) {
        	 errorReporter.reportError("*** Error: no main method");
        }
        return ast;
    }

    @Override
    public Object visitPackage(Package prog, Integer flag) {
        ClassDeclList classDecls = prog.classDeclList;

        idTable.openScope();

        for (ClassDecl classDecl : classDecls) {
            if (classDecl.id.spelling.startsWith("_")) {
                errorReporter.reportError("*** At line " + classDecl.posn.start + ": Illegal class name");
                return null;
            }
            if (classDecl.id.spelling.equals("String")) {
                this.userDefinedString = true;
            }
            if (idTable.enterDecl(classDecl) == null) {
                errorReporter.reportError("*** Identification Error at line " + classDecl.posn.start + ": Duplicate declaration of " + classDecl.id.spelling);
            }
        }

        for (ClassDecl classDecl : classDecls) {
            for (FieldDecl fieldDecl : classDecl.fieldDeclList) {
                fieldDecl.classDecl = classDecl;
            }
            for (MethodDecl methodDecl : classDecl.methodDeclList) {
                methodDecl.classDecl = classDecl;
            }
        }

        classIndex = new HashMap<>();
        for (ClassDecl classDecl : classDecls) {
            idTable.openScope();
            for (FieldDecl fieldDecl : classDecl.fieldDeclList) {
                fieldDecl.visit(this, 0);
                if (idTable.enterDecl(fieldDecl) == null) {
                    errorReporter.reportError("*** Identification Error at line " + fieldDecl.posn.start + ": Duplicate declaration of " + fieldDecl.id.spelling);
                }
            }

            for (MethodDecl methodDecl : classDecl.methodDeclList) {
                if (idTable.enterDecl(methodDecl) == null) {
                    errorReporter.reportError("*** Identification Error at line " + methodDecl.posn.start + ": Duplicate declaration of " + methodDecl.id.spelling);
                }
                if (methodDecl.isMain()) {
                	if (hasMain) {
                		errorReporter.reportError("*** Identification Error at line " + methodDecl.posn.start + ": Duplicate declaration of main method");
                	} else {
                		hasMain = true;
                	}
                }
            }
            classIndex.put(classDecl, idTable.getCurrLevel());
        }

        int topClassIndex = idTable.getCurrLevel();
        for (ClassDecl classDecl : classDecls) {
            currClass = classDecl;
            idTable.switchTable(classIndex.get(classDecl), topClassIndex);
            classDecl.visit(this, 0);
        }

        idTable.closeScope();
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Integer flag) {
        for (MethodDecl methodDecl : cd.methodDeclList) {
            methodDecl.visit(this, 0);
        }
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Integer flag) {
        // Check if there are type references that need id
        fd.type.visit(this, 0);
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Integer flag) {
        md.type.visit(this, 0);

        inStatic = md.isStatic;
        idTable.openScope();
        localVars = new HashSet<>();
        for (ParameterDecl pd : md.parameterDeclList) {
            pd.visit(this, 0);
        }

        for (Statement statement : md.statementList) {
            statement.visit(this, 0);
        }
        idTable.closeScope();
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Integer flag) {
        pd.type.visit(this, 0);
        pd.setIdBinding();
        if (localVars.contains(pd.id.spelling)) {
        	errorReporter.reportError("*** Identification Error at line " + pd.posn.start + ": Duplicate declaration of " + pd.id.spelling);
        } else {
        	localVars.add(pd.id.spelling);
        }
        if (idTable.enterDecl(pd) == null) {
            errorReporter.reportError("*** Identification Error at line " + pd.posn.start + ": Duplicate declaration of " + pd.id.spelling);
        }
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Integer flag) {
        // First check if type is defined.
        decl.type.visit(this, 0);
        decl.setIdBinding();
        if (localVars.contains(decl.id.spelling)) {
        	errorReporter.reportError("*** Identification Error at line " + decl.posn.start + ": Duplicate declaration of " + decl.id.spelling);
        } else {
        	localVars.add(decl.id.spelling);
        }
        if (idTable.enterDecl(decl) == null) {
            errorReporter.reportError("*** Identification Error at line " + decl.posn.start + ": Duplicate declaration of " + decl.id.spelling);
        }
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Integer flag) {
        // No need to check, just return.
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Integer flag) {
        ClassDecl classDecl = (ClassDecl) idTable.findClass(type.className.spelling);
        if (classDecl == null) {
            errorReporter.reportError("*** Identification Error at line " + type.posn.start + ": Expected a class name");
        } else {
            type.className.decl = classDecl;
        }
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Integer flag) {
        type.eltType.visit(this, 0);
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Integer flag) {
        idTable.openScope();

        for (Statement statement : stmt.sl) {
            statement.visit(this, 0);
        }
        
        HashMap<String, Declaration> currTable = idTable.getCurrIdTable();
        for (String var : currTable.keySet()) {
        	if (localVars.contains(var)) {
        		localVars.remove(var);
        	}
        }
        
        idTable.closeScope();
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Integer flag) {
        currDeclaringVar = stmt.varDecl.id.spelling;
        stmt.initExp.visit(this, 0);
        currDeclaringVar = "";
        stmt.varDecl.visit(this, 0);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Integer flag) {
        if (stmt.ref instanceof ThisRef) {
            errorReporter.reportError("*** Identification Error at line " + stmt.posn.start + ": \'this\' cannot be used on the left hand side of an assignment");
        } else {
            stmt.ref.visit(this, 0);
        }
        stmt.val.visit(this, 0);
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Integer flag) {
        stmt.methodRef.visit(this, 0);

        MethodDecl methodDecl = (MethodDecl) stmt.methodRef.getDecl();

        if (methodDecl.parameterDeclList.size() != stmt.argList.size()) {
            errorReporter.reportError("*** Identification Error at line " + stmt.posn.start + ": For method: " + methodDecl.id.spelling + " number of arguments does not agree with number of parameters");
            return null;
        }

        for (Expression expression : stmt.argList) {
            expression.visit(this, 0);
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Integer flag) {
        if (stmt.returnExpr != null) stmt.returnExpr.visit(this, 0);
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Integer flag) {
        stmt.cond.visit(this, 0);

        if (stmt.thenStmt instanceof VarDeclStmt) {
            errorReporter.reportError("*** Identification Error at line " + stmt.thenStmt.posn.start + ": a variable declaration cannot be the solitary statement in a branch of a conditional statement");
        } else {
            stmt.thenStmt.visit(this, 0);
        }

        if (stmt.elseStmt != null) {
            if (stmt.elseStmt instanceof VarDeclStmt) {
                errorReporter.reportError("*** Identification Error at line " + stmt.elseStmt.posn.start + ": a variable declaration cannot be the solitary statement in a branch of a conditional statement");
            } else {
                stmt.elseStmt.visit(this, 0);
            }
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Integer flag) {
        stmt.cond.visit(this, 0);
        if (stmt.body != null) {
            if (stmt.body instanceof VarDeclStmt) {
                errorReporter.reportError("*** Identification Error at line " + stmt.body.posn.start + ": a variable declaration cannot be the solitary statement in the body of a while loop");
            } else {
                stmt.body.visit(this, 0);
            }
        }
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Integer flag) {
        expr.expr.visit(this, 0);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Integer flag) {
        expr.left.visit(this, 0);
        expr.right.visit(this, 0);
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Integer flag) {
        expr.ref.visit(this, 0);
        if (expr.ref instanceof IdRef && (expr.ref.getDecl() instanceof ClassDecl || expr.ref.getDecl() instanceof MethodDecl)) {
            errorReporter.reportError("*** Identification Error at line " + expr.posn.start + ": Cannot reference class/method name only in a RefExpr");
        }
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Integer flag) {
        expr.functionRef.visit(this, 2);


        MethodDecl methodDecl = (MethodDecl) (expr.functionRef.getDecl());
        if (methodDecl.parameterDeclList.size() != expr.argList.size()) {
            errorReporter.reportError("*** Identification Error at line " + expr.posn.start + ": For method: " + methodDecl.id.spelling + " number of arguments does not agree with number of parameters");
            return null;
        }
        for (Expression expression : expr.argList) {
            expression.visit(this, 0);
        }
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Integer flag) {
        expr.lit.visit(this, 0);
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Integer flag) {
        expr.classtype.className.visit(this, 0);
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Integer flag) {
        expr.eltType.visit(this, 0);
        expr.sizeExpr.visit(this, 0);
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Integer flag) {
        if (inStatic) {
            errorReporter.reportError("*** dentification Error at line " + ref.posn.start + ": \'this\' should not be used in static context.");
        }

        // Find the closest class decl
        ref.declaration = currClass;
        ref.isStatic = false;
        return ref;
    }

    @Override
    public Object visitIdRef(IdRef ref, Integer flag) {
        ref.id.visit(this, flag);
        ref.isStatic = ref.id.isStatic;
        if (ref.id.spelling.equals(currDeclaringVar)) {
            errorReporter.reportError("*** Identification Error at line " + ref.id.posn.start + ": " + currDeclaringVar + " is not initialized");
            return null;
        }

        if (ref.getDecl() instanceof MemberDecl) {
            MemberDecl memberDecl = (MemberDecl) ref.getDecl();
            ClassDecl classDecl = memberDecl.classDecl;
            if (classDecl != currClass) {
                errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Cannot find symbol: " + ref.id.spelling + " under scope of class: " + classDecl.id.spelling);
            }
        }
        return null;
    }

    @Override
    public Object visitIxIdRef(IxIdRef ref, Integer flag) {
        ref.id.visit(this, flag);
        ref.isStatic = ref.id.isStatic;
        if (ref.getDecl().type.typeKind != TypeKind.ARRAY) {
            errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": " + ref.id.spelling + " is not of array type");
        } else {
            ref.indexExpr.visit(this, 0);
        }
        return null;
    }

    @Override
    public Object visitQRef(QRef ref, Integer flag) {
        ref.ref.visit(this, 0);
        Declaration formerDeclaration = ref.ref.getDecl();
        boolean oldStatic = inStatic;
        inStatic = ref.ref.isStatic;
        if (formerDeclaration == null) return null;
        if (formerDeclaration instanceof MethodDecl) {
        	errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Cannot have a method name in the middle of a QRef");
        }
        if (formerDeclaration instanceof ClassDecl) {
            ClassDecl classDecl = (ClassDecl) formerDeclaration;
            ref.id.visit(this, 3);
            ref.isStatic = ref.id.isStatic;
            Declaration declaration = ref.getDecl();
            boolean sameClass = classDecl == currClass;

            if (declaration instanceof MemberDecl) {
                if (!sameClass) {
                    // Check visible & static
                    String checkName = declaration.id.spelling;
                    if (!classDecl.existsMember(checkName, inStatic, true)) {
                        errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Cannot find static and public member: " + declaration.id.spelling);
                    }
                } else {
                    if (!classDecl.existsMember(declaration.id.spelling, inStatic, false)) {
                        errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Cannot find member: " + declaration.id.spelling);
                    }
                }
            } else {
                errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Expect a member declaration: " + declaration.id.spelling);
            }

        } else {
            ref.id.visit(this, 3);
            ref.isStatic = ref.id.isStatic;
            Declaration declaration = ref.getDecl();
            if (formerDeclaration.type.typeKind == TypeKind.CLASS) {
                ClassDecl classDecl = (ClassDecl) (formerDeclaration.type.getDecl());
                if (formerDeclaration instanceof ClassDecl) classDecl = (ClassDecl)formerDeclaration;
                boolean sameClass = classDecl == currClass;

                if (declaration instanceof MemberDecl) {
                    if (!sameClass) {
                        // Check visible
                        String checkName = declaration.id.spelling;
                        if (!classDecl.existsMember(checkName, false, true)) {
                            errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Cannot find public member: " + declaration.id.spelling);
                        }
                    } else {
                        if (!classDecl.existsMember(declaration.id.spelling, false, false)) {
                            errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Cannot member: " + declaration.id.spelling);
                        }
                    }
                } else {
                    errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Expect a member declaration: " + declaration.id.spelling);
                }
            } else if (formerDeclaration.type.typeKind == TypeKind.ARRAY) {
                if (ref.ref instanceof IxIdRef || ref.ref instanceof IxQRef) {
                    ClassDecl classDecl = (ClassDecl) (formerDeclaration.type.getDecl());
                    boolean sameClass = classDecl == currClass;

                    if (declaration instanceof MemberDecl) {
                        if (!sameClass) {
                            // Check visible
                            String checkName = declaration.id.spelling;
                            if (!classDecl.existsMember(checkName, false, true)) {
                                errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Cannot find public member: " + declaration.id.spelling);
                            }
                        } else {
                            if (!classDecl.existsMember(declaration.id.spelling, false, false)) {
                                errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Cannot find member: " + declaration.id.spelling);
                            }
                        }
                    } else {
                        errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Expect a member declaration: " + declaration.id.spelling);
                    }
                } else {
                    if (!ref.id.spelling.equals("length")) errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Referencing a member of an ARRAY");
                }
            } else {
                errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": referncing member of primitive types");
            }
        }
        inStatic = oldStatic;

        return null;
    }

    @Override
    public Object visitIxQRef(IxQRef ref, Integer flag) {
        ref.ref.visit(this, 0);
        boolean oldStatic = inStatic;
        inStatic = ref.ref.isStatic;
        Declaration formerDeclaration = ref.ref.getDecl();
        if (formerDeclaration == null) return null;
        if (formerDeclaration instanceof MethodDecl) {
        	errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Cannot have a method name in the middle of a QRef");
        }
        if (formerDeclaration instanceof ClassDecl) {
            ClassDecl classDecl = (ClassDecl) formerDeclaration;
            ref.id.visit(this, 3);
            ref.isStatic = ref.id.isStatic;
            Declaration declaration = ref.getDecl();
            boolean sameClass = classDecl == currClass;

            if (declaration instanceof MemberDecl) {
                if (!sameClass) {
                    // Check visible & static
                    if (!classDecl.existsMember(declaration.id.spelling, inStatic, true)) {
                        errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Cannot find static and public member: " + declaration.id.spelling);
                    }
                } else {
                    // Check static
                    if (!classDecl.existsMember(declaration.id.spelling, inStatic, false)) {
                        errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Cannot find static member: " + declaration.id.spelling);
                    }
                }
            } else {
                errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Expect a member declaration: " + declaration.id.spelling);
            }
        } else {
            ref.id.visit(this, 3);
            ref.isStatic = ref.id.isStatic;
            Declaration declaration = ref.getDecl();
            if (formerDeclaration.type.typeKind == TypeKind.CLASS) {
                ClassDecl classDecl = (ClassDecl) (formerDeclaration.type.getDecl());
                boolean sameClass = classDecl == currClass;

                if (declaration instanceof MemberDecl) {
                    if (!sameClass) {
                        // Check visible
                        String checkName = declaration.id.spelling;
                        if (!classDecl.existsMember(checkName, false, true)) {
                            errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Cannot find public member: " + declaration.id.spelling);
                        }
                    }
                } else {
                    errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Expect a member declaration: " + declaration.id.spelling);
                }
            } else if (formerDeclaration.type.typeKind == TypeKind.ARRAY) {
                if (ref.ref instanceof IxIdRef || ref.ref instanceof IxQRef) {
                    ClassDecl classDecl = (ClassDecl) (formerDeclaration.type.getDecl());
                    boolean sameClass = classDecl == currClass;

                    if (declaration instanceof MemberDecl) {
                        if (!sameClass) {
                            // Check visible
                            String checkName = declaration.id.spelling;
                            if (!classDecl.existsMember(checkName, false, true)) {
                                errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Cannot find public member: " + declaration.id.spelling);
                            }
                        }
                    } else {
                        errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": Expect a member declaration: " + declaration.id.spelling);
                    }
                }
            }
        }

        if (ref.getDecl().type.typeKind != TypeKind.ARRAY) {
            errorReporter.reportError("*** Identification Error at line " + ref.posn.start + ": " + ref.id.spelling + " is not of array type");
        } else {
            ref.ixExpr.visit(this, 0);
        }

        inStatic = oldStatic;
        return null;
    }


    @Override
    public Object visitIdentifier(Identifier id, Integer flag) {

        id.isStatic = false;
        if (id.spelling.equals("length")) {
            id.decl = new FieldDecl(false, false, new BaseType(TypeKind.INT, id.posn), id, id.posn);
            return null;
        }

        if (flag == 2) {
            Declaration declaration = idTable.findMethod(id.spelling);
            if (declaration == null) {
                errorReporter.reportError("*** Identification Error at line " + id.posn.start + ": Expecting a method name");
            } else {
                id.decl = declaration;
            }
            return null;
        }
        
        if (flag == 3) {
            Declaration declaration = idTable.findMember(id.spelling);
            if (declaration == null) {
                errorReporter.reportError("*** Identification Error at line " + id.posn.start + ": Expecting a method name");
            } else {
                id.decl = declaration;
            }
            return null;
        }

        Declaration decl = idTable.find(id.spelling);
        if (decl == null) {
            errorReporter.reportError("*** Identification Error at line " + id.posn.start + ": Cannot find symbol: " + id.spelling);
        } else {
            if (decl instanceof ClassDecl) {
                id.decl = decl;
                id.isStatic = true;
            } else {
                if (decl instanceof MemberDecl) {
                    MemberDecl memberDecl = (MemberDecl) decl;
                    if ((inStatic && !memberDecl.isStatic)) {
                        errorReporter.reportError("*** Identification Error at line " + id.posn.start + ": Referencing non-static variables in static methods");
                    }
                    boolean sameClass = currClass == memberDecl.classDecl;
                    if (!sameClass && !inStatic && memberDecl.isStatic) {
                        errorReporter.reportError("*** Identification Error at line " + id.posn.start + ": Referencing static variables in non-static methods");
                    }
                }
                decl.type.visit(this, 0);
                id.decl = decl;
            }
        }
        return null;
    }

    @Override
    public Object visitOperator(Operator op, Integer flag) {
        // No need to check, just return.
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Integer flag) {
        // No need to check, just return.
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nullLiteral, Integer flag) {
        // No need to check, just return.
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Integer flag) {
        // No need to check, just return.
        return null;
    }

    @Override
    public Object visitArrayLengthRef(ArrayLengthRef arrayLengthRef, Integer flag) {
        return null;
    }
}
