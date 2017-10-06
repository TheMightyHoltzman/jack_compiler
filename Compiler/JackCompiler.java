package Compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.List;

public class JackCompiler {
	private JackTokenizer tokenizer;
	private JackParser parser;
	private VMWriter vmWriter;
	
	public JackCompiler() {
		super();
		this.tokenizer = new JackTokenizer();
		this.parser = new JackParser();
		this.vmWriter = new VMWriter();
	}
	
	public void work(String finName) {
		File fin = new File(finName);
		if (fin.isDirectory()) {
			compileDirectory(fin);
		}
		else {
			compileFile(fin);
		}
	}
	
	public void compileDirectory(File fin) {
		for (File eFile : fin.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.matches(".*\\.jack$");
			}
		})) {
			compileFile(eFile);
		}
	}

	public void compileFile(File fin) {
		try {
			System.out.println("Compiling file: " + fin.getPath());
			String foutXml        = fin.getPath().replaceAll("\\.jack", "\\.xml"); 
			String foutVm         = fin.getPath().replaceAll("\\.jack", "\\.vm"); 
			FileReader reader     = new FileReader(fin);
			//FileOutputStream foStreanXml = new FileOutputStream(new File(foutXml));
			FileOutputStream foStreanVm  = new FileOutputStream(new File(foutVm));

			List<Token> tokens    = tokenizer.work(reader);
			System.out.println("... finished tokenizing");
			
			AST syntaxTree 		  = parser.work(tokens);
			System.out.println("... finished parsing");
			//foStreanXml.write(syntaxTree.toString().getBytes());
			//System.out.println("... generated XML-File.");
			
			vmWriter.fout = foStreanVm;
			vmWriter.work(syntaxTree);
			System.out.println("... finished code generation");
			
			System.out.println("Successfully completed compilation");
		}
		catch (Exception e) {
			System.out.println("Something went wrong:");
			e.printStackTrace(System.out);
		}
	}
	
	public static void main(String[] args) {
		JackCompiler compiler = new JackCompiler();
		String path = "/home/heiko/learn/nand2tetris/projects/11/MyTest";
		if (args.length != 0) {
			path = args[0];
		}
		else {
			System.out.println("Please specify file or directory to compile.");
		}
		compiler.work(path);
	}
}
