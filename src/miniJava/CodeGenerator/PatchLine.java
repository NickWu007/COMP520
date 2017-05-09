package miniJava.CodeGenerator;

import miniJava.AbstractSyntaxTrees.Declaration;

/**
 * Created by NickWu on 2017/4/3.
 */
public class PatchLine {

    public int line;
    public Declaration declaration;

    public PatchLine(int line, Declaration declaration) {
        this.line = line;
        this.declaration = declaration;
    }
}
