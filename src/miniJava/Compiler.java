package miniJava;

import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.CodeGenerator.Encoder;
import miniJava.ContextualAnalyzer.IdChecker;
import miniJava.ContextualAnalyzer.TypeChecker;
import miniJava.StandardEnv.StandardEnvGen;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.SyntaxError;
import miniJava.mJAM.Disassembler;
import miniJava.mJAM.ObjectFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Compiler {

    private static Scanner scanner;
    private static Parser parser;
    private static ErrorReporter reporter;
    private static IdChecker idChecker;
    
    public static void main(String[] args){
        if (args.length != 1) {
            System.out.println("Usage: tc filename");
            System.exit(1);
        }

        try {
        	System.out.println("********** " +
                    "Nick's miniJava Compiler (Version 2.0)" +
                    " **********");

            System.out.println("Syntactic Analysis Get Started...");
            
            FileInputStream in = new FileInputStream(new File(args[0]));

            reporter = new ErrorReporter();
            scanner  = new Scanner(in, reporter);
            parser   = new Parser(scanner, reporter);

            Package p =  parser.parse();

            System.out.println("Syntactic analysis complete:  ");
            if (reporter.hasErrors()) {
                System.out.println("INVALID program");
                System.exit(4);
            } else {
                System.out.println("valid program");
            }
            System.out.println("Contextual Analysis Get Started...");
            idChecker = new IdChecker(p, reporter);

            StandardEnvGen.genEnv(idChecker.idTable);
            idChecker.check();

            TypeChecker typeChecker = new TypeChecker(p, reporter, idChecker.userDefinedString);
            if (!reporter.hasErrors()) typeChecker.check();
            System.out.println("Contextual analysis complete:  ");
            if (reporter.hasErrors()) {
                System.out.println("INVALID program");
                System.exit(4);
            }

            Encoder encoder = new Encoder(p, reporter);
            System.out.println("Code Generations ... ");
            encoder.generate();
            System.out.println("Code Generations complete:  ");

            if (reporter.hasErrors()) {
                System.out.println(" - Code Generations ERROR");
                System.exit(4);
            } else {
                System.out.println(" - Code Generations PASSED");
            }
            System.out.println();

            String objFileName = args[0].substring(0, args[0].lastIndexOf("."));
		/* write code to object code file */
            ObjectFile objF = new ObjectFile(objFileName+".mJAM");
            System.out.print("Writing object code file " + objFileName + " ... ");
            if (objF.write()) {
                System.out.println("FAILED!");
                System.exit(4);
            }
            else System.out.println("SUCCEEDED");

            System.exit(0);

        } catch (Exception e) {
            System.out.println("System.exit(4)");
            e.printStackTrace();
            System.exit(4);
        } catch (SyntaxError e) {
            System.out.println("INVALID program");
            e.printStackTrace();
            System.exit(4);
        }

    }
}
