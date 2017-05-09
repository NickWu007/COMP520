/**
 * miniJava Abstract Syntax Tree classes
 *
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class ClassDecl extends Declaration {

    public ClassDecl(Identifier id, FieldDeclList fdl, MethodDeclList mdl, SourcePosition posn) {
        super(id, new ClassType(new Identifier(new Token(TokenKind.IDENTIFIER, id.spelling, posn)), posn), posn);
        fieldDeclList = fdl;
        methodDeclList = mdl;
    }

    // PA3 added
    public boolean existsMember(String name, boolean expectStatic, boolean expectPublic) {
        for (FieldDecl fieldDecl : fieldDeclList) {
            if (name.equals(fieldDecl.id.spelling)) {
                if (expectStatic && !fieldDecl.isStatic) continue;
                if (expectPublic && fieldDecl.isPrivate) continue;
                return true;
            }
        }

        for (MethodDecl methodDecl : methodDeclList) {
            if (name.equals(methodDecl.id.spelling)) {
                if (expectStatic && !methodDecl.isStatic) continue;
                if (expectPublic && methodDecl.isPrivate) continue;
                return true;
            }
        }
        return false;
    }

    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitClassDecl(this, o);
    }

    public FieldDeclList fieldDeclList;
    public MethodDeclList methodDeclList;
}
