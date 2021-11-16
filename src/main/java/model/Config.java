package model;

import java.util.ArrayList;

public class Config {

    private int sizeLimit;
    private int fileNumLimit;
    private ArrayList<String> blockedExtensions;

    Config(){}

    Config(int sizeLimit, int fileNumLimit, ArrayList<String> blockedExtensions) {
        this.sizeLimit = sizeLimit;
        this.fileNumLimit = fileNumLimit;
        this.blockedExtensions = blockedExtensions;
    }

    public int getSizeLimit() {
        return sizeLimit;
    }

    public void setSizeLimit(int sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    public int getFileNumLimit() {
        return fileNumLimit;
    }

    public void setFileNumLimit(int fileNumLimit) {
        this.fileNumLimit = fileNumLimit;
    }

    public ArrayList<String> getBlockedExtensions() {
        return blockedExtensions;
    }

    public void setBlockedExtensions(ArrayList<String> blockedExtensions) {
        this.blockedExtensions = blockedExtensions;
    }
}
