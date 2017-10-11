package compiler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class VMWriter {
	String className;
	SymbolTable symbolTable;
	AST syntaxTree;
	FileOutputStream fout;
	int lblCounter;
	
	private static final String CONSTANT = "constant"; 
	private static final String LOCAL    = "local";
	private static final String THIS     = "this";
	private static final String THAT     = "that";
	private static final String POINTER  = "pointer";
	private static final String STATIC   = "static";
	private static final String ARGUMENT = "argument";
	private static final String TEMP 	 = "temp";
	
	public VMWriter() {
		super();
	}
	
	public void work(AST syntaxTree) throws IOException {
		this.lblCounter  		= 0;
		this.symbolTable 		= new SymbolTable();
		this.syntaxTree  		= syntaxTree;
		this.syntaxTree.current = this.syntaxTree.root;
		this.className   		= this.syntaxTree.root.value;
		
		Iterator<ASTNode> iterator = this.syntaxTree.root.children.iterator();
		while (iterator.hasNext()) {
			ASTNode node = iterator.next();
			if (node.type.equals(JackParser.CLASS_VAR_DECLARATIONS)) {
				handleClassVarDecs(node);
			}
			else if (node.type.equals(JackParser.SUBROUTINE_DECLARATIONS)) {
				writeSubroutines(node);
			}
		}
		
		fout.close();
	}
	
	private void handleClassVarDecs(ASTNode node) {
		for (ASTNode classVarDec : node.children) {
			String scope = classVarDec.type;
			String type  = classVarDec.children.get(0).value;
			for (ASTNode identifier: classVarDec.children.get(1).children) {
				symbolTable.define(identifier.value, type, scope);
			}
		}
	}
	
	private void handleParameterList(ASTNode node) {
		for (ASTNode parameter: node.children) {
			String type 	  = parameter.children.get(0).value;
			String identifier = parameter.children.get(1).value;
			symbolTable.define(identifier, type, ARGUMENT);
		}
	}
	
	private void writeSubroutines(ASTNode node) throws IOException {
		Iterator<ASTNode> iterator = node.children.iterator();
		while (iterator.hasNext()) {
			ASTNode child = iterator.next();
			writeSubroutineDef(child);
			// TODO symbolTable.print();
			symbolTable.resetSubroutineScope();
		}
	}
	
	private void writeExpressionList(ASTNode node) throws IOException {
		for (ASTNode expression : node.children) {
			writeExpression(expression);
		}
	}
	
	private void writeExpression(ASTNode node) throws IOException {
		switch (node.children.get(0).type) {
			case JackParser.OPERAND:
				writeTerm(node.children.get(1));
				writeTerm(node.children.get(2));
				writeArithmetic(opcode(node.children.get(0).value, false));
				break;
			case JackParser.IDENTIFIER:
				writePush(node.children.get(0).value);
				break;
			case JackParser.TERM:
				writeTerm(node.children.get(0));
				break;
			case JackParser.SUBROUTINE_CALL:
				writeSubroutineCall(node.children.get(0));
				break;
			default:
				throw new IOException("Unsupported Expression: " + node.children.get(0).type);
		}
	}
	
	private void writeSubroutineCall(ASTNode node) throws IOException {
		String variable = null;
		if (node.children.get(0).children.size() != 0) {
			variable = node.children.get(0).children.get(0).value;
		}
		String functionName 	   = node.children.get(1).children.get(0).value;
		ASTNode expressionListNode = node.children.get(2);
		
		int nArgs 				   = expressionListNode.children.size();
		
		// if its null, we operate on this!
		if (variable == null) {
			functionName = className + "." + functionName;
			writePush(POINTER, 0);
			writeExpressionList(node.children.get(2));
			writeCall(functionName, ++nArgs);
		}
		else {
			SymbolTableEntry entry = symbolTable.retrieve(variable);
			if (entry != null) {
				functionName = entry.type + "." + functionName;
				writePush(variable);
				writeExpressionList(node.children.get(2));
				writeCall(functionName, ++nArgs);
			}
			else {
				// else we assume its a static call
				functionName = variable + "." + functionName;
				writeExpressionList(node.children.get(2));
				writeCall(functionName, nArgs);
			}
		}
		
	}

	private void writeUnaryTerm(ASTNode node) throws IOException {
		writeTerm(node.children.get(1));
		writeArithmetic(opcode(node.children.get(0).value, true));
	}
	
	private void writeTerm(ASTNode node) throws IOException {
		if (node.children.size() == 1) {
			String strValue = node.children.get(0).value;
			switch (node.children.get(0).type) {
				case JackParser.INT:
					writePush(CONSTANT, intValue(strValue));
					break;
				case JackParser.CHAR:
					writePush(CONSTANT, charValue(strValue));
					break;
				case JackParser.EXPRESSION:
					writeExpression(node.children.get(0));
					break;
				case JackParser.UNARY_TERM:
					writeUnaryTerm(node.children.get(0));
					break;
				case JackParser.IDENTIFIER:
					writePush(node.children.get(0).value);
					break;
				case JackParser.SUBROUTINE_CALL:
					writeSubroutineCall(node.children.get(0));
					break;
				case JackParser.STRING:
					// TODO proper string handling
					writePush(CONSTANT, 65);
					break;
				case JackParser.ARRAY_REFERENCE:
					writeArrayReference(node.children.get(0));
					writePop(POINTER, 1);
					writePush(THAT, 0);
					break;
				case JackParser.CONSTANT:
					writePush(CONSTANT, constantValue(node.children.get(0).value));
					break;
				default:
					throw new IOException("Unsupported term: " + node.children.get(0).type + "\n" + node.toString());
			}
		}
		else {
			throw new IOException("Term has too many children: " + node.toString());
		}
	}
	
	private void writeSubroutineDef(ASTNode node) throws IOException {
		String returnType   = node.children.get(0).value;
		String functionName = this.className + "." + node.children.get(1).value;
		
		int nLocalVars = 0; // not the best solution
		for (ASTNode varDec : node.children.get(3).children.get(0).children) {
			nLocalVars += varDec.children.get(1).children.size();
		}
		
		if (node.type.equals("FUNCTION")) {
			handleParameterList(node.children.get(2));
			writeFunction(functionName, nLocalVars);
			writeSubroutineBody(node.children.get(3));
		}
		else if (node.type.equals("METHOD")) {
			symbolTable.define("this", this.className, SymbolTable.KIND_ARG);
			handleParameterList(node.children.get(2));
			writeFunction(functionName, nLocalVars);
			writePush(ARGUMENT, 0);
			writePop(POINTER, 0);
			writeSubroutineBody(node.children.get(3));
			
		}
		else if (node.type.equals("CONSTRUCTOR")) {
			int nrFields = symbolTable.varCount(SymbolTable.KIND_FIELD);
			symbolTable.define("this", this.className, SymbolTable.KIND_ARG);
			handleParameterList(node.children.get(2));
			
			// call memory allocate with the given number of fields
			writePush(CONSTANT, nrFields);
			write("call Memory.alloc 1");
			writePop(POINTER, 0);
			
			writeSubroutineBody(node.children.get(3));
		}
	}
	
	private void writeSubroutineBody(ASTNode node) throws IOException {
		handleLocalVarDeclarations(node.children.get(0));
		writeStatements(node.children.get(1));
	}
	
	private void handleLocalVarDeclarations(ASTNode node) throws IOException {
		for (ASTNode localVarDec: node.children) {
			String type = localVarDec.children.get(0).value;
			for (ASTNode identifierNode : localVarDec.children.get(1).children) {
				String identifier = identifierNode.value;
				symbolTable.define(identifier, type, LOCAL);
			}
		}
	}
	
	private void writeStatements(ASTNode node) throws IOException {
		for (ASTNode statement: node.children) {
			switch (statement.type) {
				case JackParser.DO_STATEMENT:
					writeDoStatement(statement);
					break;
				case JackParser.LET_STATEMENT:
					writeLetStatement(statement);
					break;
				case JackParser.IF_STATEMENT:
					writeIfStatement(statement);
					break;
				case JackParser.WHILE_STATEMENT:
					writeWhileStatement(statement);
					break;
				case JackParser.RETURN_STATEMENT:
					writeReturnStatement(statement);
					break;
			}
		}
	}
	
	private void writeReturnStatement(ASTNode node) throws IOException {
		if (node.children.size() == 0) {
			writeReturnVoid();
		}
		else {
			ASTNode expNode = node.children.get(0).children.get(0);
			writeExpression(expNode);
			writeReturn();
		}
	}
	
	private void writeDoStatement(ASTNode node) throws IOException {
		writeSubroutineCall(node.children.get(0));
	}
	
	private void writeLetStatement(ASTNode node) throws IOException {
		// if its an array assignment
		if (node.children.get(0).children.get(0).type.equals(JackParser.ARRAY_REFERENCE)) {
			writeArrayReference(node.children.get(0).children.get(0));
			writeExpression(node.children.get(1).children.get(0));
			writePop(TEMP, 0);
			writePop(POINTER, 1);
			writePush(TEMP, 0);
			writePop(THAT, 0);
		} 
		else {
			String identifier = node.children.get(0).children.get(0).value;
			writeExpression(node.children.get(1).children.get(0));
			writePop(identifier);
		}
	}
	
	private void writeArrayReference(ASTNode node) throws IOException {
			String identifier    = node.children.get(0).value;
			ASTNode indexNode    = node.children.get(1);
			writePush(identifier);	
			writeExpression(indexNode.children.get(0));
			write("add");
	}
	
	private void writeIfStatement(ASTNode node) throws IOException {
		// if it has 3 children, it has an additional else statement
		ASTNode condition = node.children.get(0);
		ASTNode ifBody    = node.children.get(1);
		String afterIf    = createLblFlow();
		if (node.children.size() == 3) {
			ASTNode elseBody = node.children.get(2);
			String afterElse = createLblFlow();
			
			writeExpression(condition.children.get(0));
			write("not");
			writeIf(afterIf);
			writeStatements(ifBody);
			writeGoto(afterElse);
			writeLbl(afterIf);
			writeStatements(elseBody);
			writeLbl(afterElse);
			
		}
		else {
			writeExpression(condition.children.get(0));
			write("not");
			writeIf(afterIf);
			writeStatements(ifBody);
			writeLbl(afterIf);
		}
	}
	
	private void writeWhileStatement(ASTNode node) throws IOException {
		String whileStart = createLblFlow();
		String whileEnd   = createLblFlow();
		writeLbl(whileStart);
		writeExpression(node.children.get(0).children.get(0));
		write("not");
		writeIf(whileEnd);
		writeStatements(node.children.get(1));
		writeGoto(whileStart);
		writeLbl(whileEnd);
	}

	private void writePush(String segment, int index) throws IOException {
		String str = "push " + segment + " " + index + "\n";
		fout.write(str.getBytes());
	}
	
	private void writePush(String identifier) throws IOException {
		SymbolTableEntry entry = symbolTable.retrieve(identifier);
		writePush(entry.scope, entry.index);
	}
	
	private void writePop(String segment, int index) throws IOException {
		String str = "pop " + segment + " " + index + "\n";
		fout.write(str.getBytes());
	}
	
	private void writePop(String identifier) throws IOException {
		SymbolTableEntry entry = symbolTable.retrieve(identifier);
		writePop(entry.scope, entry.index);
	}
	
	private void writeArithmetic(String command) throws IOException {
		String str = command + "\n";
		fout.write(str.getBytes());
	}
	
	private void writeLbl(String lbl) throws IOException {
		String str = "label " + lbl  + "\n";
		fout.write(str.getBytes());
	}
	
	private String createLblFlow() {
		return this.className + ".flow." + lblCounter++;
	}
	
	private void writeGoto(String lbl) throws IOException {
		String str = "goto " + lbl + "\n";
		fout.write(str.getBytes());
	}
	
	private void writeIf(String lbl) throws IOException {
		String str = "if-goto " + lbl + "\n";
		fout.write(str.getBytes());
	}
	
	private void writeCall(String name, int nArgs) throws IOException {
		String str = "call " + name + " " + nArgs + "\n";
		fout.write(str.getBytes());
	}
	
	private void writeFunction(String name, int nLocals) throws IOException {
		String str = "function " + name + " " + nLocals + "\n";
		fout.write(str.getBytes());
	}
	
	private void writeReturn() throws IOException {
		String str = "return" + "\n";
		fout.write(str.getBytes());
	}
	
	private void writeReturnVoid() throws IOException {
		writePush("constant", 0);
		writeReturn();
	}
	
	private void write(String command) throws IOException {
		command = command + "\n";
		fout.write(command.getBytes());
	}
	
	private void close() throws IOException {
		fout.close();
	}
	
	private int charValue(String s) {
		return ((int) s.charAt(0));
	}
	
	private int intValue(String s) {
		return Integer.valueOf(s);
	}
	
	private int constantValue(String s) {
		switch (s) {
		case "this":
			break;
		case "null":
			break;
		case "true":
			break;
		case "false":
			break;
		}
		return 0; // TODO
	}
	
	private String opcode(String op, boolean isUnary) throws IOException {
		switch (op) {
		case "+":
			return "add";
		case "-":
			if (isUnary) {
				return "neg";
			}
			return "sub";
		case "=":
			return "eq";
		case ">":
			return "gt";
		case "<":
			return "lt";
		case "&":
			return "and";
		case "|":
			return "or";
		case "~":
			return "not";
		case "*":
			return "call Math.multiply 2";
		case "/":
			return "call Math.divide 2";
		case "%":
			return "call Math.mod 2";
		default:
			throw new IOException("Unknown Operator:" + op);
		}
	}
}
