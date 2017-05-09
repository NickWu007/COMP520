/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class MethodDecl extends MemberDecl {
	
	public MethodDecl(MemberDecl md, ParameterDeclList pl, StatementList sl, SourcePosition posn){
    super(md,posn);
    parameterDeclList = pl;
    statementList = sl;
	}
	
	public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitMethodDecl(this, o);
    }
	
	public ParameterDeclList parameterDeclList;
	public StatementList statementList;
	// PA3 added
    public TypeDenoter returnType;
    
    // PA3 added
    public boolean isMain() {
    	if (this.isPrivate) return false;
    	if (!this.isStatic) return false;
    	if (this.type.typeKind != TypeKind.VOID) return false;
    	if (!this.id.spelling.equals("main")) return false;
    	if (this.parameterDeclList.size() != 1) return false;
    	if (!(this.parameterDeclList.get(0).type instanceof ArrayType)) return false;
    	ArrayType arrayType = (ArrayType) this.parameterDeclList.get(0).type;
    	if (arrayType.eltType instanceof ClassType) {
    		ClassType type = (ClassType) arrayType.eltType;
    		return type.typeKind == TypeKind.UNSUPPORTED || type.className.spelling.equals("String");
    	}
    	return false;
    }
}
