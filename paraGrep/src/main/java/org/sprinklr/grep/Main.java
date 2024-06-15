package org.sprinklr.grep;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.lang.Thread.sleep;

/**
 * Main class takes input from the commmand line and instantiates the paraGrep(producer-consumer in a file) object to search
 */
public class Main {
    static String ANSI_GREEN = "\u001B[32m";
    static String ANSI_YELLOW = "\u001B[33m";
    static String ANSI_RED = "\u001B[31m";
    static String ANSI_RESET = "\u001B[0m";
    static String ANSI_PURPLE = "\u001B[35m";

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String pwd = System.getProperty("user.dir");
        if(args.length<2){
            System.err.println("Usage : Grep.java -p <pattern1> <pattern2> ... -f <file1> <file2> ..  [ -c | -n | -l |  ] ");
            return;
        }
        System.out.println("Under dev ... " +ANSI_PURPLE +" paraGrep "+ANSI_RESET);
        System.out.println("ran command from = "+pwd);

        List<Pattern> regexPatterns = new ArrayList<>();
        List<String> options = new ArrayList<>();
        String myFileToSearch="";

        for(int i = 0; i < args.length; i++) {
            System.out.println("parsing args["+i+"]="+args[i]);
            switch(args[i]){
                case "-p":
                    i++;
                    while (i < args.length && !args[i].startsWith("-")) {
                        try {
                            regexPatterns.add(Pattern.compile(args[i]));
                        } catch (PatternSyntaxException e ){
                            System.err.println(ANSI_RED+"Regex pattern is not valid: " + e.getMessage());
                            System.err.println(" [ignored]  + " + args[i] + " pattern"+ANSI_RESET);
                        }
                        i++;
                    }
                    i--;
                    break;
                case "-f":
                    i++;

                    try{
                        String filePath = pwd + "/" + args[i];
                        // Check if the file exists
                        boolean exists = Files.exists(Paths.get(filePath));
                        if (exists) {
                            myFileToSearch=args[i];
                        } else {
                            System.err.println(ANSI_RED + "File "+args[i]+ " does not exist. [IGNORING] : filePath="+filePath + ANSI_RESET);
                        }
                    } catch (Exception e) {
                        System.err.println(ANSI_RED+"[ERROR] opening file "+args[i] + " : " +e+ANSI_RESET);
                    }

                    break;
                case "-n":
                    //to show line number
                    options.add("-n");
                    break;
                case "-l":
                    //to show the line
                    options.add("-l");
                    break;
                case "-sp":
                    //to show the file
                    options.add("-sp");
                    break;
                case "-i":
                    //case insensitive
                    options.add("-i");
                    break;
                case "-c":
                    //only count
                    options.add("-c");
                    break;
                default:
                    System.err.println(ANSI_RED+"Usage : Grep.java -p <pattern1> <pattern2> ... -f <file1> <file2> ..  [-c | -n | -v ] ");
                    System.err.println("... unknown option " + args[i] + "  ignoring... "+ANSI_RESET);
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
        System.out.println("File gotten = ["+myFileToSearch+"]");
        System.out.print("Options = [");
        for(String option : options){
            System.out.print(option +",");
        }
        System.out.println("]");


        MyOptions myOptions=new MyOptions(options);

        List<Pattern> patternsToPass=new ArrayList<>();
        if(myOptions.caseInsensitive){
            System.out.println("making patterns in insensitive");
            for(Pattern pattern : regexPatterns) {
                patternsToPass.add(Pattern.compile(pattern.pattern(), pattern.flags() | Pattern.CASE_INSENSITIVE));
            }
        } else {
            for(Pattern pattern : regexPatterns) {
                patternsToPass.add(Pattern.compile(pattern.pattern() ));
            }
        }

        ParallelFileReader myFileReader = new ParallelFileReader(myOptions,10,10,myFileToSearch,patternsToPass);
        long beginTime=System.currentTimeMillis();
        myFileReader.execute();
        long endTime=System.currentTimeMillis();

        try {
            sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(!myOptions.onlyCountLines) {
            myFileReader.printResults();
        }

        System.out.println(ANSI_GREEN);
        myFileReader.grepResult.printCount();
        System.out.print(ANSI_RESET);

        System.out.println(ANSI_YELLOW+" ... ParallelFileReader took " + (endTime-beginTime) + " ms"+ANSI_RESET);
    }
}


