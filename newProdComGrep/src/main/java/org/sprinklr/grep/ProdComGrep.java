package org.sprinklr.grep;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
* Class for performing newGrep operations using producer-consumer pattern
*/
public class ProdComGrep {
    String pwd;
    List<Pattern> regexPatterns;
    BlockingQueue<String> filesQueue;
    List<String> ignoreExtn;
    List<String> directories;
    MyRegexOptions options;
    Result grepResult;
    LineResult.Builder builder;
    BlockingQueue<LineData> linesQueue = new LinkedBlockingQueue<LineData>();
    AtomicInteger activeProducers;

    int PRODUCER_THREADS;
    int CONSUMER_THREADS;

    public ProdComGrep(String pwd, List<Pattern> regexPatterns, List<String> files, List<String> directories, MyRegexOptions options,List<String> ignoreExtn, int pts, int cts) throws IOException {
        this.pwd = pwd;
        this.regexPatterns = regexPatterns;
        this.filesQueue = new LinkedBlockingDeque<>();
        filesQueue.addAll(files);
        this.ignoreExtn = ignoreExtn;
        this.directories = directories;
        this.options = options;

        this.grepResult=new Result();
        this.builder=new LineResult.Builder(options);

        this.activeProducers=new AtomicInteger();

        this.PRODUCER_THREADS=pts;
        this.CONSUMER_THREADS=cts;

        if(options.dirSearch){
            addFilesRecursive();
        }
    }

    /**
     * Recursively adds files from the specified directory and its subdirectories to the filesQueue
     *
     * @throws IOException
     */
    private void addFilesRecursive() throws IOException {
        Path dir = Paths.get(pwd);
        addFileRecursiveUtil(dir);
    }

    /**
     * Utility method to recursively add files from the specified directory and its subdirectories to the filesQueue.
     *
     * @param dir
     * @throws IOException
     */
    private void addFileRecursiveUtil(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    String relFilePath=path.toString().substring(pwd.length()+1);
                    filesQueue.add(relFilePath);
                }
            });
        }
    }

    /**
    * Executes the producer-consumer grep operation.
    */
    public void execute()  {
//        ExecutorService commonExecutor= Executors.newFixedThreadPool(this.CONSUMER_THREADS+this.PRODUCER_THREADS+1);
        ExecutorService producerExecutor= Executors.newFixedThreadPool(this.PRODUCER_THREADS+1);
        ExecutorService consumerExecutor= Executors.newFixedThreadPool(this.CONSUMER_THREADS+1);

        for (int i = 0; i < this.PRODUCER_THREADS; i++) {
//            commonExecutor.execute(new NewProducer(filesQueue,pwd,linesQueue,ignoreExtn));
            producerExecutor.execute(new NewProducer(filesQueue,pwd,linesQueue,ignoreExtn,activeProducers));
        }
        for (int i = 0; i < this.CONSUMER_THREADS; i++) {
//            consumerExecutor.execute(new NewConsumer(linesQueue,regexPatterns,grepResult,builder));
            consumerExecutor.execute(new NewConsumer(linesQueue,regexPatterns,grepResult,builder,activeProducers));
        }

//        commonExecutor.shutdown();
        producerExecutor.shutdown();
        consumerExecutor.shutdown();

        try {
//            commonExecutor.awaitTermination(Integer.MAX_VALUE,TimeUnit.NANOSECONDS);
            producerExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            consumerExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * A runnable class that reads files from a file queue,
     * filters out files with specified extensions,
     * and puts line data into a lines queue for processing by consumers.
     */
    static class NewProducer implements Runnable {
        private BlockingQueue<String> fileQueue;
        private String pwd;
        private BlockingQueue<LineData> linesQueue;
        private List<String> ignoreExtn;
        private final AtomicInteger activeProducers;
        public NewProducer(BlockingQueue<String> fileQueueParam, String pwd, BlockingQueue<LineData> linesQueue, List<String> ignoreExtn,AtomicInteger activeProducers) {
            this.fileQueue = fileQueueParam;
            this.pwd=pwd;
            this.linesQueue=linesQueue;
            this.ignoreExtn=ignoreExtn;
            this.activeProducers=activeProducers;
        }

        @Override
        public void run(){
            try {
                activeProducers.incrementAndGet();

                while (!fileQueue.isEmpty()) {
                    String filePath = fileQueue.take();

                    boolean flag=true;
                    for(String ignore : ignoreExtn){
                        if(filePath.endsWith(ignore)) {
                            flag=false;
                            break;
                        }
                    }
                    if(!flag) continue;

                    try (BufferedReader reader = new BufferedReader(new FileReader(pwd+"/"+filePath))) {
                        String line;
                        int lineNumber=0;
                        while ((line = reader.readLine()) != null) {
                            linesQueue.put(new LineData(filePath, lineNumber, line));
                        }
                        ++lineNumber;
                    }
                }
            } catch (InterruptedException | IOException e) {
                Thread.currentThread().interrupt();
            } finally {
                activeProducers.decrementAndGet();
            }
        }
    }

    /**
     * A runnable class for that processes LineData objects from a BlockingQueue,
     * matches them against the list of regex patterns,
     * and adds matching results to a Result.
     */
    static class NewConsumer implements Runnable{
        private BlockingQueue<LineData> linesQueue;
        private List<Pattern> regexPatterns;
        private Result grepResult;
        private LineResult.Builder builder;
        private final AtomicInteger activeProducers;

        public NewConsumer(BlockingQueue<LineData> linesQueue, List<Pattern> regexPatterns, Result grepResult, LineResult.Builder builder, AtomicInteger activeProducers) {
            this.linesQueue = linesQueue;
            this.builder=builder;
            this.regexPatterns=regexPatterns;
            this.grepResult=grepResult;
            this.activeProducers=activeProducers;
        }

        @Override
        public void run(){
            try {
                LineData lineData;

                while (!linesQueue.isEmpty() || activeProducers.get()>0) {
                    lineData=linesQueue.poll(1,TimeUnit.MILLISECONDS);
                    if(lineData==null) continue;

                    for (Pattern pattern : regexPatterns) {
                        Matcher matcher = pattern.matcher(lineData.getLineContent());
                        while(matcher.find()) {
                            LineResult lineResult = builder.build(lineData.getLineNumber(), matcher.group(), lineData.getFile(), pattern);
                            grepResult.addResult(lineResult);
                        }
                    }
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            } finally {
//                System.out.println("consumer DIED !!!-------!!!");
            }
        }
    }

}
