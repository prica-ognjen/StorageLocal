package model;


import storage.Storage;
import user.User;
import java.util.ArrayList;
import java.util.List;

public class LocalStorage extends Storage {

    private User currUser;
    private Config config;
    private List<LocalUser> users;

    LocalStorage(String path, String name, User superuser) {
        super(path, name, superuser);
        this.config = new Config(-1, -1, new ArrayList<>());
        this.users = new ArrayList<>();
    }

    public User getCurrUser(){
        return this.currUser;
    }

    protected void setCurrUser(User user){
        this.currUser = user;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public List<LocalUser> getUsers() {
        return users;
    }

    public void setUsers(List<LocalUser> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
