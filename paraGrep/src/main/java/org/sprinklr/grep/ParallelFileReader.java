package org.sprinklr.grep;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for performing paraGrep operations using producer-consumer pattern on a single file
 * Uses multiple producer threads to read portions of the file and put lines into a queue,
 * and multiple consumer threads to process the lines and match them against regex patterns.
 */
public class ParallelFileReader {
    BlockingQueue<LineData> linesQueue = new LinkedBlockingQueue<>();
    int PRODUCER_THREADS;
    int CONSUMER_THREADS;
    String filePath;
    MyOptions myOptions;
    AtomicInteger activeProducers;
    List<Pattern> regexPatterns;
    LineResult.Builder builder;
    Result grepResult;
    static int LINE_SEP_LEN=System.lineSeparator().getBytes().length;
    List<Future<Long>> linesPrevFutures;
    List<Long> linesPrev;

    public ParallelFileReader(MyOptions myOptions,int PRODUCER_THREADS, int CONSUMER_THREADS, String filePath,List<Pattern> regexPatterns) {
        this.PRODUCER_THREADS = PRODUCER_THREADS;
        this.CONSUMER_THREADS = CONSUMER_THREADS;
        this.filePath = filePath;
        this.myOptions=myOptions;
        this.regexPatterns=regexPatterns;
        this.builder=new LineResult.Builder(myOptions);
        this.activeProducers=new AtomicInteger();
        this.grepResult=new Result();

        this.linesPrev = new ArrayList<>(Collections.nCopies(PRODUCER_THREADS + 1, 0L));
        this.linesPrevFutures=new ArrayList<Future<Long>>(PRODUCER_THREADS+1);
    }

    /**
     * Prints the matching results stored in the grepResult object.
     */
    public void printResults() throws ExecutionException, InterruptedException {
        //        System.out.print(" prevLines : [");
        for(int i=1; i<=PRODUCER_THREADS; i++){
            linesPrev.set(i, linesPrevFutures.get(i).get() + linesPrev.get(i - 1));
//            System.out.print(linesPrev.get(i)+",");
        }
//        System.out.println("]");
        for(LineResult res : grepResult.grepResults ){
            System.out.println("LineResult : {"
                    + (res.lineNumber != null ? "lineNumber=" + (res.lineNumber + linesPrev.get(res.blockNumber)) + ", " : "")
                    + (res.line != null ? "line=" + res.line + ", " : "")
                    + (res.pattern != null ? "pattern=" + res.pattern : "")
                    + "}");
        }
    }

    /**
     * adjusts the end location to the nearest newline character ('\n') within a file
     */
    private long adjustEndLocation(FileChannel channel, long endLocation) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        while (endLocation < channel.size() - 1) {
            channel.read(buffer, endLocation);
            buffer.flip();
            if (buffer.get() == '\n') {
                break;
            }
            buffer.clear();
            endLocation++;
        }
        return endLocation;
    }

    /**
     * helper method used the time of dev and debugging
     * used to get character at a pos; used to verify each character is a starting character;
     */
    private char getCharAtPos(FileChannel channel, long pos) {
        try {
            channel.position(pos);
            ByteBuffer buffer = ByteBuffer.allocate(1);
            int bytesRead = channel.read(buffer);
            if (bytesRead != -1) {
                buffer.flip();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                char character = new String(bytes, StandardCharsets.UTF_8).charAt(0);
                return character;
            } else {
                System.out.println("End of file reached.");
            }
            return '@';
        } catch (Exception e){
            System.out.println("----------" + e);
        }
        return '@';
    }

    /**
     * Executes the producer-consumer grep operation
     */
    public void execute() {
        try(FileInputStream fileInputStream= new FileInputStream(filePath);
            FileChannel channel = fileInputStream.getChannel()){

            long fileSize = channel.size();
            System.out.println("file size = "+fileSize);
            long chunkSize = fileSize / PRODUCER_THREADS;

//            ExecutorService executor = Executors.newFixedThreadPool(PRODUCER_THREADS+CONSUMER_THREADS+1);

//            BlockingQueue<Runnable>taskQueueProducer = new ArrayBlockingQueue<>(100);
            ExecutorService producerExecutor= Executors.newFixedThreadPool(this.PRODUCER_THREADS+1);

            RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
            BlockingQueue<Runnable>taskQueueConsumer = new ArrayBlockingQueue<>(100);
            ExecutorService consumerExecutor= new ThreadPoolExecutor(this.CONSUMER_THREADS,this.CONSUMER_THREADS,0,TimeUnit.NANOSECONDS,taskQueueConsumer,rejectedExecutionHandler);

//            AtomicInteger linesPut=new AtomicInteger();
//            AtomicInteger linesConsumed=new AtomicInteger();

            linesPrevFutures.add(0, CompletableFuture.completedFuture(0L) );
            long position = 0;
            for (int i=0; i<PRODUCER_THREADS; i++) {
                long start = position;
                long end = (i == PRODUCER_THREADS - 1) ? fileSize : position + chunkSize;
                long newEnd;
                if (i < PRODUCER_THREADS - 1) {
                    newEnd= adjustEndLocation(channel, end);
                } else {
                    newEnd=end;
                }
//                System.out.println("startPos=" + start + " endPos=" + newEnd);
//                System.out.println("stChar=" + getCharAtPos(channel,start));
//                executor.submit(new Producer(filePath,start,newEnd,linesPut,activeProducers));
                linesPrevFutures.add(i+1, producerExecutor.submit(new Producer(filePath,start,newEnd,activeProducers,i)) );
                position = newEnd+1;
            }

            for(int i=0; i<CONSUMER_THREADS; i++){
//                executor.submit(new Consumer(linesConsumed));
                consumerExecutor.submit(new Consumer());
            }

//            executor.shutdown();
            producerExecutor.shutdown();
            consumerExecutor.shutdown();
            try {
//                executor.awaitTermination(Long.MAX_VALUE,TimeUnit.NANOSECONDS);
                producerExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                consumerExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (Exception e){
                System.out.println("Exception caught : " + e);
                e.printStackTrace();
            } finally {
//                System.out.println( " ... " + linesPut.get() + " lines put in the queue !!!");
//                System.out.println( " ... " + linesConsumed.get() + " lines consumed from the queue !!!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * A Callable implementation that reads a portion of a file and puts each line into a queue for processing.
     */
    public class Producer implements Callable<Long>{
        RandomAccessFile randomAccessFile;
        long start;
        long end;
        long sizeLimit;
        AtomicInteger activeProducers;
        private int blockNumber;

        public Producer(String filePath, long start, long end, AtomicInteger activeProducers,int blockNumber) {
            try {
                this.randomAccessFile = new RandomAccessFile(filePath,"r");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            this.start = start;
            this.end = end;
            this.sizeLimit=this.end-this.start;
            this.activeProducers=activeProducers;
            this.blockNumber=blockNumber;
        }

        /**
         * Reads lines from the file within the specified range and puts them into a queue for processing.
         *
         * @return The number of lines read; later used to offset the blocks ahead
         */
        @Override
        public Long call() {
            long lineNumber=0;
            try {
                activeProducers.incrementAndGet();
//                System.out.println(" prodcuer {started} : " + activeProducers.get() +" with st="+start+" end="+end);

                randomAccessFile.seek(start);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(randomAccessFile.getFD())));

                int bytesReadTillNow=0;
                String line;
                while((line=bufferedReader.readLine())!=null){
                    ++lineNumber;
                    int bytesReadFromLine=line.length() + LINE_SEP_LEN;

                    bytesReadTillNow+=bytesReadFromLine;
//                    System.out.println(Thread.currentThread().getName()+" processed line='"+line+"'" + " read"+bytesReadFromLine+"bytes" );
                    linesQueue.put(new LineData(lineNumber,line,blockNumber));
//                    globalLineNumber.incrementAndGet();

                    if (bytesReadTillNow > sizeLimit) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                activeProducers.decrementAndGet();
                //                System.out.println(" prodcuer {ended} : " + activeProducers.get());
            }
            return lineNumber;
        }
    }

    /**
     * A runnable class for that processes LineData objects from a BlockingQueue,
     * matches them against the list of regex patterns,
     * and adds matching results to a Result.
     */
    private class Consumer implements Runnable {
        @Override
        public void run(){
            try {
                LineData lineData;
                while (activeProducers.get()>0 || !linesQueue.isEmpty() ) {
                    lineData=linesQueue.poll(1,TimeUnit.MILLISECONDS);
                    if(lineData==null) continue;

                    for (Pattern pattern : regexPatterns) {
                        Matcher matcher = pattern.matcher(lineData.getLineContent());
                        while(matcher.find()) {
                            LineResult lineResult = builder.build(lineData.getLineNumber(),lineData.getBlockNumber(),matcher.group(), pattern);
                            grepResult.addResult(lineResult);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            } finally {
//                System.out.println(" consumer DIED !!!!!!");
            }
        }
    }
}