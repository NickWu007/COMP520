package miniJava.SyntacticAnalyzer;

/**
 * Created by NickWu on 2017/2/12.
 */
public class SourcePosition {

    public int start, finish;

    public SourcePosition () {
        start = 0;
        finish = 0;
    }

    public SourcePosition (int s, int f) {
        start = s;
        finish = f;
    }

    public String toString() {
        return "(" + start + ", " + finish + ")";
    }
}
