package org.sprinklr.grep;

import java.util.List;

/**
 * Represents the options for paraGrep operation.
 */
public class MyOptions {
    final boolean onlyCountLines;
    final boolean caseInsensitive;
    final boolean showLineNumbers;
    final boolean showLines;
    final boolean showPattern;

    public MyOptions(List<String> options){
        this.onlyCountLines = options.contains("-c");
        this.showLineNumbers = options.contains("-n");
        this.showLines = options.contains("-l");
        this.caseInsensitive = options.contains("-i");
        this.showPattern = options.contains("-sp");

        if( onlyCountLines && ( showLines || showLineNumbers || showPattern)  ){
            throw new IllegalArgumentException("-c cannot be used togther with [ -n | -l | -sp ]");
        }
    }
}
