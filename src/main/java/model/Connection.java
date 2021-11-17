package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import exceptions.AuthException;
import exceptions.ConnectionException;
import storage.Storage;
import user.FileType;
import user.Privilege;
import user.User;
import user.UserType;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;

public class Connection implements connection.Connection {

    private LocalStorage storage;
    private boolean connOn;
    private String currDir;

    @Override
    public Storage connect(String dest, String username, String password) {

        //dest je ceo path za storage

        File root = new File(dest);
        String name = root.getName();
        String path = root.getParent();

        if (!checkIfStorageValid(root)) {
            return null;
        }

        File userData = new File(root.getPath() + "/userData.json");
        File config = new File(root.getPath() + "/config.json");

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        List<LocalUser> userList = new ArrayList<>();
        Config configJSON = null;

        try {
            JsonReader reader = new JsonReader(new FileReader(userData));
            Type type = new TypeToken<ArrayList<LocalUser>>() {
            }.getType();
            userList = gson.fromJson(reader, type);
            if (userList.isEmpty()) {
                throw new ConnectionException("Storage was not properly initialized. UserData.json isn't properly initialized.");
            }

            reader = new JsonReader(new FileReader(config));
            type = new TypeToken<Config>() {
            }.getType();
            configJSON = gson.fromJson(reader, type);
            if (configJSON == null) {
                throw new ConnectionException("Storage was not properly initialized. Config.json isn't properly initialized.");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //dir exists
        //userData exists and isn't empty
        //config exists
        LocalUser user = loginUser(dest, username, password);
        //auth successful
        getStorageConnection(path, name, userList, configJSON, user);
        connOn = true;
        currDir = path + "/" + name;
        return storage;
    }

    @Override
    public Storage connect(String s, User user) {
        return connect(s, user.getUsername(), user.getPassword());
    }

    @Override
    public void close() {
        try {
            testConnection();
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
        this.connOn = false;
        this.storage = null;
    }

    @Override
    public void closeForUser() {
        try {
            testConnection();
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
        this.connOn = false;
    }

    @Override
    public String cd(String path){

        if(path.equals("..")){

            File file = new File(currDir);

            if(file.isDirectory()){
                currDir = file.getParentFile().getPath();
            }

            String str;

            int storName = currDir.indexOf(storage.getName());

            str = currDir.substring(storName);

            str = str.replace("\\", "/");

            return str;
        }

        File file = new File(currDir + "/" + path);

        if(file.isDirectory()){
            currDir = currDir + "/" + path;
        }

        String str;

        int storName = currDir.indexOf(storage.getName());

        str = currDir.substring(storName);

        return str;
    }

    @Override
    public boolean addUser(String username, String password, UserType type) {
        try {
            testConnection();
            testSuperUser();
            testSuperAdd(type);
            testUsername(username);
            testUserTypeError(type);
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        List<LocalUser> tempUsers = getUserData();

        Map<Privilege, Boolean> map = new HashMap<>();

        map.put(Privilege.VIEW, false);
        map.put(Privilege.ADD, false);
        map.put(Privilege.DWN, false);
        map.put(Privilege.MOV, false);
        map.put(Privilege.DEL, false);

        LocalUser u = new LocalUser(username, password, type, map);

        tempUsers.add(u);
        saveUserData(tempUsers);
        return true;
    }

    @Override
    public boolean delUser(String username) {

        try {
            testConnection();
            testSuperUser();
            testUsernameNotFound(username);
            testSuperSelfDel(username);
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        List<LocalUser> tempUsers = getUserData();
        LocalUser toDel = null;

        for (LocalUser u : tempUsers) {
            if (u.getUsername().equals(username)) {
                toDel = u;
                break;
            }
        }
        tempUsers.remove(toDel);
        saveUserData(tempUsers);
        return true;
    }

    @Override
    public boolean addPrivilege(String username, Privilege privilege, String path) {
        DirConfig dirConfig;

        List<Map<String, DirConfig>> list = storage.getConfig().getFileConfigMap();

        String s1;

        for (Map<String, DirConfig> stringDirConfigMap : list) {

            s1 = storage.getPath() + "/" + storage.getName() + "/" + path;

            s1 = s1.replace("/", "\\");

            if (stringDirConfigMap.get(s1) != null) {
                for (Map.Entry<String, DirConfig> e : stringDirConfigMap.entrySet()) {
                    if (e.getValue() == null) {
                        dirConfig = new DirConfig();

                    } else {
                        dirConfig = e.getValue();
                    }
                    Map<String, List<Privilege>> inMap = dirConfig.getBlockedPrivileges();


                    if (inMap == null) {
                        inMap = new HashMap<>();
                    }

                    boolean check = false;

                    for (Map.Entry<String, List<Privilege>> lp : inMap.entrySet()) {

                        if (lp.getKey().equals(username)) {

                            List<Privilege> tList = lp.getValue();
                            if (tList == null) {
                                tList = new ArrayList<>();
                            }

                            tList.remove(privilege);

                            lp.setValue(tList);
                            check = true;
                            break;
                        }

                    }
                    if (!check) {
                        List<Privilege> tList = new ArrayList<>();
                        inMap.put(username, tList);
                    }
                    e.setValue(dirConfig);
                }
                break;
            } else if (stringDirConfigMap.get(s1.replace("\\", "/")) != null) {
                for (Map.Entry<String, DirConfig> e : stringDirConfigMap.entrySet()) {
                    if (e.getValue() == null) {
                        dirConfig = new DirConfig();

                    } else {
                        dirConfig = e.getValue();
                    }
                    Map<String, List<Privilege>> inMap = dirConfig.getBlockedPrivileges();

                    if (inMap == null) {
                        inMap = new HashMap<>();
                    }

                    for (Map.Entry<String, List<Privilege>> lp : inMap.entrySet()) {

                        if (lp.getKey().equals(username)) {
                            List<Privilege> tList = lp.getValue();
                            if (tList == null) {
                                tList = new ArrayList<>();
                            }

                            tList.remove(privilege);
                            break;
                        }

                    }

                    e.setValue(dirConfig);
                }
                break;
            }
        }

        saveConfig(storage.getConfig());
        return true;
    }

    @Override
    public boolean addPrivilege(String username, Privilege privilege) {
        //VIEW,Add,Dwn,mov,DEL -> mislim da je ovako ok

        try {
            testConnection();
            testSuperUser();
            testUsernameNotFound(username);
            checkIfArgIsNull(privilege);
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        List<LocalUser> tempUsers = getUserData();

        for (LocalUser u : tempUsers) {
            if (u.getUsername().equals(username)) {
                for(Privilege p: privLevelAdd(privilege)){
                    u.getPrivileges().put(p, true);
                }
                u.getPrivileges().put(privilege, true);
                break;
            }
        }
        saveUserData(tempUsers);
        return true;
    }

    @Override
    public boolean delPrivilege(String username, Privilege privilege, String path) {
        try {
            testConnection();
            testSuperUser();
            testUsernameNotFound(username);
            checkIfArgIsNull(privilege);
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        DirConfig dirConfig;

        List<Map<String, DirConfig>> list = storage.getConfig().getFileConfigMap();

        String s1;

        for (Map<String, DirConfig> stringDirConfigMap : list) {

            s1 = storage.getPath() + "/" + storage.getName() + "/" + path;

            s1 = s1.replace("/", "\\");

            if (stringDirConfigMap.get(s1) != null) {
                for (Map.Entry<String, DirConfig> e : stringDirConfigMap.entrySet()) {
                    if (e.getValue() == null) {
                        dirConfig = new DirConfig();

                    } else {
                        dirConfig = e.getValue();
                    }
                    Map<String, List<Privilege>> inMap = dirConfig.getBlockedPrivileges();


                    if (inMap == null) {
                        inMap = new HashMap<>();
                    }

                    boolean check = false;

                    for (Map.Entry<String, List<Privilege>> lp : inMap.entrySet()) {

                        if (lp.getKey().equals(username)) {

                            List<Privilege> tList = lp.getValue();
                            if (tList == null) {
                                tList = new ArrayList<>();
                            }
                            if(!tList.contains(privilege))
                                tList.add(privilege);

                            lp.setValue(tList);
                            check = true;
                            break;
                        }

                    }
                    if (!check) {
                        List<Privilege> tList = new ArrayList<>();
                        tList.add(privilege);
                        inMap.put(username, tList);
                    }
                    e.setValue(dirConfig);
                }
                break;
            } else if (stringDirConfigMap.get(s1.replace("\\", "/")) != null) {
                for (Map.Entry<String, DirConfig> e : stringDirConfigMap.entrySet()) {
                    if (e.getValue() == null) {
                        dirConfig = new DirConfig();

                    } else {
                        dirConfig = e.getValue();
                    }
                    Map<String, List<Privilege>> inMap = dirConfig.getBlockedPrivileges();

                    if (inMap == null) {
                        inMap = new HashMap<>();
                    }

                    for (Map.Entry<String, List<Privilege>> lp : inMap.entrySet()) {

                        if (lp.getKey().equals(username)) {
                            List<Privilege> tList = lp.getValue();
                            if (tList == null) {
                                tList = new ArrayList<>();
                            }

                            tList.add(privilege);
                            break;
                        }

                    }

                    e.setValue(dirConfig);
                }
                break;
            }
        }

        saveConfig(storage.getConfig());
        return true;
    }

    @Override
    public boolean delPrivilege(String username, Privilege privilege) {
        try {
            testConnection();
            testSuperUser();
            testUsernameNotFound(username);
            checkIfArgIsNull(privilege);
            testUserForPrivilege(privilege, username);
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        LocalUser tUser = findUser(username);

        assert tUser != null;
        List<LocalUser> tempUsers = getUserData();

        for (LocalUser u : tempUsers) {
            if (u.getUsername().equals(username)) {
                for(Privilege p: privLevelRm(privilege)){
                    u.getPrivileges().put(p, false);
                }
                break;
            }
        }
        saveUserData(tempUsers);
        return true;
    }

    @Override
    public boolean importFile(String s, String s1) {

        try {
            File f = new File(s);
            if(s1.equals(storage.getName())){
                //dosmth
            }

            testConnection();
            testPrivilege("add");
            testFileNum();
            testStorageSize(s);
            //testIfFileExists();

            if(!f.getParentFile().getName().equals(storage.getName())){
                testPrivForDir(storage.getCurrUser().getUsername(), f.getParentFile(), Privilege.ADD);
            }
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        File f = new File(s);

        if(f.exists())
            if(f.isDirectory()){
                String root = storage.getPath() + "/" + storage.getName();

                boolean created = false;

                if(!s1.equals(storage.getName()))
                    try {
                        created = copyDirectory(f, new File(root + "/" + s1));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                else
                    try {
                        created = copyDirectory(f, new File(root +"/"+ f.getName()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if(!created){
                        unknownError();
                    }

                    HashMap<String, DirConfig> map = new HashMap<>();
                    map.put(f.getPath(), new DirConfig());

                    storage.getConfig().getFileConfigMap().add(map);
                    saveConfig(storage.getConfig());

                return true;
            }else{

                String root = storage.getPath() + "/" + storage.getName();
                File f2;
                if(!s1.equals(storage.getName())) {
                    f2 = new File(root + "/" + s1 + f.getName());
                }else{
                    f2 = new File(root + "/" + f.getName());
                }

                boolean created = false;

                try {
                    created = f2.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(!created){
                    unknownError();
                }
                return true;
            }


        return false;
    }

    @Override
    public boolean mk(String name, FileType type) {

        try {
            testConnection();
            testPrivilege("add");
            testBlockedExtensions(name);
            testFileNum();
            testStorageSize(currDir + "/" + name);
            testIfFileExists(currDir + "/" + name);

            File f = new File(currDir + "/" + name);

            if(!f.getParentFile().getName().equals(storage.getName())){
                testPrivForDir(storage.getCurrUser().getUsername(), f.getParentFile(), Privilege.ADD);
            }

        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        File file = new File(currDir + "/" + name);

        try {
            boolean created;
            if (type == FileType.FILE) {
                created = file.createNewFile();
            } else {
                created = file.mkdir();

                HashMap<String, DirConfig> map = new HashMap<>();
                map.put(file.getPath(), new DirConfig());

                storage.getConfig().getFileConfigMap().add(map);
                saveConfig(storage.getConfig());
            }

            if (!created) {
                unknownError();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean rm(String name) {
        try {
            testConnection();
            testPrivilege("del");

            File f = new File(currDir + "/" + name);

            if(!f.getParentFile().getName().equals(storage.getName())){
                testPrivForDir(storage.getCurrUser().getUsername(), f.getParentFile(), Privilege.DEL);
            }

        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        File file = new File(currDir + "/" + name);

        if (!deleteDirectory(file)) {
            unknownError();
        }
        return true;
    }

    @Override
    public List<String> ls() {
        try {
            testConnection();
            testPrivilege("view");

            File f = new File(currDir);

            if (!f.getName().equals(storage.getName())) {
                //System.out.println("uslo");
                testPrivForDir(storage.getCurrUser().getUsername(), f, Privilege.VIEW);
            }

        } catch (ConnectionException e) {
            e.printStackTrace();
            return null;
        }

        File f = new File(currDir);

        File[] allContents = f.listFiles();

        List<String> list = new ArrayList<>();

        assert allContents != null;
        for (File file : allContents) {
            list.add(file.getName());
        }

        return list;
    }

    @Override
    public List<String> lsFiles(String s) {
        try {
            testConnection();
            testPrivilege("view");
            File f = new File(currDir + "/" + s);

            if(!f.getParentFile().getName().equals(storage.getName())){
                testPrivForDir(storage.getCurrUser().getUsername(), f.getParentFile(), Privilege.VIEW);
            }
        } catch (ConnectionException e) {
            e.printStackTrace();
            return null;
        }

        File f = new File(currDir + "/" + s);

        File[] allContents = f.listFiles();

        List<String> list = new ArrayList<>();

        assert allContents != null;
        for (File file : allContents) {
            if (!file.isDirectory())
                list.add(file.getName());
        }

        return list;
    }

    @Override
    public List<String> lsDirs(String s) {
        try {
            testConnection();
            testPrivilege("view");
            File f = new File(currDir + "/" + s);

            if(!f.getParentFile().getName().equals(storage.getName())){
                testPrivForDir(storage.getCurrUser().getUsername(), f.getParentFile(), Privilege.VIEW);
            }
        } catch (ConnectionException e) {
            e.printStackTrace();
            return null;
        }

        File f = new File(currDir + "/" + s);

        File[] allContents = f.listFiles();

        List<String> list = new ArrayList<>();

        assert allContents != null;
        for (File file : allContents) {
            if (file.isDirectory())
                list.add(file.getName());
        }

        return list;
    }

    @Override
    public List<String> lsExt(String s) {
        try {
            testConnection();
            testPrivilege("view");
            File f = new File(currDir + "/" + s);

            if(!f.getParentFile().getName().equals(storage.getName())){
                testPrivForDir(storage.getCurrUser().getUsername(), f.getParentFile(), Privilege.VIEW);
            }
        } catch (ConnectionException e) {
            e.printStackTrace();
            return null;
        }

        File f = new File(currDir);

        File[] allContents = f.listFiles();

        List<String> list = new ArrayList<>();

        assert allContents != null;
        for (File file : allContents) {
            if (file.getName().contains("."))
                if (file.getName().split("\\.")[1].equals(s))
                    list.add(file.getName());
        }

        return list;
    }

    @Override
    public List<String> lsDateCreated(String s) {
        try {
            testConnection();
            testPrivilege("view");
            File f = new File(currDir + "/" + s);

            if(!f.getParentFile().getName().equals(storage.getName())){
                testPrivForDir(storage.getCurrUser().getUsername(), f.getParentFile(), Privilege.VIEW);
            }
        } catch (ConnectionException e) {
            e.printStackTrace();
            return null;
        }

        File f = new File(currDir);

        File[] allContents = f.listFiles();

        List<String> list = new ArrayList<>();

        assert allContents != null;
        Arrays.sort(allContents, Comparator.comparingLong(File::lastModified).reversed());

        for (File file : allContents) {
            list.add(file.getName());
        }

        return list;
    }

    @Override
    public List<String> lsDateModified(String s) {
        return null;
    }

    @Override
    @SuppressWarnings("all")
    public boolean movFile(String name, String to) {
        try {
            testConnection();
            testPrivilege("mov");
            File f = new File(currDir + "/" + name);

            if(!f.getParentFile().getName().equals(storage.getName())){
                testPrivForDir(storage.getCurrUser().getUsername(), f.getParentFile(), Privilege.MOV);
            }
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        File f = new File(currDir + "/" + name);
        File dest;


        if (to.equals(storage.getName())) {
            dest = new File(storage.getPath() + "/" + storage.getName() + "/" + f.getName());
        } else {
            dest = new File(storage.getPath() + "/" + storage.getName() + "/" + to + "/" + name);
        }

        if (f.isDirectory())
            dest.mkdir();

        if (!moveDirectory(f, dest)) {
            unknownError();
        }
        return true;
    }

    @Override
    public boolean dwnFile(String name, String to) {

        try {
            testConnection();
            testPrivilege("dwn");
            File f = new File(currDir + "/" + name);

            if(!f.getParentFile().getName().equals(storage.getName())){
                testPrivForDir(storage.getCurrUser().getUsername(), f.getParentFile(), Privilege.DWN);
            }
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        File f = new File(currDir + "/" + name);
        File dest;
        if (f.isDirectory()) {
            dest = new File(to + "/" + name);
        } else {
            dest = new File(to + "/" + f.getName());
        }

        boolean createdDir = false;

        if (f.isDirectory()) {
            createdDir = dest.mkdir();
        } else {
            try {
                boolean copied = copyDirectory(f, dest);
                if (!copied) {
                    unknownError();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (createdDir) {
            boolean copied;
            try {
                copied = copyDirectory(f, dest);
                if (!copied) {
                    unknownError();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public boolean limitSize(int i) {
        try {
            testConnection();
            testSuperUser();
            isLimitLow(storage.getPath() + "/" + storage.getName(), i, "root");
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        storage.getConfig().setSizeLimit(i);
        Config config = storage.getConfig();
        saveConfig(config);
        return true;
    }

    @Override
    public boolean limitSize(String s, int size) {
        try {
            testConnection();
            testSuperUser();
            isLimitLow(storage.getPath() + "/" + storage.getName() + "/" + s, size, "reg");
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        DirConfig dirConfig;

        List<Map<String, DirConfig>> list = storage.getConfig().getFileConfigMap();

        String s1;

        for (Map<String, DirConfig> stringDirConfigMap : list) {

            s1 = storage.getPath() + "/" + storage.getName() + "/" + s;

            s1 = s1.replace("/", "\\");

            if (stringDirConfigMap.get(s1) != null) {
                for (Map.Entry<String, DirConfig> e : stringDirConfigMap.entrySet()) {
                    if (e.getValue() == null) {
                        dirConfig = new DirConfig();

                    } else {
                        dirConfig = e.getValue();
                    }
                    dirConfig.setSizeLimit(size);
                    e.setValue(dirConfig);
                }
                break;
            } else if (stringDirConfigMap.get(s1.replace("\\", "/")) != null) {
                for (Map.Entry<String, DirConfig> e : stringDirConfigMap.entrySet()) {
                    if (e.getValue() == null) {
                        dirConfig = new DirConfig();

                    } else {
                        dirConfig = e.getValue();
                    }
                    dirConfig.setSizeLimit(size);
                    e.setValue(dirConfig);
                }
                break;
            }
        }

        saveConfig(storage.getConfig());
        return true;
    }

    @Override
    public boolean limitFileNum(int i) {
        try {
            testConnection();
            testSuperUser();
            testFileNum(); //mod
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        storage.getConfig().setFileNumLimit(i);
        Config config = storage.getConfig();
        saveConfig(config);
        return true;
    }

    @Override
    public boolean limitFileNum(String s, int num) {
        //ovo treba za neki odredjeni dir
        try {
            testConnection();
            testSuperUser();
            testFileNum(); // treba za vise directory-ja da se modifikuje
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        DirConfig dirConfig;

        List<Map<String, DirConfig>> list = storage.getConfig().getFileConfigMap();

        String s1;

        for (Map<String, DirConfig> stringDirConfigMap : list) {

            s1 = storage.getPath() + "/" + storage.getName() + "/" + s;

            s1 = s1.replace("/", "\\");

            if (stringDirConfigMap.get(s1) != null) {
                for (Map.Entry<String, DirConfig> e : stringDirConfigMap.entrySet()) {
                    if (e.getValue() == null) {
                        dirConfig = new DirConfig();

                    } else {
                        dirConfig = e.getValue();
                    }

                    dirConfig.setFileNumLimit(num);
                    e.setValue(dirConfig);
                }
                break;
            } else if (stringDirConfigMap.get(s1.replace("\\", "/")) != null) {
                for (Map.Entry<String, DirConfig> e : stringDirConfigMap.entrySet()) {
                    if (e.getValue() == null) {
                        dirConfig = new DirConfig();

                    } else {
                        dirConfig = e.getValue();
                    }
                    dirConfig.setFileNumLimit(num);
                    e.setValue(dirConfig);
                }
                break;
            }
        }

        saveConfig(storage.getConfig());
        return true;
    }

    @Override
    public boolean unblockExt(String s, String s1) throws Exception {
        try {
            testConnection();
            testSuperUser();
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        storage.getConfig().getBlockedExtensions().remove(s);
        Config config = storage.getConfig();
        saveConfig(config);
        return true;
    }

    @Override
    public boolean unblockExt(String s) throws Exception {
        try {
            testConnection();
            testSuperUser();
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        storage.getConfig().getBlockedExtensions().remove(s);
        Config config = storage.getConfig();
        saveConfig(config);
        return true;
    }

    @Override
    public boolean blockExt(String s) {
        try {
            testConnection();
            testSuperUser();
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        storage.getConfig().getBlockedExtensions().add(s);
        Config config = storage.getConfig();
        saveConfig(config);
        return true;
    }

    @Override
    public boolean blockExt(String s, String s1) {
        //ovo treba za neki odredjeni dir
        try {
            testConnection();
            testSuperUser();
        } catch (ConnectionException e) {
            e.printStackTrace();
            return false;
        }

        storage.getConfig().getBlockedExtensions().add(s1);

        Config config = storage.getConfig();
        saveConfig(config);
        return true;
    }

    //private methods

    private LocalUser findUser(String name) {

        List<LocalUser> tempUsers = getUserData();
        for (LocalUser u : tempUsers) {
            if (u.getUsername().equals(name)) {
                return u;
            }
        }
        return null;
    }

    private void saveConfig(Config config) {
        File file = new File(storage.getPath() + "/" + storage.getName() + "/config.json");
        try {
            if (!file.exists()) {
                throw new ConnectionException("Config doesn't exist");
            } else {
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
    }

    private void saveUserData(List<LocalUser> tempUsers) {
        File file = new File(storage.getPath() + "/" + storage.getName() + "/userData.json");
        try {
            if (!file.exists()) {
                throw new ConnectionException("UserData doesn't exist");
            } else {
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
    }

    private List<LocalUser> getUserData() {

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.registerTypeAdapter(User.class, new UserSerializer()).create();
        File file = new File(storage.getPath() + "/" + storage.getName() + "/userData.json");
        List<LocalUser> userList = new ArrayList<>();
        try {
            JsonReader reader = new JsonReader(new FileReader(file));
            Type type = new TypeToken<List<User>>() {
            }.getType();

            userList = gson.fromJson(reader, type);
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return userList;

    }

    private LocalUser loginUser(String path, String username, String password) {
        LocalUser check = authUser(path, username, password);
        if (check != null) {
            return check;
        } else {
            try {
                failedAuth();
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @SuppressWarnings("all")
    private boolean checkIfStorageValid(File root) {

        if (root.exists() && !root.isDirectory()) {
            return false;
        }
        File userData = new File(root.getPath() + "/userData.json");
        if (!userData.exists() || userData.isDirectory()) {
            return false;
        }
        File config = new File(root.getPath() + "/config.json");
        if (!config.exists() || config.isDirectory()) {
            return false;
        }
        return true;
    }

    private void getStorageConnection(String to, String name, List<LocalUser> userList, Config config, LocalUser user) {

        LocalStorage s = new LocalStorage(to, name, userList.get(0));
        s.setConfig(config);
        s.setUsers(userList);

        if (s.getCurrUser() != null) {
            try {
                takenConn();
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
        }

        s.setCurrUser(user);
        storage = s;
    }

    private LocalUser authUser(String path, String username, String password) {

        if (!authDir(path)) {
            throw new ConnectionException("Storage doesn't exist");
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.registerTypeAdapter(User.class, new UserSerializer()).create();
        File file = new File(path + "/userData.json");
        LocalUser check = null;
        try {
            JsonReader reader = new JsonReader(new FileReader(file));
            Type type = new TypeToken<List<User>>() {
            }.getType();

            List<LocalUser> userList = gson.fromJson(reader, type);

            for (LocalUser u : userList) {
                if (u.getUsername().equals(username)) {
                    if (u.getPassword().equals(password)) {
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

        boolean b = true;

        if (code.equals("add")) {
            b = currUser.getPrivileges().get(Privilege.ADD);
        }
        if (code.equals("del")) {
            b = currUser.getPrivileges().get(Privilege.DEL);
        }
        if (code.equals("view")) {
            b = currUser.getPrivileges().get(Privilege.VIEW);
        }
        if (code.equals("mov")) {
            b = currUser.getPrivileges().get(Privilege.MOV);
        }
        if (code.equals("dwn")) {
            b = currUser.getPrivileges().get(Privilege.DWN);
        }

        return !b;

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

    @SuppressWarnings("all")
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

    //exceptions

    private void testConnection() {
        if (!connOn) {
            throw new ConnectionException("No active connection.");
        }
    }

    private void testSuperUser() {
        if (!storage.getCurrUser().getType().equals(UserType.SUPER)) {
            throw new ConnectionException("User isn't superuser");
        }
    }

    private void testSuperAdd(UserType type) {
        if (type.equals(UserType.SUPER)) {
            throw new ConnectionException("Superuser can only create regular users");
        }
    }

    private void testUsername(String username) {
        if (findUser(username) != null) {
            throw new ConnectionException("Username taken");
        }
    }

    private void testUsernameNotFound(String username) {
        if (findUser(username) == null) {
            throw new ConnectionException("User not found");
        }
    }

    private void testUserTypeError(UserType type) {
        if (!type.equals(UserType.REGULAR)) {
            throw new ConnectionException("Usertype must be either SUPER or REGULAR");
        }
    }

    private void testSuperSelfDel(String username) {
        if (Objects.requireNonNull(findUser(username)).getUsername().equals(storage.getCurrUser().getUsername())) {
            throw new ConnectionException("Superuser cannot delete itself");
        }
    }

    private void testPrivilege(String p) {
        if (privilegeCheck(storage.getCurrUser(), p)) {
            throw new ConnectionException("User doesn't have privilege for: " + p);
        }
    }

    private void testPrivForDir(String username, File file, Privilege p) {

        String parentDirName = file.getPath();

        DirConfig dirConfig = getDirConfig(parentDirName);
        if(dirConfig == null){
            System.out.println("PLSNO");
        }else{

            List<Privilege> list = getFromDirConfig(username, dirConfig);
            if(list.contains(p)){
                throw new ConnectionException("User doesn't have privilege" + p + " for " + parentDirName);
            }

        }
    }

    private List<Privilege> getFromDirConfig(String user, DirConfig dirConfig){

        Map<String, List<Privilege>> inMap = dirConfig.getBlockedPrivileges();

        if (inMap == null) {
            inMap = new HashMap<>();
        }

        for (Map.Entry<String, List<Privilege>> lp : inMap.entrySet()) {

            if (lp.getKey().equals(user)) {

                List<Privilege> tList = lp.getValue();
                if (tList == null) {
                    tList = new ArrayList<>();
                }
                return tList;
            }

        }
        return new ArrayList<>();
    }

    private DirConfig getDirConfig(String s) {
        DirConfig dirConfig;

        List<Map<String, DirConfig>> list = storage.getConfig().getFileConfigMap();

        String s1;
        for (Map<String, DirConfig> stringDirConfigMap : list) {

            s1 = s;

            s1 = s1.replace("/", "\\");

            if (stringDirConfigMap.get(s1) != null) {
                for (Map.Entry<String, DirConfig> e : stringDirConfigMap.entrySet()) {
                    if (e.getValue() == null) {
                        dirConfig = new DirConfig();

                    } else {
                        dirConfig = e.getValue();
                    }
                    return dirConfig;
                }
            } else if (stringDirConfigMap.get(s1.replace("\\", "/")) != null) {
                for (Map.Entry<String, DirConfig> e : stringDirConfigMap.entrySet()) {
                    if (e.getValue() == null) {
                        dirConfig = new DirConfig();

                    } else {
                        dirConfig = e.getValue();
                    }
                    return dirConfig;
                }
                break;
            }
        }
        return null;

    }


    private void checkIfArgIsNull(Object arg) {
        if (arg == null) {
            throw new ConnectionException("Argument is null");
        }
    }

    private void testUserForPrivilege(Privilege privilege, String username) {
        try {
            testUsernameNotFound(username);
        } catch (ConnectionException e) {
            e.printStackTrace();
            return;
        }
        LocalUser tUser = findUser(username);
        assert tUser != null;
        if (!tUser.getPrivileges().get(privilege)) {
            throw new ConnectionException("User doesn't have this privilege");
        }
    }

    private void testFileNum() {
        if ((getFileNumber(new File(storage.getPath() + "/" + storage.getName())) + 1 > storage.getConfig().getFileNumLimit()) && storage.getConfig().getFileNumLimit() != -1) {
            throw new ConnectionException("Exceeded file number limit.");
        }
    }

    private void testStorageSize(String s) {

        File file = new File(s);

        long size = getFileSize(file);
        if ((size + getFileSize(new File(storage.getPath() + "/" + storage.getName())) > storage.getConfig().getSizeLimit()) && storage.getConfig().getSizeLimit() != -1) {
            throw new ConnectionException("Exceeded storage size limit.");
        }
    }

    private void testBlockedExtensions(String name) {
        String extension = "";
        if (name.contains(".")) {
            extension = name.split("\\.")[1];
        }

        if (storage.getConfig().getBlockedExtensions().contains(extension)) {
            throw new ConnectionException("Extension not supported by storage");
        }
    }

    private void testIfFileExists(String s) {
        if (new File(s).exists()) {
            throw new ConnectionException("File with same name already exists.");
        }
    }

    private void unknownError() {
        throw new ConnectionException("Unknown error happened");
    }

    private void isLimitLow(String s, int i, String root) {
        if (root.equals("root")) {
            long rootSize = getFileSize(new File(s));
            long userData = getFileSize(new File(s + "/userData.json"));
            long config = getFileSize(new File(s + "/config.json"));

            if (rootSize - userData - config > i) {
                throw new ConnectionException("Cannot limit storage size because it's currently larger than the provided limit");
            }
        } else {
            if (getFileSize(new File(s)) > i) {
                throw new ConnectionException("Cannot limit storage size because it's currently larger than the provided limit");
            }
        }
    }

    private void failedAuth() {
        throw new AuthException("Failed to authenticate user. Please check your username or password.");
    }

    private void takenConn() {
        throw new ConnectionException("Another user is currently connected.");
    }

    private List<Privilege> privLevelAdd(Privilege p){
        //VIEW,Add,Dwn,mov,DEL -> mislim da je ovako ok

        int level = 0;

        if(p.equals(Privilege.VIEW))
            level = 1;

        if(p.equals(Privilege.ADD))
            level = 2;

        if(p.equals(Privilege.DWN))
            level = 3;

        if(p.equals(Privilege.MOV))
            level = 4;

        if(p.equals(Privilege.DEL))
            level = 5;

        List<Privilege> list = new ArrayList<>();

        for(int i = 0; i < level; i++){
            if(i == 0)
                list.add(Privilege.VIEW);
            if(i == 1)
                list.add(Privilege.ADD);
            if(i == 2)
                list.add(Privilege.DWN);
            if(i == 3)
                list.add(Privilege.MOV);
            if(i == 4)
                list.add(Privilege.DEL);
        }

        return list;
    }
    private List<Privilege> privLevelRm(Privilege p){
        //VIEW,Add,Dwn,mov,DEL -> mislim da je ovako ok

        int level = 0;

        if(p.equals(Privilege.VIEW))
            level = 5;

        if(p.equals(Privilege.ADD))
            level = 4;

        if(p.equals(Privilege.DWN))
            level = 3;

        if(p.equals(Privilege.MOV))
            level = 2;

        if(p.equals(Privilege.DEL))
            level = 1;

        List<Privilege> list = new ArrayList<>();

        for(int i = 0; i < level; i++){
            if(i == 0)
                list.add(Privilege.DEL);
            if(i == 1)
                list.add(Privilege.MOV);
            if(i == 2)
                list.add(Privilege.DWN);
            if(i == 3)
                list.add(Privilege.ADD);
            if(i == 4)
                list.add(Privilege.VIEW);
        }

        return list;
    }

}
