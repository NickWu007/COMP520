/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class Identifier extends Terminal {

  public Identifier (Token t) {
    super (t);
    decl = null;
    type = null;
  }

  public <A,R> R visit(Visitor<A,R> v, A o) {
      return v.visitIdentifier(this, o);
  }

  public Declaration decl;
  public TypeDenoter type;

  public boolean isStatic;

}
