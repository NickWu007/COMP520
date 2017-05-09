/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGenerator.RuntimeEntity;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class QRef extends QualifiedRef {
	
	public QRef(Reference ref, Identifier id, SourcePosition posn){
		super(posn);
		this.ref = ref;
		this.id  = id;
	}

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitQRef(this, o);
	}

	public Reference ref;
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
		return id.runtimeEntity;
	}
}
