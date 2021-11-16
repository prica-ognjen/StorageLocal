package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import exceptions.StorageInitException;
import exceptions.UserTypeException;
import storage.Storage;
import user.Privilege;
import user.User;
import user.UserManager;
import user.UserType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
    public String initStorage(String path) throws Exception {

        String[] split = path.split("\\\\");
        StringBuilder stringBuilder = new StringBuilder();
        String name = split[split.length-1];

        for(int i = 0; i < split.length-1; i++){
            stringBuilder.append(split[i]);
            if(i != split.length-2)
                stringBuilder.append("\\");

        }

        String to = stringBuilder.toString();

        if (this.getType() == UserType.REGULAR || this.getType() == null) {
            throw new UserTypeException("User isn't admin");
        }
        File file = new File(path);
        boolean ok = file.mkdirs();

        if(!ok){{

            if(!checkIfStorageExists(path))
                throw new StorageInitException("Couldn't initialize storage at " + path);
            else
                return "storage already exists";
        }

        }else{

            initUserData(path, this);
            initConfig(path);

            return "success: Storage " + name + " initialized at: " + to;
        }

    }

    private boolean checkIfStorageExists(String path) {

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
                System.out.println(0);
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
                System.out.println(0);
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

    @Override
    public String toString() {
        return this.getUsername();
    }
}
