
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JackParser_v1 {

		private List<Token> tokens;
		private int currentIndex;
		private FileOutputStream out;
		
		public JackParser_v1() {
			super();
		}

		public void start(List<Token> aTokens, FileOutputStream aOut) {
			tokens 		 = aTokens;
			out          = aOut;
			currentIndex = 0;

			if (tokens.size() == 0) {
				return; // nothing to do
			}
			
			try {		
				parseClass();
			}
			catch (Exception e) {
				System.out.println("Parsing-Error:");
				System.out.println(e.getMessage());
				e.printStackTrace(System.out);
			}
		}
		
		public void parseClass()  throws Exception {
			writeOpen("class");
			
			process(JackTokenizer.KEYWORD_CLASS);
			
			// class name
			processIdentifier();
			
			process("{");
			
			parseClassVarDec();
			
			parseSubroutineDec();
			
			process("}");
			
			writeClose("class");
		}
		
		
		/**
		 * STAR - matches one or more
		 * @throws Exception
		 */
		public void parseClassVarDec()  throws Exception {
			if (!getCurrent().value.equals(JackTokenizer.KEYWORD_STATIC) && !getCurrent().value.equals(JackTokenizer.KEYWORD_FIELD)) {
				return;
			}
			
			writeOpen("classVarDec");
			
			process(getCurrent().value);
			parseType();
			processIdentifier();
			while (getCurrent().value.equals(",")) {
				process(",");
				processIdentifier();
			}
			process(";");
			
			writeClose("classVarDec");
			
			parseClassVarDec();
		}
		
		/**
		 * 
		 * STAR - matches one or more
		 * @throws Exception
		 */
		public void parseSubroutineDec()  throws Exception {
			writeOpen("subroutineDec");
			
			process(getCurrent().value);
			if (getCurrent().value.equals(JackTokenizer.KEYWORD_VOID)) {
				process(JackTokenizer.KEYWORD_VOID);
			}
			else {
				parseType();
			}
			processIdentifier();
			process("(");
			
			
			writeOpen("parameterList");
			if (!getCurrent().value.equals(")")) {
				parseParameterList();
			}
			writeClose("parameterList");
			
			process(")");
			parseSubroutineBody();
			
			writeClose("subroutineDec");
			
			if (!getCurrent().value.equals("}")) {
				parseSubroutineDec();
			}
		}
		
		
		/**
		 * 
		 * XML-Tokens handled
		 * 
		 * @throws Exception
		 */
		public void parseSubroutineBody()  throws Exception {
			writeOpen("subroutineBody");
			
			process("{");
			
			if (getCurrent().value.equals(JackTokenizer.KEYWORD_VAR)) {
				parseVarDec();
			}
			writeOpen("statements");
			compileStatements();
			writeClose("statements");
			
			process("}");
			
			
			writeClose("subroutineBody");
		}
		
		
		/**
		 * 
		 * XML-Tokens handled in calling method
		 * 
		 * @throws Exception
		 */
		public void parseParameterList()  throws Exception {
			parseType();
			processIdentifier();
			
			while(getCurrent().value.equals(",")) {
				process(",");
				parseType();
				processIdentifier();
			}
		}
		
		/**
		 * Matches one or more!
		 * @throws Exception
		 */
		public void parseVarDec()  throws Exception {
			writeOpen("varDec");
			
			process(JackTokenizer.KEYWORD_VAR);
			parseType();
			processIdentifier();
			while (getCurrent().value.equals(",")) {
				process(",");
				processIdentifier();
			}
			process(";");
			
			writeClose("varDec");
			
			if (getCurrent().value.equals(JackTokenizer.KEYWORD_VAR)) {
				parseVarDec();
			}
		}
		
		public void compileStatements()  throws Exception {
			switch (getCurrent().value) {
				case JackTokenizer.KEYWORD_WHILE:
					parseWhile();
					compileStatements();
					break;
				case JackTokenizer.KEYWORD_IF:
					parseIf();
					compileStatements();
					break;
				case JackTokenizer.KEYWORD_LET:
					parseLet();
					compileStatements();
					break;
				case JackTokenizer.KEYWORD_DO:
					parseDo();
					compileStatements();
					break;
				case JackTokenizer.KEYWORD_RETURN:
					parseReturn();
					compileStatements();
					break;
				default:
					return;
			}
			return;
		}
		
		public void parseType()  throws Exception {
			if (getCurrent().value.equals(")")) {
				// nothing to do
			}
			else if (getCurrent().value.equals("int") || getCurrent().value.equals("char") || getCurrent().value.equals("boolean") ) {
				process(getCurrent().value);
			}
			else {
				// if its a class
				processIdentifier();
			}
		}
		
		public void parseLet()  throws Exception {
			writeOpen("letStatement");
			
			process(JackTokenizer.KEYWORD_LET);
			processIdentifier();
			if (getCurrent().value.equals("[")) {
				process("[");
				parseExpression();
				process("]");
			}
			process("=");
			parseExpression();
			process(";");
			
			writeClose("letStatement");
		}
		
		public void parseIf()  throws Exception {
			writeOpen("ifStatement");
			
			process(JackTokenizer.KEYWORD_IF);
			process("(");
			parseExpression();
			process(")");
			process("{");
			
			writeOpen("statements");
			compileStatements();
			writeClose("statements");
			
			process("}");
			
			if (getCurrent().value.equals(JackTokenizer.KEYWORD_ELSE)) {
				process(JackTokenizer.KEYWORD_ELSE);
				process("{");
				
				writeOpen("statements");
				compileStatements();
				writeClose("statements");
				
				process("}");
			}
			
			writeClose("ifStatement");
		}
		
		public void parseWhile()  throws Exception {
			writeOpen("whileStatement");
			
			process(JackTokenizer.KEYWORD_WHILE);
			process("(");
			parseExpression();
			process(")");
			process("{");

			writeOpen("statements");
			compileStatements();
			writeClose("statements");
			
			process("}");
		
			writeClose("whileStatement");
		}
		
		public void parseDo()  throws Exception {
			writeOpen("doStatement");
			
			process(JackTokenizer.KEYWORD_DO);
			parseSubroutineCall();
			process(";");
			
			writeClose("doStatement");
		}
		
		/**
		 * @throws Exception
		 */
		public void parseSubroutineCall() throws Exception {
			processIdentifier();
			if (getCurrent().value.equals("(")) {
				process("(");
				parseExpressionList();
				process(")");
			}
			else { // else its a classname or some variable-name
				process(".");
				processIdentifier();
				process("(");
				parseExpressionList();
				process(")");
			}
		}
		
		public void parseReturn()  throws Exception {
			writeOpen("returnStatement");
			
			process(JackTokenizer.KEYWORD_RETURN);
			if (!getCurrent().value.equals(";")) {
				parseExpression();
			}
			process(";");
			
			writeClose("returnStatement");
		}
		
		public void parseExpression()  throws Exception {
			writeOpen("expression");
			
			parseTerm();
			while (isOperand()) {
				process(getCurrent().value);
				parseTerm();
			}
			
			writeClose("expression");
		}
		
		public void parseTerm()  throws Exception {
			writeOpen("term");
			
			if (getCurrent().type == "int") {
				process(getCurrent().value);
			}
			else if (getCurrent().type == "string") {
				process(getCurrent().value);
			}
			else if (isUnary()) {
				process(getCurrent().value);
				parseTerm();
			}
			else if (isConstant()) {
				process(getCurrent().value);
			}
			else if (getCurrent().value.equals("(")) {
				process("(");
				parseExpression();
				process(")");
			}
			else if (lookahead(1).equals("[")) {
				processIdentifier();
				process("[");
				parseExpression();
				process("]");
				
			}
			else if (lookahead(1).equals("(") || lookahead(1).equals(".")) {
				parseSubroutineCall();
			}
			else { //varName
				processIdentifier();
			}
			
			writeClose("term");
		}
		
		public void parseExpressionList()  throws Exception  {
			writeOpen("expressionList");
			
			 if (!getCurrent().value.equals(")")) {
				parseExpression();
				while (getCurrent().value.equals(",")) {
					process(",");
					parseExpression();
				}
			}
			
			writeClose("expressionList");
		}
		
		private void processIdentifier() throws Exception {
			process(null);
		}
		
		private void process(String token) throws Exception {
			writeCurrent();
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
		
		private void write(String str) throws IOException {
			out.write(str.getBytes());
		}
		
		private void writeOpen(String str) throws IOException {
			write("<" + str + ">");
		}
		
		private void writeClose(String str) throws IOException {
			write("</" + str + ">");
		}
		
		private void writeCurrent() throws IOException {
			writeOpen(getCurrent().type);
			String value = getCurrent().value
					.replaceAll("&", "&amp;")
					.replaceAll("<", "&lt;")
					.replaceAll(">", "&gt;");
					
			write(" " + value + " ");
			writeClose(getCurrent().type);
		}
	}
