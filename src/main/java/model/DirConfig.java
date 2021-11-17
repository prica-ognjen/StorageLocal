package model;

import user.Privilege;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirConfig {

    private int sizeLimit;
    private int fileNumLimit;
    private List<String> blockedExtensions;
    private Map<String, List<Privilege>> blockedPrivileges;

    DirConfig() {
        this.sizeLimit = -1;
        this.fileNumLimit = -1;
        this.blockedExtensions = new ArrayList<>();
        this.blockedPrivileges = new HashMap<>();
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

    public Map<String, List<Privilege>> getBlockedPrivileges() {
        return blockedPrivileges;
    }

    public void setBlockedPrivileges(Map<String, List<Privilege>> blockedPrivileges) {
        this.blockedPrivileges = blockedPrivileges;
    }
}
