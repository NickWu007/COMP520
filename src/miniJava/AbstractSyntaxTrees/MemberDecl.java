/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class MemberDecl extends Declaration {

    public MemberDecl(boolean isPrivate, boolean isStatic, TypeDenoter mt, Identifier id, SourcePosition posn) {
        super(id, mt, posn);
        this.isPrivate = isPrivate;
        this.isStatic = isStatic;
    }
    
    public MemberDecl(MemberDecl md, SourcePosition posn){
    	super(md.id, md.type, posn);
    	this.isPrivate = md.isPrivate;
    	this.isStatic = md.isStatic;
    }
    
    public boolean isPrivate;
    public boolean isStatic;

    // PA3 added
    public ClassDecl classDecl;

    // PA4 added
    public int classIndex;
}
