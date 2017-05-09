package miniJava.StandardEnv;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalyzer.IdTable;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by NickWu on 2017/3/23.
 */
public class StandardEnvGen {

    public static void genEnv(IdTable idTable) {
        try {
            String projDir = System.getProperty("user.dir");
            String filename = "";
            if (projDir.contains("bin")) { 
            	filename = projDir + "/../standardEnv.txt";
            } else {
            	filename = projDir + "/standardEnv.txt";
            }
            FileInputStream in = new FileInputStream(new File(filename));


            ErrorReporter reporter = new ErrorReporter();
            Scanner scanner = new Scanner(in, reporter);
            Parser parser = new Parser(scanner, reporter);

            Package p = parser.parse();

            ClassDecl systemClass;
            ClassDecl printClass = null;
            idTable.openScope();
            for (ClassDecl classDecl : p.classDeclList) {
            	classDecl.id.decl = classDecl;
                if (classDecl.id.spelling.equals("String")) {
                    classDecl.type = new BaseType(TypeKind.UNSUPPORTED, classDecl.posn);
                }
                if (classDecl.id.spelling.equals("System")) {
                    systemClass = classDecl;
                    classDecl.type = new ClassType(classDecl.id, classDecl.posn);
                    ((ClassType)classDecl.type).className.decl = classDecl;
                }
                if (classDecl.id.spelling.equals("_PrintStream")) {
                	classDecl.type = new ClassType(classDecl.id, classDecl.posn);
                	printClass = classDecl;
                	((ClassType)classDecl.type).className.decl = classDecl;
                }
                idTable.enterDecl(classDecl);
            }

            for (ClassDecl classDecl : p.classDeclList) {
                idTable.openScope();

                for (FieldDecl fieldDecl : classDecl.fieldDeclList) {
                    idTable.enterDecl(fieldDecl);
                    if (fieldDecl.id.spelling.equals("out")) {
                    	fieldDecl.type = new ClassType(printClass.id, printClass.posn);
                    	((ClassType)fieldDecl.type).className.decl = printClass;
                    }
                }
                for (MethodDecl methodDecl : classDecl.methodDeclList) {
                    idTable.enterDecl(methodDecl);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
