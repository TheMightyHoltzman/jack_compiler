package compiler;

public class Token {
	public String type;
	public String value;
	public int line;
	
	public Token(String type, String value, int line) {
		super();
		this.type = type;
		this.value = value;
		this.line = line;
	}
	
	public static Token make(String type, String value, int line) {
		return new Token(type, value, line);
	}
	
	public String toString() {
		return type + ": " + value;
	}
	
	public void print() {
		System.out.println(toString());
	}
}
