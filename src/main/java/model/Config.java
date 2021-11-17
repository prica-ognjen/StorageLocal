package model;

import user.Privilege;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    private int sizeLimit;
    private int fileNumLimit;
    private List<String> blockedExtensions;
    private List<Map<String, DirConfig>> fileConfigMap;

    public Config(){
        sizeLimit = -1;
        fileNumLimit = -1;
        blockedExtensions = new ArrayList<>();
        fileConfigMap = new ArrayList<>();
    }

    Config(int sizeLimit, int fileNumLimit, ArrayList<String> blockedExtensions, List<Map<String, DirConfig>>fileConfigMap) {
        this.sizeLimit = sizeLimit;
        this.fileNumLimit = fileNumLimit;
        this.blockedExtensions = blockedExtensions;
        this.fileConfigMap = fileConfigMap;
    }

    public List<Map<String, DirConfig>> getFileConfigMap() {
        return fileConfigMap;
    }

    public void setFileConfigMap(List<Map<String, DirConfig>> fileConfigMap) {
        this.fileConfigMap = fileConfigMap;
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

    public List<String> getBlockedExtensions() {
        return blockedExtensions;
    }

    public void setBlockedExtensions(List<String> blockedExtensions) {
        this.blockedExtensions = blockedExtensions;
    }

    @Override
    public String toString() {
        return "Config{" +
                "sizeLimit=" + sizeLimit +
                ", fileNumLimit=" + fileNumLimit +
                '}';
    }
}
