import java.util.ArrayList;
import java.util.List;

public class JackParser {

	private List<Token> tokens;
	private int currentIndex;
	private AST syntaxTree;
	
	public static final String CLASS_VAR_DECLARATIONS  = "CLASS_VAR_DECLARATIONS";
	public static final String LOCAL_VAR_DECLARATIONS  = "LOCAL_VAR_DECLARATIONS";
	public static final String SUBROUTINE_DECLARATIONS = "SUBROUTINE_DECLARATIONS";
	public static final String BODY                    = "BODY";
	public static final String PARAMETER_LIST          = "PARAMETER_LIST";
	public static final String PARAMETER               = "PARAMETER";
	public static final String VAR_DEC                 = "VAR_DEC";
	public static final String TYPE                    = "TYPE";
	public static final String TYPE_CLASS              = "TYPE_CLASS";
	public static final String STATEMENTS  		       = "STATEMENTS";
	public static final String LET_STATEMENT  		   = "LET_STATEMENT";
	public static final String WHILE_STATEMENT         = "WHILE_STATEMENT";
	public static final String IF_STATEMENT            = "IF_STATEMENT";
	public static final String ELSE_STATEMENT          = "ELSE_STATEMENT";
	public static final String DO_STATEMENT            = "DO_STATEMENT";
	public static final String ARRAY_REFERENCE         = "ARRAY_REFERENCE";
	public static final String ARRAY_INDEX             = "ARRAY_INDEX";
	public static final String TERM					   = "TERM";             
	public static final String VARIABLE			       = "VARIABLE";
	public static final String ASSIGNMENT		 	   = "ASSIGNMENT";
	public static final String INT		         	   = "INT";
	public static final String CHAR		         	   = "CHAR";
	public static final String STRING			       = "STRING";
	public static final String CONSTANT				   = "CONSTANT";
	public static final String CONDITION			   = "CONDITION";
	public static final String FUNCTION_CALL		   = "FUNCTION_CALL";
	public static final String VAR		         	   = "VAR";
	public static final String SUBROUTINE_CALL         = "SUBROUTINE_CALL";
	public static final String FUNCTION_NAME           = "FUNCTION_NAME";
	public static final String RETURN_STATEMENT        = "RETURN_STATEMENT";
	public static final String EXPRESSION              = "EXPRESSION";
	public static final String UNARY_TERM              = "UNARY_TERM";
	public static final String OPERAND                 = "OPERAND";
	public static final String IDENTIFIER              = "IDENTIFIER";
	
	public JackParser() {
		super();
	}

	public AST work(List<Token> aTokens) {
		syntaxTree = new AST();
		ASTNode root    = new ASTNode("CLASS", null);
		syntaxTree.root = root;
		
		tokens 		 = aTokens;
		currentIndex = 0;

		if (tokens.size() == 0) {
			return null; // nothing to do
		}
		
		try {		
			parseClass();
		}
		catch (Exception e) {
			System.out.println("Parsing-Error:");
			System.out.println(e.getMessage());
			e.printStackTrace(System.out);
		}
		return syntaxTree;
	}
	
	public void parseClass()  throws Exception {
		process(JackTokenizer.KEYWORD_CLASS);
		
		// class name
		syntaxTree.className  = getCurrent().value;
		syntaxTree.root.value = getCurrent().value;
		process(getCurrent().value);
		
		process("{");
		
		ASTNode classVarNode = syntaxTree.root.addChild(CLASS_VAR_DECLARATIONS, null);
		parseClassVarDec(classVarNode);

		ASTNode subroutinesNode = syntaxTree.root.addChild(SUBROUTINE_DECLARATIONS, null);
		parseSubroutineDec(subroutinesNode);
		
		process("}");
	}
	
	
	/**
	 * STAR - matches one or more
	 * @throws Exception
	 */
	public void parseClassVarDec(ASTNode classVarNode)  throws Exception {
		if (!getCurrent().value.equals(JackTokenizer.KEYWORD_STATIC) && !getCurrent().value.equals(JackTokenizer.KEYWORD_FIELD)) {
			return;
		}
		
		// static or field
		ASTNode singleVarNode = classVarNode.addChild(getCurrent().value, null);
		process(getCurrent().value);
		
		parseType(singleVarNode);
		ASTNode identifiersNode = singleVarNode.addChild("IDENTIFIERS", null);
		processIdentifier(identifiersNode);

		while (getCurrent().value.equals(",")) {
			process(",");
			processIdentifier(identifiersNode);
		}
		process(";");
		
		parseClassVarDec(classVarNode);
	}
	
	/**
	 * 
	 * STAR - matches one or more
	 * @throws Exception
	 */
	public void parseSubroutineDec(ASTNode subroutinesNode)  throws Exception {
		// either method or function here
		if (!getCurrent().value.equals(JackTokenizer.KEYWORD_FUNCTION) && !getCurrent().value.equals(JackTokenizer.KEYWORD_METHOD) && !getCurrent().value.equals(JackTokenizer.KEYWORD_CONSTRUCTOR)) {
			throw new Exception("subroutine declarations must start with 'method' or 'function'");
		}
		ASTNode subroutineNode = subroutinesNode.addChild(getCurrent().value.toUpperCase(), null);
		process(getCurrent().value);
		
		if (getCurrent().value.equals(JackTokenizer.KEYWORD_VOID)) {
			subroutineNode.addChild("TYPE", JackTokenizer.KEYWORD_VOID);
			process(JackTokenizer.KEYWORD_VOID);
		}
		else {
			parseType(subroutineNode);
		}
		processIdentifier(subroutineNode);
		process("(");
		
		
		ASTNode parameterList = subroutineNode.addChild(PARAMETER_LIST, null);
		if (!getCurrent().value.equals(")")) {
			parseParameterList(parameterList);
		}
		
		process(")");
		parseSubroutineBody(subroutineNode);
		
		if (!getCurrent().value.equals("}")) {
			parseSubroutineDec(subroutinesNode);
		}
	}
	
	
	/**
	 * 
	 * XML-Tokens handled
	 * 
	 * @throws Exception
	 */
	public void parseSubroutineBody(ASTNode node)  throws Exception {
		ASTNode subroutineBody = node.addChild(BODY, null);
		process("{");
		
		ASTNode localVarDecs = subroutineBody.addChild(LOCAL_VAR_DECLARATIONS, null);
		while (getCurrent().value.equals(JackTokenizer.KEYWORD_VAR)) {
			parseSingleVarDec(localVarDecs);
		}
		ASTNode statements = subroutineBody.addChild(STATEMENTS, null);
		compileStatements(statements);
		
		process("}");
	}
	
	
	/**
	 * 
	 * XML-Tokens handled in calling method
	 * 
	 * @throws Exception
	 */
	public void parseParameterList(ASTNode parameterList)  throws Exception {
		parseSingleParameter(parameterList);
		
		while(getCurrent().value.equals(",")) {
			process(",");
			parseSingleParameter(parameterList);
		}
	}
	
	public void parseSingleParameter(ASTNode parameterListNode) throws Exception {
		ASTNode singleParameter = parameterListNode.addChild(PARAMETER, null);
		parseType(singleParameter);
		processIdentifier(singleParameter);
	}
	
	/**
	 * Matches one or more!
	 * @throws Exception
	 */
	public void parseVarDec(ASTNode node)  throws Exception {
		parseSingleVarDec(node);
		
		if (getCurrent().value.equals(JackTokenizer.KEYWORD_VAR)) {
			parseVarDec(node);
		}
	}
	
	public void parseSingleVarDec(ASTNode node) throws Exception {
		ASTNode singleVarDec = node.addChild(VAR_DEC, null);
		process(JackTokenizer.KEYWORD_VAR);
		parseType(singleVarDec);
		ASTNode identifiersNode = singleVarDec.addChild("IDENTIFIERS", null);
		processIdentifier(identifiersNode);
		while (getCurrent().value.equals(",")) {
			process(",");
			processIdentifier(identifiersNode);
		}
		process(";");
	}
	
	public void compileStatements(ASTNode node)  throws Exception {
		switch (getCurrent().value) {
			case JackTokenizer.KEYWORD_WHILE:
				parseWhile(node);
				compileStatements(node);
				break;
			case JackTokenizer.KEYWORD_IF:
				parseIf(node);
				compileStatements(node);
				break;
			case JackTokenizer.KEYWORD_LET:
				parseLet(node);
				compileStatements(node);
				break;
			case JackTokenizer.KEYWORD_DO:
				parseDo(node);
				compileStatements(node);
				break;
			case JackTokenizer.KEYWORD_RETURN:
				parseReturn(node);
				compileStatements(node);
				break;
			default:
				return;
		}
		return;
	}
	
	public void parseType(ASTNode node)  throws Exception {
		if (getCurrent().value.equals(")")) {
			// nothing to do
		}
		else if (getCurrent().value.equals("int") || getCurrent().value.equals("char") || getCurrent().value.equals("boolean") ) {
			node.addChild(TYPE, getCurrent().value);
			process(getCurrent().value);
		}
		else {
			node.addChild(TYPE_CLASS, getCurrent().value);
			// if its a class
			process(getCurrent().value);
		}
	}
	
	public void parseLet(ASTNode node)  throws Exception {
		ASTNode letNode      = node.addChild(LET_STATEMENT, null);
		ASTNode variableNode = letNode.addChild(VARIABLE, null);
		
		process(JackTokenizer.KEYWORD_LET);
		
		if (lookahead(1).equals("[")) {
			ASTNode arrefNode = variableNode.addChild(ARRAY_REFERENCE, null);
			processIdentifier(arrefNode);
			ASTNode arrayIndex = arrefNode.addChild(ARRAY_INDEX, null);
			process("[");
			parseExpression(arrayIndex);
			process("]");
		}
		else {
			processIdentifier(variableNode);
		}
		process("=");
		ASTNode assignmentNode = letNode.addChild(ASSIGNMENT, null);
		parseExpression(assignmentNode);
		process(";");
	}
	
	public void parseIf(ASTNode node)  throws Exception {
		ASTNode ifNode = node.addChild(IF_STATEMENT, null);
		process(JackTokenizer.KEYWORD_IF);
		process("(");
		ASTNode condition = ifNode.addChild(CONDITION, null);
		parseExpression(condition);
		process(")");
		process("{");
		
		ASTNode body = ifNode.addChild(BODY, null);
		compileStatements(body);
		
		process("}");
		
		if (getCurrent().value.equals(JackTokenizer.KEYWORD_ELSE)) {
			ASTNode elseNode = ifNode.addChild(ELSE_STATEMENT, null);
			process(JackTokenizer.KEYWORD_ELSE);
			process("{");
			
			ASTNode elseBody = elseNode.addChild(BODY, null);
			compileStatements(elseBody);
			
			process("}");
		}
	}
	
	public void parseWhile(ASTNode node)  throws Exception {
		ASTNode whileNode = node.addChild(WHILE_STATEMENT, null);
		process(JackTokenizer.KEYWORD_WHILE);
		process("(");
		ASTNode condition = whileNode.addChild(CONDITION, null);
		parseExpression(condition);
		process(")");
		process("{");

		ASTNode body = whileNode.addChild(BODY, null);
		compileStatements(body);
		
		process("}");
	}
	
	public void parseDo(ASTNode node)  throws Exception {
		ASTNode doNode = node.addChild(DO_STATEMENT, null);
		process(JackTokenizer.KEYWORD_DO);
		parseSubroutineCall(doNode);
		process(";");
	}
	
	/**
	 * @throws Exception
	 */
	public void parseSubroutineCall(ASTNode node) throws Exception {
		ASTNode subroutineNode = node.addChild(SUBROUTINE_CALL, null);
		if (lookahead(1).equals("(")) {
			subroutineNode.addChild("VAR", null); // var-node has no further children in static call of same class
			ASTNode nameNode = subroutineNode.addChild(FUNCTION_NAME, null);
			processIdentifier(nameNode);
			process("(");
			parseExpressionList(subroutineNode);
			process(")");
		}
		else { // else its a classname or some variable-name
			ASTNode varNode = subroutineNode.addChild(VAR, null);
			processIdentifier(varNode);
			process(".");
			ASTNode nameNode = subroutineNode.addChild(FUNCTION_NAME, null);
			processIdentifier(nameNode);
			process("(");
			parseExpressionList(subroutineNode);
			process(")");
		}
	}
	
	public void parseReturn(ASTNode node) throws Exception {
		ASTNode returnNode = node.addChild(RETURN_STATEMENT, null);
		process(JackTokenizer.KEYWORD_RETURN);
		if (!getCurrent().value.equals(";")) {
			ASTNode body = returnNode.addChild("BODY", null);
			parseExpression(body);
		}
		process(";");
	}
	
	public void parseExpression(ASTNode node) throws Exception {
		ASTNode expression = node.addChild(EXPRESSION, null);
		parseTerm(expression);
		while (isOperand()) {
			ASTNode firstTerm = expression.popLastChild();
			expression.addChild(OPERAND, getCurrent().value);
			process(getCurrent().value);
			expression.addChild(firstTerm);
			parseTerm(expression);
		}
	}
	
	public void parseTerm(ASTNode node) throws Exception {
		ASTNode termNode = node.addChild(TERM, null);
		if (getCurrent().type == "int") {
			termNode.addChild(INT, getCurrent().value);
			process(getCurrent().value);
		}
		else if (getCurrent().type.equals("string")) {
			termNode.addChild(STRING, getCurrent().value);
			process(getCurrent().value);
		}
		else if (isUnary()) {
			ASTNode unaryNode = termNode.addChild(UNARY_TERM, null);
			unaryNode.addChild(OPERAND, getCurrent().value);
			process(getCurrent().value);
			parseTerm(unaryNode);
		}
		else if (isConstant()) {
			termNode.addChild(CONSTANT, getCurrent().value);
			process(getCurrent().value);
		}
		else if (getCurrent().value.equals("(")) {
			process("(");
			parseExpression(termNode);
			process(")");
		}
		// Array Reference
		else if (lookahead(1).equals("[")) {
			ASTNode arrefNode = termNode.addChild(ARRAY_REFERENCE, null);
			processIdentifier(arrefNode);
			process("[");
			ASTNode arrayIndex = arrefNode.addChild(ARRAY_INDEX, null);
			parseExpression(arrayIndex);
			process("]");
			
		}
		else if (lookahead(1).equals("(") || lookahead(1).equals(".")) {
			parseSubroutineCall(termNode);
		}
		else { //varName
			processIdentifier(termNode);
		}
	}
	
	public void parseExpressionList(ASTNode node)  throws Exception  {
		ASTNode expressionList = node.addChild("EXPRESSION_LIST", null);
		if (!getCurrent().value.equals(")")) {
			parseExpression(expressionList);
			while (getCurrent().value.equals(",")) {
				process(",");
				parseExpression(expressionList);
			}
		}
	}
	
	private void processIdentifier(ASTNode node) throws Exception {
		node.addChild(IDENTIFIER, getCurrent().value);
		process(null);
	}
	
	private void process(String token) throws Exception {
		if (currentIndex >= tokens.size()) {
			throw new Exception("Premature stop");
		}
		if (token == null) { // identifier-case
			advance();
		}
		else if (token.equals(getCurrent().value)) {
			advance();
		}
		else {
			Exception e = new Exception("Unexpected token: " + getCurrent().value);
			e.printStackTrace();
			throw e;
		}
	}
	
	private Token getCurrent() {
		return tokens.get(currentIndex);
	}
	
	private String lookahead(int steps) {
		if (tokens.size() <= currentIndex + steps) {
			// we cant lookahead that many steps, since the array is too small
			return null;
		}
		else {
			return tokens.get(currentIndex + steps).value;
		}
	}
	
	private void advance() {
		currentIndex++;
	}
	
	private boolean isOperand() {
		List<String> ops = new ArrayList<>();
		ops.add("+");
		ops.add("-");
		ops.add("*");
		ops.add("/");
		ops.add("&");
		ops.add("|");
		ops.add("<");
		ops.add(">");
		ops.add("=");
		
		return ops.contains(getCurrent().value);
	}
	
	private boolean isUnary() {
		List<String> ops = new ArrayList<>();
		ops.add("~");
		ops.add("-");
		
		return ops.contains(getCurrent().value);
	}
	
	private boolean isConstant() {
		List<String> ops = new ArrayList<>();
		ops.add("null");
		ops.add("this");
		ops.add("true");
		ops.add("false");
		
		return ops.contains(getCurrent().value);
	}
}
