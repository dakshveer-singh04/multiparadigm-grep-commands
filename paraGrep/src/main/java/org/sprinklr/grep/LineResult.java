package org.sprinklr.grep;

import java.util.regex.Pattern;

/**
 * Represents the result of a line in a file that matches a pattern
 */
public class LineResult {
    Long lineNumber;
    String line;
    String file;
    String pattern;
    int blockNumber;


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LineResult{");
        if (lineNumber != null) {
            sb.append("lineNumber=").append(lineNumber).append(", ");
        }
        if (pattern != null) {
            sb.append("pattern='").append(pattern).append("', ");
        }
        if (line != null) {
            sb.append("line='").append(line).append("'");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Consturctors marked private so can only only use builder
     *
     * @param lineNumber
     * @param line
     * @param pattern
     */
    private LineResult(Long lineNumber, String line, Pattern pattern, int blNo) {
        this.lineNumber = lineNumber;
        this.line = line;
        this.blockNumber=blNo;
        if(pattern!=null) this.pattern = String.valueOf(pattern);
        else this.pattern=null;
    }
    private LineResult() {
        this.lineNumber = null;
        this.line = null;
        this.file = null;
        this.pattern = null;
    }

    /**
     * Builder class for constructing LineResult objects based on MyOptions.
     */
    public static class Builder {
        private MyOptions options;
        public Builder(MyOptions options) {
            this.options = options;
        }

        public LineResult build(Long lineNumber,int blNo ,String line, Pattern pattern) {
            if (options.showLineNumbers && options.showLines && options.showPattern) {
                return new LineResult(lineNumber, line, pattern, blNo);
            } else if (options.showLineNumbers && options.showLines ) {
                return new LineResult(lineNumber, line, null,blNo);
            } else if (options.showLineNumbers && options.showPattern) {
                return new LineResult(lineNumber, null, pattern,blNo);
            } else if (options.showLines && options.showPattern) {
                return new LineResult(null, line, pattern,blNo);
            } else if (options.showPattern) {
                return new LineResult(null, null, pattern,blNo);
            } else if (options.showLineNumbers) {
                return new LineResult(lineNumber, null, null,blNo);
            } else if (options.showLines) {
                return new LineResult(null, line, null,blNo);
            } else {
                return new LineResult(null, null, null,blNo);
            }
        }
    };
}
