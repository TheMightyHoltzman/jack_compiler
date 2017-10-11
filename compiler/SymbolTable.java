package compiler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SymbolTable {
	Map<String, SymbolTableEntry> classLvl;
	Map<String, SymbolTableEntry> subroutineLvl;
	Map<String, Integer> subroutineInfo;
	
	// these have subroutine scopes
	public static final String KIND_ARG    = "argument"; // these are the things handed to the function
	public static final String KIND_VAR    = "local"; // these are the variables declared at the beginning of the function body
	// these have class scopes
	public static final String KIND_FIELD  = "field"; 
	public static final String KIND_STATIC = "static";	
	
	public SymbolTable() {
		super();
		this.classLvl = new HashMap<>();
		this.subroutineLvl = new HashMap<>();
		//this.subroutineInfo = subroutineInfo;
	}

	public void resetSubroutineScope() {
		subroutineLvl.clear();
	}
	
	/**
	 * 
	 * @param name - the identifier of the method/function 
	 * @param type - the type
	 * @param kind 
	 */
	public void define(String name, String type, String kind) {
		getTable(kind).put(name, new SymbolTableEntry(name, type, kind, varCount(kind)));
	}
	
	public int varCount(String kind) {
		int nr = 0;
		for (SymbolTableEntry entry : getTable(kind).values()) {
			if (entry.scope.equals(kind)) {
				nr++;
			}
		}
		return nr;
	}
	
	public String kindOf(String name) {
		if (subroutineLvl.get(name) != null) {
			return subroutineLvl.get(name).scope;
		}
		else {
			if (classLvl.get(name) != null) {
				return classLvl.get(name).scope;
			}
			return null;
		}
	}
	
	public String typeOf(String name) {
		if (subroutineLvl.get(name) != null) {
			return subroutineLvl.get(name).type;
		}
		else {
			if (classLvl.get(name) != null) {
				return classLvl.get(name).type;
			}
			return null;
		}
	}
	
	private Map<String, SymbolTableEntry> getTable(String kind) {
		if (kind.equals(KIND_ARG) || kind.equals(KIND_VAR)) {
			return subroutineLvl;
		}
		else {
			return classLvl;
		}
	}
	
	public SymbolTableEntry retrieve(String name) {
		// first look in function-scope:
		SymbolTableEntry entry;
		if (subroutineLvl.containsKey(name)) {
			 entry = subroutineLvl.get(name);
		}
		else {
			entry = classLvl.get(name);
		}
		return entry;
	}
	
	public String retrieveForVM(String name) {
		SymbolTableEntry entry = retrieve(name);
		return (entry.scope + " " + entry.index);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CLASS-LEVEL\n");
		for (Entry<String, SymbolTableEntry> entry : classLvl.entrySet()) {
			sb.append(entry.getKey());
			sb.append("\t: ");
			sb.append(entry.getValue().toString());
			sb.append("\n");
		}
		sb.append("\nSUBROUTINE-LEVEL\n");
		for (Entry<String, SymbolTableEntry> entry : subroutineLvl.entrySet()) {
			sb.append(entry.getKey());
			sb.append("\t: ");
			sb.append(entry.getValue().toString());
			sb.append("\n");
		}
		sb.append("\n");
		return sb.toString();
	}
	
	public void print() {
		System.out.println(toString());
	}
}
