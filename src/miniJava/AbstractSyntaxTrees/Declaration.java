/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Declaration extends AST {

	// Changed first argument from String to Identifier
	public Declaration(Identifier id, TypeDenoter type, SourcePosition posn) {
		super(posn);
		this.id = id;
		this.type = type;
	}

	// PA3 added
	public void setIdBinding() {
		id.decl = this;
		id.type = this.type;
		id.runtimeEntity = this.runtimeEntity;
	}

	public Identifier id;
	public TypeDenoter type;
}
