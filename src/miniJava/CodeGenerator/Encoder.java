package miniJava.CodeGenerator;

import com.sun.org.apache.bcel.internal.generic.POP;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.TokenKind;
import miniJava.mJAM.Disassembler;
import miniJava.mJAM.Machine;
import miniJava.mJAM.ObjectFile;

import static miniJava.mJAM.Machine.Op.*;
import static miniJava.mJAM.Machine.Reg.LB;
import static miniJava.mJAM.Machine.Reg.OB;
import static miniJava.mJAM.Machine.Reg.SB;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Created by NickWu on 2017/4/8.
 */
public class Encoder implements Visitor<Integer, Integer> {


    public AST ast;
    int mainAddr;
    int frameOffset;
    ArrayList<PatchLine> patchList;
    ErrorReporter errorReporter;
    String fileName;

    public Encoder(AST ast, ErrorReporter reporter) {
        this.ast = ast;
        this.errorReporter = reporter;
        this.patchList = new ArrayList<>();
        this.fileName = fileName;
    }

    public void generate() {
        Machine.initCodeGen();
        ast.visit(this, null);
        for (PatchLine patchLine : patchList) {
            Machine.patch(patchLine.line, ((Address) patchLine.declaration.runtimeEntity).address.offset);
        }
    }

    @Override
    public Integer visitPackage(Package prog, Integer arg) {

        // Load in static fields
        int staticOffset = 0;
        for (ClassDecl classDecl : prog.classDeclList) {
            for (FieldDecl fieldDecl : classDecl.fieldDeclList) {
                if (fieldDecl.isStatic) {
                    Machine.emit(Machine.Op.PUSH, 1);
                    fieldDecl.runtimeEntity = new Address(Machine.characterSize, staticOffset);
                    staticOffset++;
                }
            }

            int size = classDecl.fieldDeclList.size() - staticOffset;
            classDecl.runtimeEntity = new Address(size, classDecl.fieldDeclList.size() - staticOffset);
        }
        Machine.emit(Machine.Op.LOADL, 0);
        Machine.emit(Machine.Prim.newarr);
        mainAddr = Machine.nextInstrAddr();
        Machine.emit(Machine.Op.CALL, Machine.Reg.CB, -1);
        Machine.emit(Machine.Op.HALT, 0, 0, 0);

        for (ClassDecl classDecl : prog.classDeclList) {
            for (int i = 0; i < classDecl.fieldDeclList.size(); i++) {
                classDecl.fieldDeclList.get(i).visit(this, i);
            }
        }

        for (ClassDecl classDecl : prog.classDeclList) {
            classDecl.visit(this, arg);
        }

        return null;
    }

    @Override
    public Integer visitClassDecl(ClassDecl cd, Integer arg) {
        for (MethodDecl methodDecl : cd.methodDeclList) {
            methodDecl.visit(this, arg);
        }
        return null;
    }

    @Override
    public Integer visitFieldDecl(FieldDecl fd, Integer arg) {
        if (!fd.isStatic) {
            fd.runtimeEntity = new Address(Machine.characterSize, arg);
        }
        return null;
    }

    @Override
    public Integer visitMethodDecl(MethodDecl md, Integer arg) {
        md.runtimeEntity = new Address(Machine.addressSize, Machine.nextInstrAddr());

        if (md.id.spelling.equals("main")) {
            Machine.patch(mainAddr, Machine.nextInstrAddr());
        }

        int paraOffset = -1 * md.parameterDeclList.size();
        for (ParameterDecl parameterDecl : md.parameterDeclList) {
            parameterDecl.visit(this, paraOffset);
            paraOffset++;
        }

        frameOffset = 3;
        for (Statement statement : md.statementList) {
            statement.visit(this, arg);
        }

        if (md.type.typeKind == TypeKind.VOID) {
            Machine.emit(Machine.Op.RETURN, 0 ,0, md.parameterDeclList.size());
        } else {
            Machine.emit(Machine.Op.RETURN, 1 ,0, md.parameterDeclList.size());
        }
        return null;
    }

    @Override
    public Integer visitParameterDecl(ParameterDecl pd, Integer arg) {
        pd.runtimeEntity = new Address(Machine.addressSize, arg);
        return null;
    }

    @Override
    public Integer visitVarDecl(VarDecl decl, Integer arg) {
        decl.runtimeEntity = new Address(Machine.characterSize, frameOffset);
        return null;
    }

    @Override
    public Integer visitBaseType(BaseType type, Integer arg) {
        return null;
    }

    @Override
    public Integer visitClassType(ClassType type, Integer arg) {
        return null;
    }

    @Override
    public Integer visitArrayType(ArrayType type, Integer arg) {
        return null;
    }

    @Override
    public Integer visitBlockStmt(BlockStmt stmt, Integer arg) {
        int numVar = 0;
        for (Statement statement : stmt.sl) {
            if (statement instanceof VarDeclStmt) {
                numVar++;
            }
            statement.visit(this, arg);
        }
        Machine.emit(Machine.Op.POP, 0, 0, numVar);
        frameOffset -= numVar;
        return null;
    }

    @Override
    public Integer visitVardeclStmt(VarDeclStmt stmt, Integer arg) {
        stmt.varDecl.visit(this, arg);
        stmt.initExp.visit(this, 1);
        frameOffset++;
        return null;
    }

    @Override
    public Integer visitAssignStmt(AssignStmt stmt, Integer arg) {
        if (stmt.ref instanceof IdRef) {
            int offset = stmt.ref.visit(this, 2);
            stmt.val.visit(this, 1);
            if (stmt.ref.getDecl() instanceof FieldDecl &&
                    ((FieldDecl) (stmt.ref.getDecl())).isStatic) {
                Machine.emit(Machine.Op.STORE, SB, offset);
            } else if (stmt.ref.getDecl() instanceof FieldDecl) { //non static fieldDecl
                Machine.emit(Machine.Op.STORE, Machine.Reg.OB, offset); // store into var
            } else {
                Machine.emit(Machine.Op.STORE, Machine.Reg.LB, offset); // store into var
            }
        } else if (stmt.ref instanceof IxIdRef || stmt.ref instanceof IxQRef) {
            stmt.ref.visit(this, 2);
            
            stmt.val.visit(this, 1);
            Machine.emit(Machine.Prim.arrayupd);
        } else if (stmt.ref instanceof QRef) {
            if (stmt.ref.getDecl() instanceof FieldDecl &&
                    ((FieldDecl) (stmt.ref.getDecl())).isStatic) {
                int offset = stmt.ref.visit(this, 2);
                stmt.val.visit(this, 1);
                Machine.emit(Machine.Op.STORE, SB, offset);
            }  else {
                stmt.ref.visit(this, 2);
                stmt.val.visit(this, 1);
                Machine.emit(Machine.Prim.fieldupd); // store into var
            }
        }
        return null;
    }

    @Override
    public Integer visitCallStmt(CallStmt stmt, Integer arg) {
        for (Expression argument : stmt.argList) {
            argument.visit(this, 1);
        }

        if (stmt.methodRef instanceof QualifiedRef && stmt.methodRef.getDecl() instanceof MethodDecl && stmt.methodRef.getDecl().id.spelling.equals("println")) {
            Machine.emit(Machine.Prim.putintnl);
        } else if (stmt.methodRef instanceof IdRef) {
            // foo()
            if (((MethodDecl) stmt.methodRef.getDecl()).isStatic) {
                // static method call
                patchList.add(new PatchLine(Machine.nextInstrAddr(), stmt.methodRef.getDecl()));
                Machine.emit(Machine.Op.CALL, Machine.Reg.CB, -1);
            } else {
                // instance method call
                Machine.emit(Machine.Op.LOAD, Machine.Reg.LB, 0);
                patchList.add(new PatchLine(Machine.nextInstrAddr(), stmt.methodRef.getDecl()));
                Machine.emit(Machine.Op.CALLI, Machine.Reg.CB, -1);
            }
        } else {
            //  a.foo()
            stmt.methodRef.visit(this, 1);
            patchList.add(new PatchLine(Machine.nextInstrAddr(), stmt.methodRef.getDecl()));
            Machine.emit(Machine.Op.CALLI, Machine.Reg.CB, -1);
        }
        
        if (stmt.methodRef.getDecl().type.typeKind != TypeKind.VOID) {
        	Machine.emit(Machine.Op.POP, 0, 0, 1);
        }
        return null;
    }

    @Override
    public Integer visitReturnStmt(ReturnStmt stmt, Integer arg) {
        if (stmt.returnExpr != null) stmt.returnExpr.visit(this, 1);
        return null;
    }

    @Override
    public Integer visitIfStmt(IfStmt stmt, Integer arg) {
        stmt.cond.visit(this, 1);
        int patchAddrIf = Machine.nextInstrAddr();
        Machine.emit(Machine.Op.JUMPIF, 0, Machine.Reg.CB, -1);

        stmt.thenStmt.visit(this, arg);
        int patchAddrThen = Machine.nextInstrAddr();
        Machine.emit(JUMP, Machine.Reg.CB, -1);

        int patchAddrElse = Machine.nextInstrAddr();
        Machine.patch(patchAddrIf, patchAddrElse);
        if (stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, arg);
        }

        int patchAddrEnd = Machine.nextInstrAddr();
        Machine.patch(patchAddrThen, patchAddrEnd);
        return null;
    }

    @Override
    public Integer visitWhileStmt(WhileStmt stmt, Integer arg) {
        int patchAddrWhile = Machine.nextInstrAddr();
        stmt.cond.visit(this, 1);
        int patchAddrAfterCond = Machine.nextInstrAddr();
        Machine.emit(Machine.Op.JUMPIF, 0, Machine.Reg.CB, -1);

        stmt.body.visit(this, arg);
        Machine.emit(JUMP, Machine.Reg.CB, patchAddrWhile);

        int patchAddrEnd = Machine.nextInstrAddr();
        Machine.patch(patchAddrAfterCond, patchAddrEnd);
        return null;
    }

    @Override
    public Integer visitUnaryExpr(UnaryExpr expr, Integer arg) {
        expr.expr.visit(this, 1);
        if (expr.operator != null) {
            if (expr.operator.kind == TokenKind.MINUS) {
                Machine.emit(Machine.Prim.neg);
            } else if (expr.operator.kind == TokenKind.NOT) {
                Machine.emit(Machine.Op.LOADL, 0);
                Machine.emit(Machine.Prim.eq);
            } else {
                errorReporter.reportError("CodeGen: unrecognized unary operator at line " + expr.operator.posn);
            }
        }
        return null;
    }

    @Override
    public Integer visitBinaryExpr(BinaryExpr expr, Integer arg) {
        if (expr.operator.kind == TokenKind.AND) {
            expr.left.visit(this, 1);

            int patchAddrAnd = Machine.nextInstrAddr();
            Machine.emit(Machine.Op.JUMPIF, 0, Machine.Reg.CB, -1);

            Machine.emit(Machine.Op.LOADL, 1);
            expr.right.visit(this, 1);
            Machine.emit(Machine.Prim.and);
            int patchAddrAndEnd = Machine.nextInstrAddr();
            Machine.emit(JUMP, Machine.Reg.CB, -1);

            Machine.patch(patchAddrAnd, Machine.nextInstrAddr());
            Machine.emit(Machine.Op.LOADL, 0);
            Machine.patch(patchAddrAndEnd, Machine.nextInstrAddr());
            return null;
        } else if (expr.operator.kind == TokenKind.OR) {
            expr.left.visit(this, 1);

            int patchAddrOr = Machine.nextInstrAddr();
            Machine.emit(Machine.Op.JUMPIF, 1 , Machine.Reg.CB, -1);

            Machine.emit(Machine.Op.LOADL, 0);
            expr.right.visit(this, 1);
            Machine.emit(Machine.Prim.or);
            int patchAddrOrEnd = Machine.nextInstrAddr();
            Machine.emit(JUMP, Machine.Reg.CB, -1);

            Machine.patch(patchAddrOr, Machine.nextInstrAddr());
            Machine.emit(LOADL, 1);
            Machine.patch(patchAddrOrEnd, Machine.nextInstrAddr());
            return null;
        } else {
            expr.left.visit(this, 1);
            expr.right.visit(this, 1);
            switch (expr.operator.kind) {
                case PLUS:
                    Machine.emit(Machine.Prim.add);
                    break;
                case DIVIDE:
                    Machine.emit(Machine.Prim.div);
                    break;
                case EQUAL:
                    Machine.emit(Machine.Prim.eq);
                    break;
                case GREATER:
                    Machine.emit(Machine.Prim.gt);
                    break;
                case GREATER_EQUAL:
                    Machine.emit(Machine.Prim.ge);
                    break;
                case LESS:
                    Machine.emit(Machine.Prim.lt);
                    break;
                case LESS_EQUAL:
                    Machine.emit(Machine.Prim.le);
                    break;
                case MINUS:
                    Machine.emit(Machine.Prim.sub);
                    break;
                case NOT_EQUAL:
                    Machine.emit(Machine.Prim.ne);
                    break;
                case TIMES:
                    Machine.emit(Machine.Prim.mult);
                    break;
                default:
                    errorReporter.reportError("Binary expr operator" + expr.operator.spelling + " at line:" + expr.posn + " not recognized");
                    break;
            }
        }
        return null;
    }

    @Override
    public Integer visitRefExpr(RefExpr expr, Integer arg) {
        expr.ref.visit(this, arg);
        return null;
    }

    @Override
    public Integer visitCallExpr(CallExpr expr, Integer arg) {
        for (Expression argument : expr.argList) {
            argument.visit(this, 1);
        }

        if (expr.functionRef.getDecl().id.spelling.equals("println")) {
            Machine.emit(Machine.Prim.putintnl);
        } else if (expr.functionRef instanceof IdRef) {
            if (((MethodDecl) expr.functionRef.getDecl()).isStatic) {
                patchList.add(new PatchLine(Machine.nextInstrAddr(), expr.functionRef.getDecl()));
                Machine.emit(CALL, Machine.Reg.CB, -1);
            } else {
                Machine.emit(LOADA, Machine.Reg.OB, 0);
                patchList.add(new PatchLine(Machine.nextInstrAddr(), expr.functionRef.getDecl()));
                Machine.emit(CALLI, Machine.Reg.CB, -1);
            }
        } else {
            expr.functionRef.visit(this, 1);

            patchList.add(new PatchLine(Machine.nextInstrAddr(), expr.functionRef.getDecl()));
            Machine.emit(CALLI, Machine.Reg.CB, -1);
        }
        return null;
    }

    @Override
    public Integer visitLiteralExpr(LiteralExpr expr, Integer arg) {
        int val = -1;
        switch (expr.lit.kind) {
            case NUM:
                val = Integer.parseInt(expr.lit.spelling);
                break;
            case TRUE:
                val = 1;
                break;
            case FALSE:
                val = 0;
                break;
            case NULL:
                val = 0;
                break;
            default:
                errorReporter.reportError("Literal expression " + expr.lit.spelling + " at line " + expr.posn.start + " not recognizable");
        }
        Machine.emit(LOADL, val);
        return null;
    }

    @Override
    public Integer visitNewObjectExpr(NewObjectExpr expr, Integer arg) {
        Machine.emit(LOADL, -1);
        Machine.emit(LOADL, ((ClassDecl) expr.classtype.getDecl()).runtimeEntity.size);
        Machine.emit(Machine.Prim.newobj);
        return null;
    }

    @Override
    public Integer visitNewArrayExpr(NewArrayExpr expr, Integer arg) {
        expr.sizeExpr.visit(this, 1);
        Machine.emit(Machine.Prim.newarr);
        return null;
    }

    @Override
    public Integer visitThisRef(ThisRef ref, Integer arg) {
        Machine.emit(LOADA, Machine.Reg.OB, 0);
        return null;
    }

    @Override
    public Integer visitIdRef(IdRef ref, Integer arg) {
        RuntimeAddress addr = ((Address) ref.getDecl().runtimeEntity).address;
        if (arg == 1) {
            if (ref.getDecl() instanceof FieldDecl && ((FieldDecl) ref.getDecl()).isStatic) {
                Machine.emit(LOAD, SB, addr.offset);
            } else if (ref.getDecl() instanceof FieldDecl) {
                Machine.emit(LOAD, OB, addr.offset);
            } else {
                Machine.emit(LOAD, LB, addr.offset);
            }
        } else {
            return addr.offset;
        }
        return null;
    }

    @Override
    public Integer visitIxIdRef(IxIdRef ref, Integer arg) {
        RuntimeAddress addr = ((Address) ref.getDecl().runtimeEntity).address;
        if (arg == 1) {
            ref.id.visit(this, 1);
            ref.indexExpr.visit(this, 1);
            Machine.emit(Machine.Prim.arrayref);
        } else {
            ref.id.visit(this, 1);
            ref.indexExpr.visit(this, 1);
        }
        return null;
    }

    @Override
    public Integer visitQRef(QRef ref, Integer arg) {
        if (arg == 1) {
            if (ref.id.decl instanceof FieldDecl && ((FieldDecl) ref.id.decl).id.spelling.equals("length")) {
                ref.ref.visit(this, 1);
                Machine.emit(Machine.Op.LOADL, -1);
                Machine.emit(Machine.Prim.add);
                Machine.emit(Machine.Op.LOADI);
            } else if (ref.id.decl instanceof FieldDecl && ((FieldDecl) ref.id.decl).isStatic) {
                Machine.emit(Machine.Op.LOAD, Machine.Reg.SB, ((Address) ref.id.decl.runtimeEntity).address.offset);
            } else if (ref.id.decl instanceof FieldDecl) {
                //fetch a.x
                ref.ref.visit(this, 1);
                Machine.emit(Machine.Op.LOADL, ((Address) ref.id.decl.runtimeEntity).address.offset);
                Machine.emit(Machine.Prim.fieldref);
            } else if (ref.id.decl instanceof MethodDecl) { //methodDecl a.method()
                ref.ref.visit(this, 1);
            } else {
                errorReporter.reportError("unrecognized fetch type in QRef at" + ref.posn.start);
            }
        } else {
            //store static A.staticVar = 4
            if (ref.id.decl instanceof FieldDecl && ((FieldDecl) ref.id.decl).id.spelling.equals("length")) {
                errorReporter.reportError("Invalid: Cannot asign to length field of array at" + ref.posn.start);
            } else if (ref.id.decl instanceof FieldDecl && ((FieldDecl) ref.id.decl).isStatic) {
                RuntimeAddress addr = ((Address) ref.id.decl.runtimeEntity).address;
                return addr.offset;
            } else {
                //store nonstatic a.var = 4
                ref.ref.visit(this, 1);
                RuntimeAddress addr = ((Address) ref.id.decl.runtimeEntity).address;
                Machine.emit(Machine.Op.LOADL, addr.offset);
                return addr.offset;
            }

        }
        return null;
    }

    @Override
    public Integer visitIxQRef(IxQRef ref, Integer arg) {
        if (arg == 1) {
            if (ref.id.decl instanceof FieldDecl && ((FieldDecl) ref.id.decl).isStatic) {
                Machine.emit(Machine.Op.LOAD, Machine.Reg.SB, ((Address) ref.id.decl.runtimeEntity).address.offset);
                ref.ixExpr.visit(this, 1);
                Machine.emit(Machine.Prim.arrayref);
            } else if (ref.id.decl instanceof FieldDecl) {
                //fetch a.x
                ref.ref.visit(this, 1);
                Machine.emit(Machine.Op.LOADL, ((Address) ref.id.decl.runtimeEntity).address.offset);
                Machine.emit(Machine.Prim.fieldref);
                ref.ixExpr.visit(this, 1);
                Machine.emit(Machine.Prim.arrayref);}
//            } else if (ref.id.decl instanceof MethodDecl) { //methodDecl a.method()
//                ref.ref.visit(this, 1);
//            }
                else {
                errorReporter.reportError("unrecognized fetch type in IxQRef at" + ref.posn.start);
            }
        } else {
            //store static A.staticVar = 4
            if (ref.id.decl instanceof FieldDecl && ((FieldDecl) ref.id.decl).isStatic) {
                ref.ixExpr.visit(this, 1);
                RuntimeAddress addr = ((Address) ref.id.decl.runtimeEntity).address;
                return addr.offset;
            } else {
                //store nonstatic a.var = 4
                ref.ref.visit(this, 1);

                RuntimeAddress addr = ((Address) ref.id.decl.runtimeEntity).address;
                Machine.emit(Machine.Op.LOADL, addr.offset);
                Machine.emit(Machine.Prim.fieldref);
                ref.ixExpr.visit(this, 1);
                return addr.offset;
            }

        }
        return null;
    }

    @Override
    public Integer visitIdentifier(Identifier id, Integer arg) {
        RuntimeAddress addr = ((Address) id.decl.runtimeEntity).address;
        if (arg == 1) {
            if (id.decl instanceof FieldDecl && ((FieldDecl) id.decl).isStatic) {
                Machine.emit(LOAD, SB, addr.offset);
            } else if (id.decl instanceof FieldDecl) {
                Machine.emit(LOAD, OB, addr.offset);
            } else {
                Machine.emit(LOAD, LB, addr.offset);
            }
        } else {
            return addr.offset;
        }
        return null;
    }

    @Override
    public Integer visitOperator(Operator op, Integer arg) {
        return null;
    }

    @Override
    public Integer visitIntLiteral(IntLiteral num, Integer arg) {
        return null;
    }

    @Override
    public Integer visitNullLiteral(NullLiteral nullLiteral, Integer arg) {
        return null;
    }

    @Override
    public Integer visitBooleanLiteral(BooleanLiteral bool, Integer arg) {
        return null;
    }

    @Override
    public Integer visitArrayLengthRef(ArrayLengthRef arrayLengthRef, Integer arg) {
        return null;
    }
}
