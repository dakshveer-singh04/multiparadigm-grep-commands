package org.sprinklr.grep;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a grep operation
 */
public class Result {
    List<LineResult> result;
    int count;

    Result(){
        result= new ArrayList<>();
        count=0;
    }

    public void addResult(LineResult lineResult) {
        result.add(lineResult);
        count++;
    }

    public void printResults() {
        for(LineResult lineResult : result) {
            System.out.println(lineResult);
        }
    }

    public void printCount() {
        System.out.println("Total matches found  : " + count);
    }
}
