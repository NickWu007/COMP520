package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGenerator.RuntimeEntity;
import miniJava.SyntacticAnalyzer.SourcePosition;

/**
 * Created by NickWu on 2017/4/2.
 */
public class ArrayLengthRef extends Reference{

    public Reference arrayRef;
    public int length;

    public ArrayLengthRef(Reference arrayRef, SourcePosition position) {
        super(position);
        this.arrayRef = arrayRef;
        this.length = 0;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitArrayLengthRef(this, o);
    }

    @Override
    public TypeDenoter getType() {
        return arrayRef.getType();
    }

    @Override
    public Declaration getDecl() {
        return arrayRef.getDecl();
    }

    @Override
    public RuntimeEntity getDeclRTE() {
        return null;
    }
}
