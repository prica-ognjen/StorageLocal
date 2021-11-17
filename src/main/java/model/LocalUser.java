package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import exceptions.StorageInitException;
import exceptions.UserTypeException;
import user.Privilege;
import user.User;
import user.UserManager;
import user.UserType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LocalUser extends user.User {

    static{
        UserManager.registerUser(new LocalUser());
    }

    LocalUser(){}
    LocalUser(String username, String password, UserType type, Map<Privilege, Boolean> privileges){
        super(username, password, type, privileges);
    }

    @Override
    public void initStorage(String path){
        if (this.getType() != UserType.SUPER || this.getType() == null) {
            throw new UserTypeException("User isn't admin");
        }

        File file = new File(path);
        boolean created = file.mkdir();

        if(!created){
            if(checkIfStorageExists(path)) {
               throw new StorageInitException("Storage " + file.getName() + " already exists at " + file.getParent());
            }
            else if(dirWithSameName(path)){
                throw new StorageInitException("Folder with same name " + file.getName() + "that isn't a storage already exists at " + file.getParent());
            }else{
                throw new StorageInitException("Unexpected error occurred when creating a storage");
            }
        }else{
            initUserData(path, this);
            initConfig(path);
        }
    }

    @Override
    public String toString() {
        return this.getUsername();
    }

    //private methods

    private boolean dirWithSameName(String path) {
        File f = new File(path).getParentFile();
        File storage = new File(path);
        File[] files = f.listFiles();
        assert files != null;
        for(File file: files){
            if(file.getName().equals(storage.getName())){
                return true;
            }
        }
        return false;
    }

    private boolean checkIfStorageExists(String path) {
        File f0 = new File(path);
        if(!f0.exists()){
            return false;
        }
        if(!f0.isDirectory()){
            return false;
        }
        File f1 = new File(path + "/userData.json");
        if(!f1.exists())
            return false;

        File f2 = new File(path + "/config.json");
        return f2.exists();
    }

    private void initUserData(String path, LocalUser user) {
        File file = new File(path + "/userData.json");
        try {
            boolean created = file.createNewFile();
            if(!created){
                throw new StorageInitException("Unexpected error occurred when initializing userData.json");
            }else{
                FileWriter out = new FileWriter(file);
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.setPrettyPrinting().registerTypeAdapter(User.class, new UserSerializer());
                Gson gson = gsonBuilder.create();
                List<LocalUser> userList = new ArrayList<>();
                userList.add(user);
                out.write(gson.toJson(userList));
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initConfig(String path) {
        File file = new File(path + "/config.json");
        try {
            boolean created = file.createNewFile();
            if(!created){
                throw new StorageInitException("Unexpected error occurred when initializing config.json");
            }else{
                FileWriter out = new FileWriter(file);
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.setPrettyPrinting().registerTypeAdapter(Config.class, new ConfigSerializer());
                Gson gson = gsonBuilder.create();
                Config config = new Config(-1, -1, new ArrayList<>());
                out.write(gson.toJson(config));
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
