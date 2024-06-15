package org.sprinklr.grep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the result of a grep operation
 */
public class Result {
    List<LineResult> result;

    Result(){
        result= Collections.synchronizedList(new ArrayList<>());
    }

    public void addResult(LineResult lineResult) {
        result.add(lineResult);
    }

    public void printResults() {
        for(LineResult lineResult : result) {
            System.out.println(lineResult);
        }
    }

    public void printCount() {
        System.out.println("Total matches found  : " + result.size());
    }
}
