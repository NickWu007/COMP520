/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class BaseType extends TypeDenoter
{
    public BaseType(TypeKind t, SourcePosition posn){
        super(t, posn);
    }
    
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitBaseType(this, o);
    }

	@Override
	public boolean equals(Object obj) {
        if (this.typeKind == TypeKind.ANY) {
            return true;
        }

        if (this.typeKind == TypeKind.UNSUPPORTED || this.typeKind == TypeKind.ERROR) {
            return false;
        }
        if (obj == null || ((TypeDenoter)obj).typeKind == TypeKind.UNSUPPORTED){
            return false;
        }
        else if (((TypeDenoter)obj).typeKind == TypeKind.ANY || ((TypeDenoter)obj).typeKind == TypeKind.ERROR){
            return true;
        }
        else if (obj instanceof BaseType && this.typeKind == ((BaseType)obj).typeKind){
            return true;
        }
        else{
            return false;
        }
	}

    @Override
    public Declaration getDecl() {
        return null;
    }
}
