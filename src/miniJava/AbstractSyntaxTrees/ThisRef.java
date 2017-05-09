/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGenerator.RuntimeEntity;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class ThisRef extends BaseRef {

	public Declaration declaration;
	
	public ThisRef(SourcePosition posn) {
		super(posn);
	}

	@Override
	public TypeDenoter getType() {
		return declaration.type;
	}

	@Override
	public Declaration getDecl() {
		return declaration;
	}

	@Override
	public RuntimeEntity getDeclRTE() {
		return declaration.runtimeEntity;
	}

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitThisRef(this, o);
	}
	
}
