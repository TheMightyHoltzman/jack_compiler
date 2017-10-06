package Compiler;

public class SymbolTableEntry {
	public String name;
	public String type;
	public String scope;
	public int index;
	
	public SymbolTableEntry(String name, String type, String kind, int index) {
		super();
		this.name = name;
		this.type = type;
		this.scope = kind;
		this.index = index;
	}
	
	@Override
	public String toString() {
		return "name: " + name + ", type: " + type + ", scope: " + scope + ", index: " + index; 
	}
}
