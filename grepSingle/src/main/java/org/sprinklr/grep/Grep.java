package org.sprinklr.grep;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Grep class for flexible and customizable way to perform text searches in files using regular expressions
 */
public class Grep {
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
    public Grep(String pwd, List<Pattern> regexPatterns, List<String> files, List<String> directories, MyRegexOptions options,List<String> ignoreExtn) throws IllegalArgumentException, IOException {
        this.pwd=pwd;
        this.options=options;
        this.regexPatterns=regexPatterns;
        this.files=files;
        this.directories=directories;
        this.grepResult=new Result();
        this.builder=new LineResult.Builder(options);
        this.ignoreExtn=ignoreExtn;

        if(options.dirSearch){
            addFilesRecursive();
        }

        if(files.isEmpty()){
            throw new IllegalArgumentException("{ No files to search }");
        }
    }

    /**
     * Method to execute the grep operation
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
     * Method to process a file for search
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
                        LineResult lineResult = builder.build(lineNumber, matcher.group(), file, pattern);
                        grepResult.addResult(lineResult);
                    }
                }
                lineNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to process a file for inverted search
     *
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
