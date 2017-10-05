
public class Token {
	public String type;
	public String value;
	
	public Token(String type, String value) {
		super();
		this.type = type;
		this.value = value;
	}
	
	public static Token make(String type, String value) {
		return new Token(type, value);
	}
	
	public String toString() {
		return type + ": " + value;
	}
	
	public void print() {
		System.out.println(toString());
	}
}
