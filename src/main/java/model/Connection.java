package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import exceptions.AuthException;
import exceptions.ConnectionException;
import storage.Storage;
import user.Privilege;
import user.User;
import user.UserType;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class Connection implements connection.Connection {

    private LocalStorage storage;
    private boolean connOn;

    @Override
    public Storage connectToStorage(String s, String s1, String s2) throws Exception{

        String[] split = s.split("\\\\");
        StringBuilder stringBuilder = new StringBuilder();
        String name = split[split.length-1];

        for(int i = 0; i < split.length-1; i++){
            stringBuilder.append(split[i]);
            if(i != split.length-2)
                stringBuilder.append("\\");

        }

        String to = stringBuilder.toString();

        File root = new File(s);
        if(root.exists() && !root.isDirectory()){
            System.out.println("root isn't directory");
            return null;
        }
        File userData = new File(s + "/userData.json");
        if(!userData.exists() || userData.isDirectory()){
            System.out.println("userData doesn't exist or its a directory");
            return null;
        }
        File config = new File(s + "/config.json");
        if(!config.exists() || config.isDirectory()){
            System.out.println("config doesn't exist or its a directory");
            return null;
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.registerTypeAdapter(User.class, new UserSerializer()).create();

        List<LocalUser> userList = new ArrayList<>();
        Config config1 = null;

        try {
            JsonReader reader = new JsonReader(new FileReader(userData));
            Type type = new TypeToken<List<User>>() {}.getType();

            userList= gson.fromJson(reader, type);

            if(userList.isEmpty()){
                System.out.println("storage wasn't initialized so there are no users");
                return null;
            }

            reader = new JsonReader(new FileReader(config));
            type = new TypeToken<List<Config>>() {}.getType();

            config1 = gson.fromJson(reader,type);



        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //dir exists
        //userData exists and isn't empty
        //config exists

        LocalUser user = loginUser(s, s1, s2);

        //auth successful

        getStorageConnection(to, name, userList, config1, user);
        connOn = true;
        return storage;
    }

    private void getStorageConnection(String to, String name, List<LocalUser> userList, Config config, LocalUser user) throws Exception {

        LocalStorage s =  new LocalStorage(to, name, userList.get(0));
        s.setConfig(config);
        s.setUsers(userList);

        if(s.getCurrUser() != null){
            throw new ConnectionException("Another user is currently connected.");
        }

        s.setCurrUser(user);
        storage = s;

    }

    @Override
    public Storage connectToStorage(String s, User user) throws Exception{
        return connectToStorage(s, user.getUsername(), user.getPassword());
    }

    @Override
    public String closeConnection() throws Exception {

        if(!connOn){
            return "fail: there is no active connection";
        }

        String name = this.storage.getName();

        this.connOn = false;
        this.storage = null;

        return "success: connection to : " + name + "is terminated";
    }

    @Override
    public String closeConnectionToUser() throws Exception {
        if(!connOn){
            return "fail: there is no active connection";
        }

        String name = this.storage.getName();

        this.connOn = false;
        return "success: " + storage.getCurrUser() + " disconnected";
    }

    @Override
    public String addUser(String username, String password, String type) throws Exception {

        if(!connOn){
            return "fail: there is no active connection";
        }

        if(!storage.getCurrUser().getType().equals(UserType.SUPER)){
            return "fail: user doesn't have superuser privileges";
        }

        if(type.equals("super")){
            //regulate exceptions
            return "fail: superuser can only create regular users";
        }

        if(findUser(username) != null){
            return "fail: username taken";
        }

        //type mora reg da bude!

        List<LocalUser> tempUsers = getUserData();

        Map<Privilege, Boolean> map = new HashMap<>();

        map.put(Privilege.MOV, false);
        map.put(Privilege.ADD, false);
        map.put(Privilege.VIEW, false);
        map.put(Privilege.DWN, false);
        map.put(Privilege.DEL, false);

        LocalUser u = new LocalUser(username, password, UserType.REGULAR, map);

        tempUsers.add(u);
        String s = saveUserData(tempUsers);

        if(s.contains("success")){
            return "success: user: " + u + " added to storage: " + storage;
        }

       return "fail: something went wrong with adding a new user.";
    }

    @Override
    public String delUser(String s) throws Exception {

        if(!connOn){
            return "fail: there is no active connection";
        }

        if(!storage.getCurrUser().getType().equals(UserType.SUPER)){
            return "fail: user doesn't have superuser privileges";
        }

        if(findUser(s) == null){
            return "fail: there is no user with this username";
        }else if(Objects.requireNonNull(findUser(s)).getUsername().equals(storage.getCurrUser().getUsername())){
            return "fail: superuser cannot delete itself";
        }

        List<LocalUser> tempUsers = getUserData();
        LocalUser toDel=null;

        for(LocalUser u: tempUsers){
            if(u.getUsername().equals(s)){
                toDel = u;
                break;
            }
        }

        tempUsers.remove(toDel);

        String save = saveUserData(tempUsers);

        if(save.contains("success")){
            return "success: user: " + toDel + " removed from storage: " + storage;
        }

        return "fail: something went wrong with deleting a user.";

    }

    @Override
    public String addPrivilege(String s, Privilege privilege) throws Exception {
        //create hierarchy
        //DEL,ADD,VIEW,DWN,MOV
        //VIEW,DWN,MOV,ADD,DEL -> mislim da je ovako ok

        if(!connOn){
            return "fail: there is no active connection";
        }

        if(!storage.getCurrUser().getType().equals(UserType.SUPER)){
            return "fail: user doesn't have superuser privileges";
        }
        LocalUser tUser = findUser(s);

        if(tUser == null){
            return "fail: there is no user with this username";
        }

        if(privilege == null){
            return "privilege you've entered is null";
        }

        if(tUser.getPrivileges().get(privilege)){
            return "fail: user already has this privilege";
        }

        List<LocalUser> tempUsers = getUserData();
        LocalUser tempU = null;

        for(LocalUser u: tempUsers){
            if(u.getUsername().equals(s)){
                tempU = u;
                u.getPrivileges().put(privilege, true);
                break;
            }
        }

        String save = saveUserData(tempUsers);

        if(save.contains("success")){
            return "success: added privilege: " + privilege.name() + " to user: " + tempU;
        }

        return "fail: something went wrong with adding a privilege.";
    }

    @Override
    public String delPrivilege(String s, Privilege privilege) throws Exception {
        if(!connOn){
            return "fail: there is no active connection";
        }

        if(!storage.getCurrUser().getType().equals(UserType.SUPER)){
            return "fail: user doesn't have superuser privileges";
        }

        LocalUser tUser = findUser(s);

        if(tUser == null){
            return "fail: there is no user with this username";
        }

        if(privilege == null){
            return "privilege you've entered is null";
        }

        if(!tUser.getPrivileges().get(privilege)){
            return "fail: user doesn't have this privilege";
        }

        List<LocalUser> tempUsers = getUserData();
        LocalUser tempU = null;

        for(LocalUser u: tempUsers){
            if(u.getUsername().equals(s)){
                tempU = u;
                u.getPrivileges().put(privilege, false);
                break;
            }
        }

        String save = saveUserData(tempUsers);

        if(save.contains("success")){
            return "success: removed privilege: " + privilege.name() + " to user: " + tempU;
        }

        return "fail: something went wrong with removing a privilege.";
    }

    @Override
    public String createFile(String s, String s1) {
        return null;
    }

    @Override
    public String createDir(String s, String s1) {
        return null;
    }

    @Override
    public String delFile(String s) {
        return null;
    }

    @Override
    public String delDir(String s) {
        return null;
    }

    @Override
    public String listDir(String s) {
        return null;
    }

    @Override
    public String movFile(String s, String s1) {
        return null;
    }

    @Override
    public String movDir(String s, String s1) {
        return null;
    }

    @Override
    public String downloadFile(String s, String s1) {
        return null;
    }

    @Override
    public String limitStorageSize(String s, int i) throws Exception {
        return null;
    }

    @Override
    public String limitFileNumber(String s, int i) throws Exception {
        return null;
    }

    @Override
    public String blockExtForStorage(String s, String s1) throws Exception {
        return null;
    }

    private LocalUser findUser(String name) {

        List<LocalUser> tempUsers = getUserData();
        for(LocalUser u: tempUsers){
            if(u.getUsername().equals(name)){
                return u;
            }
        }
        return null;
    }

    private String saveUserData(List<LocalUser> tempUsers) {
        File file = new File(storage.getPath()+"/"+storage.getName() + "/userData.json");
        try {
            if(!file.exists()){
                return "fail: userData doesn't exist.";
            }else{
                FileWriter out = new FileWriter(file);
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.setPrettyPrinting().registerTypeAdapter(User.class, new UserSerializer());
                Gson gson = gsonBuilder.create();
                out.write(gson.toJson(tempUsers));
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "success: userData updated.";
    }

    private List<LocalUser> getUserData() {

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.registerTypeAdapter(User.class, new UserSerializer()).create();
        File file = new File(storage.getPath()+"/"+storage.getName()+ "/userData.json");
        List<LocalUser> userList = new ArrayList<>();
        try {
            JsonReader reader = new JsonReader(new FileReader(file));
            Type type = new TypeToken<List<User>>() {}.getType();

            userList = gson.fromJson(reader, type);
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return userList;

    }

    public LocalUser loginUser(String path, String username, String password) throws Exception{
        LocalUser check = authUser(path, username, password);
        if(check != null){
            return check;
        }else{
            throw new AuthException("Failed to authenticate user " + username + ". Please check if the username or password is correct.");
        }
    }

    private LocalUser authUser(String path, String username, String password) {

        if(!authDir(path)){
            System.out.println("dir doesn't exist");
            return null;
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.registerTypeAdapter(User.class, new UserSerializer()).create();
        File file = new File(path + "/userData.json");
        LocalUser check = null;
        try {
            JsonReader reader = new JsonReader(new FileReader(file));
            Type type = new TypeToken<List<User>>() {}.getType();

            List<LocalUser> userList = gson.fromJson(reader, type);

            for (LocalUser u: userList){
                if(u.getUsername().equals(username)){
                    if(u.getPassword().equals(password)){
                        check = u;
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return check;
    }

    private boolean authDir(String path) {

        File file = new File(path);

        return file.exists() && file.isDirectory();
    }

}
