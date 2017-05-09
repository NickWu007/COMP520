/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */

package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ArrayType extends TypeDenoter {

	    public ArrayType(TypeDenoter eltType, SourcePosition posn){
	        super(TypeKind.ARRAY, posn);
	        this.eltType = eltType;
	    }
	        
	    public <A,R> R visit(Visitor<A,R> v, A o) {
	        return v.visitArrayType(this, o);
	    }

	    public TypeDenoter eltType;

	    // PA4 added
		public int length;

		@Override
		public boolean equals(Object obj){
			if (obj == null || ((TypeDenoter)obj).typeKind == TypeKind.UNSUPPORTED){
				return false;
			}
			else if (((TypeDenoter)obj).typeKind == TypeKind.ANY || ((TypeDenoter)obj).typeKind == TypeKind.ERROR){
				return true;
			}
			else if (obj instanceof ArrayType){
				TypeDenoter elt = ((ArrayType) obj).eltType;
				if(elt instanceof BaseType){
					return elt.equals(this.eltType);
				}
				else if (elt instanceof ClassType){
					return elt.equals(this.eltType);
				}
				else{
					System.out.println("*** Incorrect Array type");
					return false;
				}
			}
			else{
				return false;
			}
		}

	@Override
	public Declaration getDecl() {
		return eltType.getDecl();
	}
}


