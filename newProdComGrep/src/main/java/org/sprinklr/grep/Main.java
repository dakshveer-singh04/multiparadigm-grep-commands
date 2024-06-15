package org.sprinklr.grep;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.lang.Thread.sleep;

/**
 * Main class takes input from the commmand line and instantiates the newGrep(producer-consumer across files) object to search
 */
public class Main {
    static String ANSI_GREEN = "\u001B[32m";
    static String ANSI_YELLOW = "\u001B[33m";
    static String ANSI_RED = "\u001B[31m";
    static String ANSI_PURPLE = "\u001B[35m";
    static String ANSI_RESET = "\u001B[0m";

    public static void main(String[] args) throws IOException {
        String pwd = System.getProperty("user.dir");
        if(args.length<2){
            System.out.println("Usage : Grep.java -p <pattern1> <pattern2> ... -f <file1> <file2> ..  [ -c | -n | -l |  ] ");
            return;
        }
        System.out.println("Under dev ... " +ANSI_PURPLE +" newGrep "+ANSI_RESET);
        System.out.println("ran command from = "+pwd);

        List<Pattern> regexPatterns = new ArrayList<>();
        List<String> options = new ArrayList<>();
        List<String> files = new ArrayList<>();
        List<String> fileIgnoreExtension = new ArrayList<>();
        List<String> dirs=new ArrayList<>();

        for(int i = 0; i < args.length; i++) {
            System.out.println("parsing args["+i+"]="+args[i]);
            switch(args[i]){
                case "-p":
                    i++;
                    while (i < args.length && !args[i].startsWith("-")) {
                        try {
                            regexPatterns.add(Pattern.compile(args[i]));
                        } catch (PatternSyntaxException e ){
                            System.out.println(ANSI_RED+"Regex pattern is not valid: " + e.getMessage());
                            System.out.println(" [ignored]  + " + args[i] + " pattern"+ANSI_RESET);
                        }
                        i++;
                    }
                    i--;
                    break;
                case "-f":
                    i++;
                    while (i < args.length && !args[i].startsWith("-")) {
                        try{
                            String filePath = pwd + "/" + args[i];
                            // Check if the file exists
                            boolean exists = Files.exists(Paths.get(filePath));
                            if (exists) {
                                files.add(args[i]);
                            } else {
                                System.out.println(ANSI_RED+"File "+args[i]+ " does not exist. [IGNORING] : filePath="+filePath+ANSI_RESET);
                            }
                        } catch (Exception e) {
                                System.out.println(ANSI_RED+"[ERROR] opening file "+args[i] + " : " + e+ANSI_RESET);
                        }
                        i++;
                    }
                    i--;
                    break;
                case "-n":
                    //to show line number
                    options.add("-n");
                    break;
                case "-l":
                    //to show the line
                    options.add("-l");
                    break;
                case "-sf":
                    //to show the file
                    options.add("-sf");
                    break;
                case "-sp":
                    //to show the file
                    options.add("-sp");
                    break;
                case "-v":
                    //inverted search
                    options.add("-v");
                    break;
                case "-i":
                    //case insensitive
                    options.add("-i");
                    break;
                case "-c":
                    //only count
                    options.add("-c");
                    break;
                case "-R":
                    options.add("-R");
                    break;
                case "-ig":
                    options.add("-ig");
                    i++;
                    while (i < args.length && !args[i].startsWith("-")) {
                        try{
                            fileIgnoreExtension.add(args[i]);
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                        i++;
                    }
                    i--;
                    break;
                default:
                    System.out.println(ANSI_RED+"Usage : Grep.java -p <pattern1> <pattern2> ... -f <file1> <file2> ..  [-c | -n | -v ] ");
                    System.out.println("... unknown option " + args[i] + "  ignoring... "+ANSI_RESET);
                    break;
            }
        }

        System.out.println("arguments processed.");
        System.out.print("Patterns = [");
        for(Pattern regexPattern : regexPatterns){
            System.out.print(regexPattern + ",");
        }
        System.out.println("]");

        //now process files and throw excpetions if any
        System.out.print("Files gotten = [");
        for(String file : files){
            System.out.print(file +",");
        }
        System.out.println("]");
        System.out.print("FilesIgnoreExtn = [");
        for(String ele : fileIgnoreExtension){
            System.out.print(ele +",");
        }
        System.out.println("]");
        System.out.print("Dirs = [");
        for(String dir : dirs){
            System.out.print(dir +",");
        }
        System.out.println("]");
        System.out.print("Options = [");
        for(String option : options){
            System.out.print(option +",");
        }
        System.out.println("]");

        MyRegexOptions optionsObj=new MyRegexOptions(options);

        List<Pattern> patternsToPass=new ArrayList<>();
        if(optionsObj.caseInsensitive){
            System.out.println("making patterns in insensitive");
            for(Pattern pattern : regexPatterns) {
                patternsToPass.add(Pattern.compile(pattern.pattern(), pattern.flags() | Pattern.CASE_INSENSITIVE));
            }
        } else {
            for(Pattern pattern : regexPatterns) {
                patternsToPass.add(Pattern.compile(pattern.pattern() ));
            }
        }

        ProdComGrep myGrep0= new ProdComGrep(pwd,patternsToPass,files,dirs,optionsObj,fileIgnoreExtension,10,5);
        long begin = System.currentTimeMillis();
        myGrep0.execute();
        long end = System.currentTimeMillis();

        try {
            sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(!optionsObj.onlyCountLines) myGrep0.grepResult.printResults();

        System.out.println(ANSI_GREEN);
        myGrep0.grepResult.printCount();
        System.out.print(ANSI_RESET);
        System.out.println(ANSI_YELLOW+" ... newProdComGrep took " + (end-begin) + " ms"+ANSI_RESET);
    }
}
