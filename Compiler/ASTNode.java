package Compiler;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ASTNode {
	public String type;
	public String value;
	public ASTNode parent;
	public List<ASTNode> children;
	
	public ASTNode(String type, String value) {
		super();
		this.type     = type;
		this.value    = value;
		this.children = new LinkedList<>();
	}

	public ASTNode addChild(String type, String value) {
		ASTNode node = new ASTNode(type, value);
		this.children.add(node);
		this.parent = this;
		return node;
	}
	
	public ASTNode addChild(ASTNode node) {
		this.children.add(node);
		this.parent = this;
		return node;
	}
	
	public ASTNode popLastChild() {
		ASTNode last = this.children.get(this.children.size() - 1);
		this.children.remove(this.children.size() - 1);
		return last;
	}
	
	public String toString() {
		String str = tkn(this.type, true);
		if (this.value != null) {
			str += escapeValue(this.value);
		}
		Iterator<ASTNode> iterator = this.children.iterator();
		while (iterator.hasNext()) {
			str += iterator.next().toString();
		}
		str += tkn(this.type, false);
		return str;
	}
	
	public String tkn(String str, boolean start) {
		if (start) {
			return "<" + str + ">";
		}
		return "</" + str + ">";
	}
	
	private static String escapeValue(String str) {
		return str.replaceAll("&", "&amp;")
				.replaceAll("<", "&lt;")
				.replaceAll(">", "&gt;");
	}
}
