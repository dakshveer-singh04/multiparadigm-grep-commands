package org.sprinklr.grep;

/**
 * LineData is the bundled up object (of the line) put in the shared buffer; added blockNumnber to later calculate line number
 */
public class LineData {
    private long lineNumber;
    private String lineContent;
    private int blockNumber;

    public LineData(long lineNumber, String lineContent, int blockNumber) {
        this.lineNumber = lineNumber;
        this.lineContent = lineContent;
        this.blockNumber = blockNumber;
    }

    public int getBlockNumber(){
        return blockNumber;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public String getLineContent() {
        return lineContent;
    }

}
