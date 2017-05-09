package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

/**
 * Created by NickWu on 2017/3/13.
 */
public class NullLiteral extends Terminal{

    public NullLiteral(Token t) {
        super(t);
    }

    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitNullLiteral(this, o);
    }
}
