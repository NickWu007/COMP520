/**
 *   TokenKind is a simple enumeration of the different kinds of tokens
 *   
 */
package miniJava.SyntacticAnalyzer;

// 41 kinds.
public enum TokenKind {
	NUM, IDENTIFIER,
	PLUS, TIMES, MINUS, DIVIDE,
	AND, OR, NOT,
	LESS, GREATER, LESS_EQUAL, GREATER_EQUAL, ASSIGN, EQUAL, NOT_EQUAL,
	LPAREN, RPAREN, LBRACKET, RBRACKET, LCBRACKET, RCBRACKET,
	CLASS, VOID, PUBLIC, PRIVATE, STATIC, 
	INT, BOOL, THIS, RETURN, IF, ELSE, WHILE, 
	SIMICOLON, TRUE, FALSE, NEW, COMMA, DOT,
	EOT, ERROR, COMMENT,
	NULL
}
