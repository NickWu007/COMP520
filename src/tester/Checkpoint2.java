package tester;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Scanner;

/* Automated regression tester for Checkpoint 2 tests
 * Created by Max Beckman-Harned, update by jfp to handle varying miniJava project organizatons in Eclipse
 * Put your tests in "tests/pa2_tests" folder in your Eclipse workspace directory
 * If you preface your error messages / exceptions with ERROR or *** then they will be displayed if they appear during processing
 */

public class Checkpoint2 {

    private static class ReturnInfo {
        int returnCode;
        String ast;
        public ReturnInfo(int _returnCode, String _ast) {
            returnCode = _returnCode;
            ast = _ast;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // should be project directory for miniJava and tester
        String cwd = System.getProperty("user.dir");
        String testDirPath = cwd + "/src/tests/pa2_tests";
        System.out.println("Running tests from directory " + testDirPath);
        File testDir = new File(testDirPath);
        int failures = 0;
        for (File x : testDir.listFiles()) {
            if (x.getName().endsWith("out") || x.getName().startsWith("."))
                continue;
            ReturnInfo info = runTest(x);
            int returnCode = info.returnCode;
            String ast = info.ast;
            if (x.getName().indexOf("pass") != -1) {
                if (returnCode == 0) {
                    String actualAST = getAST(new FileInputStream(x.getPath() + ".out"));
                    if (actualAST.equals(ast))
                        System.out.println(x.getName() + " parsed successfully and has a correct AST!");
                    else {
                        System.err.println(x.getName() + " parsed successfully but has an incorrect AST!");
                        failures++;
                    }
                }
                else {
                    failures++;
                    System.err.println(x.getName()
                            + " failed to be parsed!");
                }
            } else {
                if (returnCode == 4)
                    System.out.println(x.getName() + " failed successfully!");
                else {
                    System.err.println(x.getName() + " did not fail properly!");
                    failures++;
                }
            }
        }
        System.out.println(failures + " failures in all.");
    }

    private static ReturnInfo runTest(File x) throws IOException, InterruptedException {
        // should be be project directory when source and class files are kept together
        // should be "bin" subdirectory of project directory when separate src
        // and bin organization is used
        String jcp = System.getProperty("java.class.path");

        String testPath = x.getPath();
        ProcessBuilder pb = new ProcessBuilder("java", "miniJava.Compiler", testPath);
        pb.directory(new File(jcp));
        pb.redirectErrorStream(true);
        Process p = pb.start();

        String ast = getAST(p.getInputStream());
        p.waitFor();
        int exitValue = p.exitValue();
        return new ReturnInfo(exitValue, ast);
    }


    public static String getAST(InputStream stream) {
        Scanner scan = new Scanner(stream);
        String ast = null;
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            if (line.equals("======= AST Display =========================")) {
                line = scan.nextLine();
                while(scan.hasNext() && !line.equals("=============================================")) {
                    ast += line + "\n";
                    line = scan.nextLine();
                }
            }
            if (line.startsWith("*** "))
                System.out.println(line);
            if (line.startsWith("ERROR")) {
                System.out.println(line);
                while(scan.hasNext())
                    System.out.println(scan.next());
            }
        }
        scan.close();
        return ast;
    }
}
