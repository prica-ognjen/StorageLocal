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
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Connection implements connection.Connection {

    private LocalStorage storage;
    private boolean connOn;
    private String currDir;

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
        Gson gson = gsonBuilder.registerTypeAdapter(Config.class, new ConfigSerializer()).create();

        List<LocalUser> userList = new ArrayList<>();
        Config config1 = null;

        try {
            JsonReader reader = new JsonReader(new FileReader(userData));
            Type type = new TypeToken<ArrayList<LocalUser>>() {}.getType();

            userList= gson.fromJson(reader, type);

            if(userList.isEmpty()){
                System.out.println("storage wasn't initialized so there are no users");
                return null;
            }

            reader = new JsonReader(new FileReader(config));
            type = new TypeToken<Config>(){}.getType();

            config1 = gson.fromJson(reader, type);


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
        currDir = to+"\\"+name;
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
    @SuppressWarnings("unchecked")
    public String createFile(String name, String path) {

        if(!connOn){
            return "fail: there is no active connection";
        }

        File file = new File(currDir+"\\"+path+"\\"+name);

        if(file.exists()){
            return "fail: file already exists.";
        }
        int size = 0;
        if((size + getFileSize(new File(storage.getPath()+"\\"+storage.getName())) > (Integer)storage.getConfig().getSizeLimit()) && (Integer)storage.getConfig().getSizeLimit() == -1){
            return "fail: file too large";
        }

        if((getFileNumber(new File(storage.getPath()+"\\"+storage.getName())) + 1 > (Integer)storage.getConfig().getFileNumLimit()) && (Integer)storage.getConfig().getFileNumLimit() != 1){
            System.out.println(getFileNumber(new File(storage.getPath()+"\\"+storage.getName())));
            return "fail: File number limit exceeded";
        }

        String extension = name.split("\\.")[1];

        if(storage.getConfig().getBlockedExtensions().contains(extension)){
            return "fail: Extension " +extension+ " not supported";
        }

        for(String s: storage.getConfig().getBlockedExtensions()){
            if(s.equals(extension)){
                return "fail: cannot create file with that extension as it is blocked by the storage";
            }
        }

        try {
            boolean created = file.createNewFile();

            if(!created){
                return "fail: error with creating a file";
            }else{
                return "success: file created";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    @Override
    public String createFile(String name) {

        if(!connOn){
            return "fail: there is no active connection";
        }

        if(!privilegeCheck(storage.getCurrUser(), "add")){
            return "fail: user doesn't have create privilege";
        }

        File file = new File(currDir+"\\"+name);

        if(file.exists()){
            return "fail: file already exists.";
        }
        String extension = "";
        if(name.contains("\\.")){
            extension = name.split("\\.")[1];
        }

        int size = 0;

        if((size + getFileSize(new File(storage.getPath()+"\\"+storage.getName())) > (Integer)storage.getConfig().getSizeLimit() && (Integer)storage.getConfig().getSizeLimit() == -1)){
            return "fail: file too large";
        }

        if((getFileNumber(new File(storage.getPath()+"\\"+storage.getName())) + 1 > (Integer)storage.getConfig().getFileNumLimit() && (Integer)storage.getConfig().getFileNumLimit() != 1)){
            System.out.println(getFileNumber(new File(storage.getPath()+"\\"+storage.getName())));
            return "fail: File number limit exceeded";
        }
        if(storage.getConfig().getBlockedExtensions().contains(extension)){
            return "fail: Extension " +extension+ " not supported";
        }
        if(name.contains("sizeTest")) {

            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(file));

                out.write(testSize(size));
                out.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        try {
            boolean created = file.createNewFile();

            if(!created && !name.contains("sizeTest")){
                return "fail: error with creating a file";
            }else{
                return "success: file created";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String testSize(int bytes){

        StringBuilder stringBuilder = new StringBuilder();

        for(int i = 0; i < bytes; i++){
            stringBuilder.append(i%10);
        }

        return stringBuilder.toString();

    }

    @Override
    public String createDir(String name, String path) {

        if(!connOn){
            return "fail: there is no active connection";
        }

        if(!privilegeCheck(storage.getCurrUser(), "add")){
            return "fail: user doesn't have create privilege";
        }

        File file = new File(currDir+"\\"+path + "\\" + name);

        if(file.exists()){
            return "fail: file already exists.";
        }

        boolean created = file.mkdir();

        if(!created){
            return "fail: error with creating a directory";
        }else{
            return "success: directory created";
        }
    }

    @Override
    public String createDir(String name) {

        if(!connOn){
            return "fail: there is no active connection";
        }

        if(!privilegeCheck(storage.getCurrUser(), "add")){
            return "fail: user doesn't have create privilege";
        }

        File file = new File(currDir+"\\"+name);

        if(file.exists()){
            return "fail: file already exists.";
        }

        boolean created = file.mkdir();

        if(!created){
            return "fail: error with creating a directory";
        }else{
            return "success: directory created";
        }

    }

    @Override
    public String delFile(String name) {

        if(!connOn){
            return "fail: there is no active connection";
        }

        if(!privilegeCheck(storage.getCurrUser(), "del")){
            return "fail: user doesn't have delete privilege";
        }

        File file = new File(currDir+"\\"+name);

        if(!file.exists()){
            return "fail: file doesn't exists.";
        }

        boolean deleted = file.delete();

        if(!deleted){
            return "fail: error with deleting a file";
        }else{
            return "success: file deleted";
        }

    }

    @Override
    public String delFile(String name, String path) {

        if(!connOn){
            return "fail: there is no active connection";
        }

        if(!privilegeCheck(storage.getCurrUser(), "del")){
            return "fail: user doesn't have delete privilege";
        }

        File file = new File(currDir+"\\"+path+"\\"+name);

        if(!file.exists()){
            return "fail: file doesn't exists.";
        }

        boolean deleted = file.delete();

        if(!deleted){
            return "fail: error with deleting a file";
        }else{
            return "success: file deleted";
        }
    }

    @Override
    public String delDir(String name) {
        if(!connOn){
            return "fail: there is no active connection";
        }

        if(!privilegeCheck(storage.getCurrUser(), "del")){
            return "fail: user doesn't have delete privilege";
        }

        File file = new File(currDir+"\\"+name);

        if(!deleteDirectory(file)){
            return "fail: there was a problem with deleting the directory";
        }

        return "success: directory deleted";
    }

    @Override
    public String delDir(String name, String path) {
        if(!connOn){
            return "fail: there is no active connection";
        }

        if(!privilegeCheck(storage.getCurrUser(), "del")){
            return "fail: user doesn't have delete privilege";
        }

        File file = new File(currDir+"\\"+path+"\\"+name);

        if(!deleteDirectory(file)){
            return "fail: there was a problem with deleting the directory";
        }

        return "success: directory deleted";
    }

    @Override
    public List<String> listDir(String name) throws Exception{

        if(!connOn){
            return null;
        }

        if(!privilegeCheck(storage.getCurrUser(), "view")){
            throw new Exception("fail: user doesn't have view privilege");
        }

        File f = new File(currDir+"\\"+name);

        File[] allContents = f.listFiles();

        List<String> list = new ArrayList<>();

        assert allContents != null;
        for(File file: allContents){
            list.add(file.getName());
        }

        return list;
    }

    @Override
    public List<String> listDir() throws Exception {
        if(!connOn){
            return null;
        }

        if(!privilegeCheck(storage.getCurrUser(), "view")){
            throw new Exception("fail: user doesn't have view privilege");
        }

        File f = new File(currDir);

        File[] allContents = f.listFiles();

        List<String> list = new ArrayList<>();

        assert allContents != null;
        for(File file: allContents){
            list.add(file.getName());
        }

        return list;
    }

    @Override
    public String movFile(String name, String to) throws Exception{

        if(!connOn){
            return "fail: there is no active connection";
        }

        if(!privilegeCheck(storage.getCurrUser(), "mov")){
            throw new Exception("fail: user doesn't have move privilege");
        }

        File f = new File(currDir+"\\"+name);

        Path source = Paths.get(f.getPath());
        Path target = Paths.get(currDir+"\\"+to+"\\"+f.getName());

        Files.move(source, target);

        return "success: file moved";
    }

    @Override
    public String movDir(String name, String to) throws Exception{

        if(!connOn){
            return "fail: there is no active connection";
        }
        if(!privilegeCheck(storage.getCurrUser(), "mov")){
            throw new Exception("fail: user doesn't have move privilege");
        }
        File f = new File(currDir+"\\"+name);

        File dest = new File(currDir+"\\"+to+"\\"+name);
        boolean createdDir = dest.mkdir();

        if(createdDir) {
            boolean moved = moveDirectory(f, dest);

            if(moved){
                return "success: moved directory to new location";
            }
        }
        return "fail: couldn't move directory";

    }

    @Override
    public String downloadFile(String name, String to) throws Exception{

        if(!connOn){
            return "fail: there is no active connection";
        }

        if(!privilegeCheck(storage.getCurrUser(), "dwn")){
            throw new Exception("fail: user doesn't have download privilege");
        }

        File f = new File(currDir+"\\"+name);
        File dest = null;
        if(f.isDirectory()){
            dest = new File(to+"\\"+name);
        }else{
            dest = new File(to+"\\"+f.getName());
        }

        boolean createdDir = false;

        if(f.isDirectory()) {
            createdDir = dest.mkdir();
        }
        else{
            try {
                boolean copied = copyDirectory(f, dest);
                if(copied){
                    return "success: downloaded file to new location";
                }else{
                    return "fail: couldn't download to file to wanted location";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(createdDir) {
            boolean copied = false;
            try {
                copied = copyDirectory(f, dest);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(copied){
                return "success: downloaded directory to new location";
            }
        }
        return "fail: couldn't download directory";
    }

    @Override
    public String limitStorageSize(String s, int i) throws Exception {
        if(!connOn){
            return "fail: there is no active connection";
        }
        if(!storage.getCurrUser().getType().equals(UserType.SUPER)){
            return "fail: user doesn't have superuser privileges";
        }

        File f = new File(s);

        if(getFileSize(f) > i){
            return "fail: cannot limit storage size because it's currently larger than the provided limit";
        }

        storage.getConfig().setSizeLimit(i);

        Config config = storage.getConfig();

        if(saveConfig(config).contains("success")){
            return "success: updated config.json";
        }else{
            return "fail: something went wrong when saving config.json";
        }
    }

    @Override
    public String limitFileNumber(String s, int i) throws Exception {
        if(!connOn){
            return "fail: there is no active connection";
        }
        if(!storage.getCurrUser().getType().equals(UserType.SUPER)){
            return "fail: user doesn't have superuser privileges";
        }

        storage.getConfig().setFileNumLimit(i);
        Config config = storage.getConfig();
        if(saveConfig(config).contains("success")){
            return "success: updated config.json";
        }else{
            return "fail: something went wrong when saving config.json";
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String blockExtForStorage(String s, String s1) throws Exception {
        if(!connOn){
            return "fail: there is no active connection";
        }
        if(!storage.getCurrUser().getType().equals(UserType.SUPER)){
            return "fail: user doesn't have superuser privileges";
        }

        storage.getConfig().getBlockedExtensions().add(s1);

        storage.getConfig().setBlockedExtensions(storage.getConfig().getBlockedExtensions());
        Config config = storage.getConfig();
        if(saveConfig(config).contains("success")){
            return "success: updated config.json";
        }else{
            return "fail: something went wrong when saving config.json";
        }
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

    private String saveConfig(Config config) {
        File file = new File(storage.getPath()+"/"+storage.getName() + "/config.json");
        try {
            if(!file.exists()){
                return "fail: config doesn't exist.";
            }else{
                FileWriter out = new FileWriter(file);
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.setPrettyPrinting();
                Gson gson = gsonBuilder.create();
                out.write(gson.toJson(config));
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "success: config updated.";
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

    private boolean privilegeCheck(User currUser, String code) {

        if(code.equals("add")){

            return currUser.getPrivileges().get(Privilege.ADD);

        }
        if(code.equals("del")){

            return currUser.getPrivileges().get(Privilege.DEL);

        }
        if(code.equals("view")){

            return currUser.getPrivileges().get(Privilege.VIEW);

        }
        if(code.equals("mov")){

            return currUser.getPrivileges().get(Privilege.MOV);

        }
        if(code.equals("dwn")){

            return currUser.getPrivileges().get(Privilege.DWN);

        }

        return false;
    }

    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private static boolean moveDirectory(File sourceFile, File destFile) {

        if (sourceFile.isDirectory()) {
            File[] files = sourceFile.listFiles();

            assert files != null;
            for (File file : files)
                moveDirectory(file, new File(destFile, file.getName()));

            return sourceFile.delete();

        } else {
            if (!destFile.getParentFile().exists())
                if (!destFile.getParentFile().mkdirs())
                    return false;
            return sourceFile.renameTo(destFile);
        }
    }

    private static boolean copyDirectory(File sourceFile, File destFile) throws IOException {
        if (sourceFile.isDirectory()) {
            copyDirectoryRecursively(sourceFile, destFile);
        } else {
            Files.copy(sourceFile.toPath(), destFile.toPath());
        }
        return true;
    }
    // recursive method to copy directory and sub-diretory in Java
    private static void copyDirectoryRecursively(File source, File target)
            throws IOException {
        if (!target.exists()) {
            target.mkdir();
        }

        for (String child : Objects.requireNonNull(source.list())) {
            copyDirectory(new File(source, child), new File(target, child));
        }
    }

    private static long getFileSize(File dir) {
        long fileSize = 0;

        if (dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isFile()) {
                    fileSize += file.length();
                } else
                    fileSize += getFileSize(file);
            }
        } else if (dir.isFile()) {
            fileSize += dir.length();
        }
        return fileSize;
    }
    private static long getFileNumber(File dir) {
        long fileNum = 0;

        if (dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isFile()) {
                    fileNum += 1;
                } else
                    fileNum += getFileNumber(file);
            }
        } else if (dir.isFile()) {
            fileNum += 0;
        }
        return fileNum;
    }

}
