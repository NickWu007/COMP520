/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class LocalDecl extends Declaration {
	
	public LocalDecl(Identifier id, TypeDenoter t, SourcePosition posn){
		super(id,t,posn);
	}

}
