public class AST {	
	ASTNode root;
	ASTNode current;
	String className;
	
	@Override
	public String toString() {
		return root.toString();
	}
}
