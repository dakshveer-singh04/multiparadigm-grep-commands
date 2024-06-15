package org.sprinklr.grep;

/**
 * LineData is the bundled up object (of the line) put in the shared buffer
 */
public class LineData {
    private String file;
    private int lineNumber;
    private String lineContent;

    public LineData(String file, int lineNumber, String lineContent) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.lineContent = lineContent;
    }

    public String getFile() {
        return file;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getLineContent() {
        return lineContent;
    }
}
