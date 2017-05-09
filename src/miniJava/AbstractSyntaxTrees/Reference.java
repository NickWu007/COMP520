/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGenerator.RuntimeEntity;
import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Reference extends AST
{
	public Reference(SourcePosition posn){
		super(posn);
	}

	//pa3 added for get type of Reference;
	public abstract TypeDenoter getType();

	public abstract Declaration getDecl();

	public abstract RuntimeEntity getDeclRTE();

	public boolean isStatic = false;

}
