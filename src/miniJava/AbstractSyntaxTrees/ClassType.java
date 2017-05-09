/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassType extends TypeDenoter
{
    public ClassType(Identifier cn, SourcePosition posn){
        super(TypeKind.CLASS, posn);
        className = cn;
    }
            
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitClassType(this, o);
    }

    public Identifier className;

	@Override
	public boolean equals(Object obj) {
        if (obj == null || ((TypeDenoter)obj).typeKind == TypeKind.UNSUPPORTED){
            return false;
        }
        else if (((TypeDenoter)obj).typeKind == TypeKind.ANY || ((TypeDenoter)obj).typeKind == TypeKind.ERROR){
            return true;
        }
        else if (obj instanceof ClassType){
            ClassType type = (ClassType) obj;
            return type.className.spelling.equals(this.className.spelling);
        }
        else{
            return false;
        }
	}

    public ClassDecl getDecl() {
	    Declaration declaration = className.decl;
	    if (declaration instanceof ClassDecl) {
	        return (ClassDecl) declaration;
        } else {
	        return null;
        }
    }
}
