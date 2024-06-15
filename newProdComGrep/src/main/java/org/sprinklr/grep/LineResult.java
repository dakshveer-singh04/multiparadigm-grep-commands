package org.sprinklr.grep;

import java.util.regex.Pattern;

/**
 * Represents the result of a line in a file that matches a pattern
 */
public class LineResult {
    Integer lineNumber;
    String line;
    String file;
    String pattern;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LineResult{");
        if (file != null) {
            sb.append("file='").append(file).append("' , ");
        }
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
     * @param file
     * @param pattern
     */
    private LineResult(Integer lineNumber, String line, String file, Pattern pattern) {
        this.lineNumber = lineNumber;
        this.line = line;
        this.file = file;
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
     * Builder class for constructing LineResult objects based on MyRegexOptions.
     */
    public static class Builder {

        private MyRegexOptions options;
        public Builder(MyRegexOptions options) {
            this.options = options;
        }

        /**
         * Builds a LineResult object based on the specified parameters.
         *
         * @param lineNumber
         * @param line
         * @param file
         * @param pattern
         * @return
         */
        public LineResult build(Integer lineNumber, String line, String file, Pattern pattern) {
            if (options.showLineNumbers && options.showLines && options.showFiles && options.showPattern) {
                return new LineResult(lineNumber, line, file, pattern);
            } else if (options.showLineNumbers && options.showLines && options.showFiles) {
                return new LineResult(lineNumber, line, file, null);
            } else if (options.showLineNumbers && options.showLines && options.showPattern) {
                return new LineResult(lineNumber, line, null, pattern);
            } else if (options.showLineNumbers && options.showFiles && options.showPattern) {
                return new LineResult(lineNumber, null, file, pattern);
            } else if (options.showLineNumbers && options.showPattern) {
                return new LineResult(lineNumber, null, null, pattern);
            } else if (options.showLines && options.showFiles && options.showPattern) {
                return new LineResult(null, line, file, pattern);
            } else if (options.showLines && options.showPattern) {
                return new LineResult(null, line, null, pattern);
            } else if (options.showFiles && options.showPattern) {
                return new LineResult(null, null, file, pattern);
            } else if (options.showLineNumbers && options.showLines) {
                return new LineResult(lineNumber, line, null, null);
            } else if (options.showLineNumbers && options.showFiles) {
                return new LineResult(lineNumber, null, file, null);
            } else if (options.showLineNumbers) {
                return new LineResult(lineNumber, null, null, null);
            } else if (options.showLines && options.showFiles) {
                return new LineResult(null, line, file, null);
            } else if (options.showLines) {
                return new LineResult(null, line, null, null);
            } else if (options.showFiles) {
                return new LineResult(null, null, file, null);
            } else if (options.showPattern) {
                return new LineResult(null, null, null, pattern);
            } else {
                return new LineResult(null, null, null, null);
            }
        }
    };
}
