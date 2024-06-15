package org.sprinklr.grep;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Grep(multiFile) class for flexible and customizable way to perform text searches in files using regular expressions
 */
public class MuliFileGrep {
    String pwd;
    List<Pattern> regexPatterns;
    List<String> files;
    List<String> ignoreExtn;
    List<String> directories;
    MyRegexOptions options;
    Result grepResult;
    LineResult.Builder builder;

    /**
     *
     * @param pwd
     * @param regexPatterns
     * @param files
     * @param directories
     * @param options
     * @param ignoreExtn
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public MuliFileGrep(String pwd, List<Pattern> regexPatterns, List<String> files, List<String> directories, MyRegexOptions options,List<String> ignoreExtn) throws IllegalArgumentException, IOException {
        this.pwd=pwd;
        this.options=options;
        this.regexPatterns=regexPatterns;
        this.files=files;
        this.directories=directories;
        this.ignoreExtn=ignoreExtn;

        this.grepResult=new Result();
        this.builder=new LineResult.Builder(options);

        if(options.dirSearch){
            addFilesRecursive();
        }

        if(files.isEmpty()){
            throw new IllegalArgumentException("{ No files to search }");
        }
    }

    /**
     * dummy execute from singleGrep; not used in this code
     */
    public void execute() {
        if(options.invertedSearch){
            for(String file : files){
                invertedProcessFile(file);
            }
        } else {
            for(String file : files){
                processFile(file);
            }
        }
    }

    /**
     * Executes the grep operation in parallel for each file, according to inverted flag
     */
    public void executeFileParallel() {
        ExecutorService excutor = Executors.newFixedThreadPool(25);
        try {
            if (options.invertedSearch) {
                for (String file : files) {
                    boolean flag=true;
                    for(String ignore : ignoreExtn){
                        if(file.endsWith(ignore)) {
                            flag=false;
                            break;
                        }
                    }
                    if(flag) excutor.submit( () -> processFile(file) );
                }
            } else {
                for (String file : files) {
                    boolean flag=true;
                    for(String ignore : ignoreExtn){
                        if(file.endsWith(ignore)) {
                            flag=false;
                            break;
                        }
                    }
                    if(flag) excutor.submit( () -> processFile(file) );
                }
            }
            excutor.shutdown();
            excutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (Exception e){
            System.out.println(e);
        }
    }

    /**
     * Method to recursively add files in the specified directories
     *
     * @throws IOException
     */
    private void addFilesRecursive() throws IOException {
        Path dir = Paths.get(pwd);
        addFileRecursiveUtil(dir);
    }

    /**
     * Utility method to add files recursively
     */
    private void addFileRecursiveUtil(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    String relFilePath=path.toString().substring(pwd.length()+1);
                    files.add(relFilePath);
                }
            });
        }
    }

    /**
     * Processes a file by matching the regular expression patterns in each line
     *
     * @param file
     */
    private void processFile(String file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(pwd+"/"+file))) {
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                for (Pattern pattern : regexPatterns) {
                    Matcher matcher = pattern.matcher(line);
                    while(matcher.find()) {
                        grepResult.addResult( builder.build(lineNumber, matcher.group(), file, pattern) );
                    }
                }
                lineNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes a file in an inverted manner, excluding lines that match the regular expression patterns
     * @param file
     */
    private void invertedProcessFile(String file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(pwd+"/"+file))) {
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                for (Pattern pattern : regexPatterns) {
                    Matcher matcher = pattern.matcher(line);
                    if (!matcher.find()) {
                        LineResult lineResult = builder.build(lineNumber, line, file,pattern);
                        grepResult.addResult(lineResult);
                    }
                }
                lineNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
