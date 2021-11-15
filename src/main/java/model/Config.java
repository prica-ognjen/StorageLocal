package model;

import java.util.List;

public class Config {

    private int size;
    private int fileLimit;
    private List<String> exclExt;

    Config(int size, int fileLimit, List<String> exclExt) {
        this.size = size;
        this.fileLimit = fileLimit;
        this.exclExt = exclExt;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getFileLimit() {
        return fileLimit;
    }

    public void setFileLimit(int fileLimit) {
        this.fileLimit = fileLimit;
    }

    public List<String> getExclExt() {
        return exclExt;
    }

    public void setExclExt(List<String> exclExt) {
        this.exclExt = exclExt;
    }
}
