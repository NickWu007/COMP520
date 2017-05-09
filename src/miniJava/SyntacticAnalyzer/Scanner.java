/**
 * Scan the first line of an input stream
 * <p>
 * Grammar:
 * num ::= digit digit*
 * digit ::= '0' | ... | '9'
 * oper ::= '+' | '*'
 * <p>
 * whitespace is the space character
 */
package miniJava.SyntacticAnalyzer;

import java.io.*;

import miniJava.ErrorReporter;

public class Scanner {

    private InputStream inputStream;
    private ErrorReporter reporter;

    private TokenKind currKind;
    private char currentChar;
    private StringBuilder currentSpelling;

    private boolean eot = false;
    private int currentLine;

    public Scanner(InputStream inputStream, ErrorReporter reporter) {
        this.inputStream = inputStream;
        this.reporter = reporter;
        this.currentLine = 1;

        readChar();
    }

    /**
     * skip whitespace and scan next token
     * @return token
     */
    public Token scan() {

        SourcePosition pos = new SourcePosition();
        pos.start = currentLine;
        pos.finish = currentLine;
        // skip whitespace
        while (!eot && (currentChar == ' ' || currentChar == '\n' || currentChar == '\t' || currentChar == '\r'))
            skipIt();

        // collect spelling and identify token kind
        currentSpelling = new StringBuilder();
        TokenKind kind = scanToken();

        if (kind == TokenKind.COMMENT) {
            return scan();
        }

        // return new token
        return new Token(kind, currentSpelling.toString(), pos);
    }


    public TokenKind scanToken() {

        if (eot)
            return (TokenKind.EOT);

        // scan Token
        switch (currentChar) {

            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '_':
                takeIt();
                while (isLetter(currentChar) || isDigit(currentChar) || currentChar == '_') {
                    takeIt();
                }

                return TokenKind.IDENTIFIER;

            case '&':
                takeIt();
                if (currentChar == '&') {
                    takeIt();
                    return TokenKind.AND;
                }
                return TokenKind.ERROR;

            case '|':
                takeIt();
                if (currentChar == '|') {
                    takeIt();
                    return TokenKind.OR;
                }
                return TokenKind.ERROR;

            case '!':
                takeIt();
                if (currentChar == '=') {
                    takeIt();
                    return TokenKind.NOT_EQUAL;
                }
                return TokenKind.NOT;

            case '>':
                takeIt();
                if (currentChar == '=') {
                    takeIt();
                    return TokenKind.GREATER_EQUAL;
                }
                return TokenKind.GREATER;

            case '<':
                takeIt();
                if (currentChar == '=') {
                    takeIt();
                    return TokenKind.LESS_EQUAL;
                }
                return TokenKind.LESS;

            case '=':
                takeIt();
                if (currentChar == '=') {
                    takeIt();
                    return TokenKind.EQUAL;
                }
                return TokenKind.ASSIGN;

            case '+':
                takeIt();
                return (TokenKind.PLUS);

            case '-':
                takeIt();
                if (currentChar == '-') {
                    scanError("Mini Java does not support \'--\'.");
                    return TokenKind.ERROR;
                }
                return (TokenKind.MINUS);

            case '*':
                takeIt();
                return (TokenKind.TIMES);

            case '/':
                takeIt();
                if (currentChar == '/') {
                    skipLineComment();
                    return TokenKind.COMMENT;
                }

                if (currentChar == '*') {
                    skipMultiLineComment();
                    return TokenKind.COMMENT;
                }
                return (TokenKind.DIVIDE);

            case '(':
                takeIt();
                return (TokenKind.LPAREN);

            case ')':
                takeIt();
                return (TokenKind.RPAREN);

            case '[':
                takeIt();
                return (TokenKind.LBRACKET);

            case ']':
                takeIt();
                return (TokenKind.RBRACKET);

            case '{':
                takeIt();
                return (TokenKind.LCBRACKET);

            case '}':
                takeIt();
                return (TokenKind.RCBRACKET);

            case ';':
                takeIt();
                return (TokenKind.SIMICOLON);

            case ',':
                takeIt();
                return (TokenKind.COMMA);

            case '.':
                takeIt();
                return (TokenKind.DOT);

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                while (isDigit(currentChar))
                    takeIt();
                return (TokenKind.NUM);

            default:
                scanError("Unrecognized character '" + currentChar + "' in input");
                return (TokenKind.ERROR);
        }
    }

    private void takeIt() {
        currentSpelling.append(currentChar);
        nextChar();
    }

    private void skipIt() {
        nextChar();
    }

    private void skipLineComment() {
        while (currentChar != '\n')  {
            if (eot) return;
            skipIt();
        }
        skipIt();
    }

    private void skipMultiLineComment() {
        skipIt();
        while (true) {
            if (eot) {
                scanError("MultiLine comment not terminated properly");
                return;
            }
            if (currentChar == '*') {
                skipIt();
                if (currentChar == '/') {
                    skipIt();
                    break;
                }
            } else skipIt();
        }
    }

    private boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isDigit(char c) {
        return (c >= '0') && (c <= '9');
    }

    private void scanError(String m) {
        reporter.reportError("Scan Error:  " + m);
    }


    private final static char eolUnix = '\n';
    private final static char eolWindows = '\r';

    /**
     * advance to next char in inputstream
     * detect end of file or end of line as end of input
     */
    private void nextChar() {
        if (!eot)
            readChar();
    }

    private void readChar() {
        try {
            int c = inputStream.read();
            currentChar = (char) c;
            if ((char)c == '\n') currentLine++;
            if (c == -1) {
                eot = true;
            }
        } catch (IOException e) {
            scanError("I/O Exception!");
            eot = true;
        }
    }

    public static void main(String[] args) throws IOException{
        ErrorReporter reporter = new ErrorReporter();
        FileInputStream in = new FileInputStream(new File("test.txt"));
        Scanner sc = new Scanner(in, reporter);

        System.out.println("Syntactic analysis ... ");

        Token tk = sc.scan();

        while (tk.kind != TokenKind.EOT) {
            System.out.println(tk.kind + ":     " + tk.spelling + "=======" + tk.posn.toString());
            tk = sc.scan();
        }

        System.out.print("Syntactic analysis complete:  ");
        if (reporter.hasErrors()) {
            System.out.println("INVALID program");
            System.exit(4);
        } else {
            System.out.println("valid program");
            System.exit(0);
        }
    }
}
