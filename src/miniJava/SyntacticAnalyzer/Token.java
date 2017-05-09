package miniJava.SyntacticAnalyzer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *  A token has a kind and a spelling
 *  In a full compiler it would also have a source position 
 */
public class Token {
	public TokenKind kind;
	public String spelling;
	public SourcePosition posn;

	public Token(TokenKind kind, String spelling, SourcePosition posn) {
		this.kind = kind;
		this.spelling = spelling;
		this.posn = posn;
		
		if (kind == TokenKind.IDENTIFIER) {
			for (String key : keywords.keySet()) {
				if (key.equals(spelling)) {
					this.kind = keywords.get(key);
				}
			}
		}
	}

	// 15 keywords
	public static Map<String, TokenKind> keywords;
	static {
		keywords = new HashMap<String, TokenKind>();
		keywords.put("class", TokenKind.CLASS);
		keywords.put("void", TokenKind.VOID);
		keywords.put("public", TokenKind.PUBLIC);
		keywords.put("private", TokenKind.PRIVATE);
		keywords.put("static", TokenKind.STATIC);
		keywords.put("boolean", TokenKind.BOOL);
		keywords.put("int", TokenKind.INT);
		keywords.put("this", TokenKind.THIS);
		keywords.put("return", TokenKind.RETURN);
		keywords.put("if", TokenKind.IF);
		keywords.put("else", TokenKind.ELSE);
		keywords.put("while", TokenKind.WHILE);
		keywords.put("true", TokenKind.TRUE);
		keywords.put("false", TokenKind.FALSE);
		keywords.put("new", TokenKind.NEW);
		keywords.put("null", TokenKind.NULL);
		keywords = Collections.unmodifiableMap(keywords);
	}
	
}