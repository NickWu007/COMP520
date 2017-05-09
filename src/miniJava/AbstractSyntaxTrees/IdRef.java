/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGenerator.RuntimeEntity;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class IdRef extends BaseRef {
	
	public IdRef(Identifier id, SourcePosition posn){
		super(posn);
		this.id = id;
	}
		
	public <A,R> R visit(Visitor<A,R> v, A o) {
		return v.visitIdRef(this, o);
	}

	public Identifier id;

	@Override
	public TypeDenoter getType() {
		return id.decl.type;
	}

	@Override
	public Declaration getDecl() {
		return id.decl;
	}

	@Override
	public RuntimeEntity getDeclRTE() {
		return id.decl.runtimeEntity;
	}
}
