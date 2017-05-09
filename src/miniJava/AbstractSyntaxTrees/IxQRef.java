package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGenerator.RuntimeEntity;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class IxQRef extends QualifiedRef {
	
	public IxQRef(Reference ref, Identifier id, Expression exp, SourcePosition posn){
		super(posn);
		this.ref = ref;
		this.id  = id;
		this.ixExpr = exp;
	}

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitIxQRef(this, o);
	}

	public Reference ref;
	public Identifier id;
	public Expression ixExpr;

	@Override
	public TypeDenoter getType() {
		TypeDenoter type = id.decl.type;
		if (type instanceof ArrayType) {
			return ((ArrayType)type).eltType;
		} else {
			return null;
		}
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
