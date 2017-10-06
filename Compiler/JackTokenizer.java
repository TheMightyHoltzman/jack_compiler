package Compiler;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JackTokenizer {
	public static final int MODE_IGNORE_API_COMMENTS = 1;
	public static final int MODE_IGNORE_COMMENTS     = 2;
	public static final int MODE_PARSE_STRING        = 3; 
	public static final int MODE_PARSE_NORMAL        = 4;
	
	// all keywords
	public static final String KEYWORD_CLASS = "class";
	public static final String KEYWORD_METHOD   = "method";
	public static final String KEYWORD_FUNCTION = "function";
	public static final String KEYWORD_CONSTRUCTOR = "constructor";
	public static final String KEYWORD_INT = "int";
	public static final String KEYWORD_BOOLEAN = "boolean";
	public static final String KEYWORD_CHAR = "char";
	public static final String KEYWORD_VOID = "void";
	public static final String KEYWORD_VAR = "var";
	public static final String KEYWORD_STATIC = "static";
	public static final String KEYWORD_FIELD = "field";
	public static final String KEYWORD_LET = "let";
	public static final String KEYWORD_DO = "do";
	public static final String KEYWORD_IF = "if";
	public static final String KEYWORD_ELSE = "else";
	public static final String KEYWORD_WHILE = "while";
	public static final String KEYWORD_RETURN = "return";
	public static final String KEYWORD_TRUE = "true";
	public static final String KEYWORD_FALSE = "false";
	public static final String KEYWORD_NULL = "null";
	public static final String KEYWORD_THIS = "this";
	
	public static List<String> keywords;
	public static List<String> symbols;
	
	public List<Token> tokens;
	public String current;
	
	public int currentInt;
	public int previousInt;
	public int prePreviousInt;
	
	public int mode;
	
	public JackTokenizer() {
		super();
		init();
	}

	public List<Token> work(FileReader reader) throws IOException {
		current = "";
		tokens = new LinkedList<>();
		currentInt = reader.read();;
		while (currentInt != -1) {
			
			char currentChar     = (char)currentInt;
			char previousChar    = (char)previousInt; 
			char prePreviousChar = (char)prePreviousInt;
			
			if (mode == MODE_PARSE_NORMAL) {
				if (previousChar == '/' && currentChar == '/') {
					tokens.remove(tokens.size() - 1);
					mode = MODE_IGNORE_COMMENTS;
				}
				else if (prePreviousChar == '/' && previousChar == '*' && currentChar == '*') {
					tokens.remove(tokens.size() - 1);
					tokens.remove(tokens.size() - 1);
					mode = MODE_IGNORE_API_COMMENTS;
				}
				else if (currentChar == '"') {
					mode = MODE_PARSE_STRING;
				}
				else {
					process(currentChar);
				}
			}
			else if (mode == MODE_IGNORE_API_COMMENTS) {
				if (previousChar == '*' && currentChar == '/') {
					mode = MODE_PARSE_NORMAL;
				}
			}
			else if (mode == MODE_IGNORE_COMMENTS) {
				if (currentChar == '\n') {
					mode = MODE_PARSE_NORMAL;
				}
			}
			else if (mode == MODE_PARSE_STRING) {
				if (currentChar != '"') {
					current += currentChar;
				}
				else {
					tokens.add(Token.make("string", current));
					current = "";
					mode = MODE_PARSE_NORMAL;
				}
			}
			
			// advance to the next char
			prePreviousInt = previousInt;
			previousInt    = currentInt;
			currentInt     = reader.read();
		}
		return tokens;
	}
	
	public void process(char symbol) {
		if (symbol == ' ' || symbol == '\r' || symbol == '\n' || symbol == '\t' || isSymbol(symbol + "")) {
			if (current.length() != 0) {
				tokens.add(Token.make(getType(current), current));
				current = "";
			}
			if (isSymbol(symbol + "")) {
				tokens.add(Token.make("symbol", symbol + ""));
			}
		}
		else {
			current += symbol;
		}
	}
	
	public static boolean isKeyword(String token) {
		return keywords.contains(token);
	}
	
	public static boolean isInt(String token) {
		Pattern isNumber = Pattern.compile("-?[0-9]+");
		Matcher m = isNumber.matcher(token);
		return m.matches();
	}
	
	public static boolean isString(String token) {
		Pattern isString = Pattern.compile("\".*\"");
		Matcher m = isString.matcher(token);
		return m.matches();
	}
	
	public static boolean isSymbol(String token) {
		return symbols.contains(token);
	}
	
	public static boolean isIdentifier(String token) {
		Pattern isString = Pattern.compile("[a-zA-Z][0-9a-zA-Z_]*");
		Matcher m = isString.matcher(token);
		return m.matches();
	}
	
	public static String getType(String token) {
		if (isKeyword(token)) {
			return "keyword";
		}
		else if (isInt(token)) {
			return "int";
			
		}
		else if (isString(token)) {
			return "string";
			
		}
		else if (isSymbol(token)) {
			return "symbol";
		}
		else if (isIdentifier(token)) {
			return "identifier";
		}
		else {
			return "unknown";
			//throw new ParseException(token + " not acceptable!", 0);
		}
	}
	
	public void init() {
		keywords = new ArrayList<>();
		Collections.addAll(keywords, 
			 KEYWORD_CLASS,
			 KEYWORD_METHOD,
			 KEYWORD_FUNCTION,
			 KEYWORD_CONSTRUCTOR,
			 KEYWORD_INT,
			 KEYWORD_BOOLEAN,
			 KEYWORD_CHAR,
			 KEYWORD_VOID,
			 KEYWORD_VAR,
			 KEYWORD_STATIC,
			 KEYWORD_FIELD,
			 KEYWORD_LET,
			 KEYWORD_DO,
			 KEYWORD_IF,
			 KEYWORD_ELSE,
			 KEYWORD_WHILE,
			 KEYWORD_RETURN,
			 KEYWORD_TRUE,
			 KEYWORD_FALSE,
			 KEYWORD_NULL,
			 KEYWORD_THIS
		);
		
		symbols = new ArrayList<>();
		Collections.addAll(symbols, "{", "}", "(", ")", "[", "]", ";", ".", ",", "+", "-", "*", "/", "&", "|", "~", "<", ">", "=" );
		mode = MODE_PARSE_NORMAL;
	}
	
	public void print() {
		for (Token token : tokens) {
			token.print();
		}
	}
}
