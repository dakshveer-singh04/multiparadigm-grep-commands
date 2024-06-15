package org.sprinklr.grep;

import java.util.List;

/**
 * Represents the options for newGrep operation.
 */
public class MyRegexOptions {
    final boolean onlyCountLines;
    final boolean invertedSearch;
    final boolean caseInsensitive;
    final boolean showLineNumbers;
    final boolean showLines;
    final boolean showFiles;
    final boolean dirSearch;
    final boolean showPattern;

    public MyRegexOptions(List<String> options){
        this.onlyCountLines = options.contains("-c");
        this.invertedSearch = options.contains("-v");
        this.showLineNumbers = options.contains("-n");
        this.showLines = options.contains("-l");
        this.showFiles = options.contains("-sf");
        this.caseInsensitive = options.contains("-i");
        this.dirSearch = options.contains("-R");
        this.showPattern = options.contains("-sp");

        if( onlyCountLines && ( showLines || showLineNumbers ||  showFiles || showPattern)  ){
            throw new IllegalArgumentException("-c cannot be used togther with [ -n | -l | -sf | -sp ]");
        }
    }
}
