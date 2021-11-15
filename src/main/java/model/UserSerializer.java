package model;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import model.LocalUser;
import user.Privilege;
import user.User;
import user.UserType;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class UserSerializer implements JsonSerializer<User>, JsonDeserializer<User> {
    @Override
    public JsonElement serialize(User user, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonUser = new JsonObject();
        jsonUser.addProperty("username", user.getUsername());
        jsonUser.addProperty("password", user.getPassword());
        jsonUser.addProperty("type", String.valueOf(user.getType()));
        jsonUser.addProperty("privileges", String.valueOf(user.getPrivileges()));
        return jsonUser;
    }

    @Override
    public User deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        String tempType = jsonObject.get("type").getAsString();
        UserType userType = null;

        if(tempType.equals(UserType.SUPER.name())){
            userType = UserType.SUPER;
        }else if(tempType.equals(UserType.REGULAR.name())){
            userType = UserType.REGULAR;
        }

        String tempPrivileges = jsonObject.get("privileges").toString();
        tempPrivileges = tempPrivileges.replace("\"","");
        tempPrivileges = tempPrivileges.replace("{", "");
        tempPrivileges = tempPrivileges.replace("}", "");
        String[] split = tempPrivileges.split(",");

        Map<Privilege, Boolean> map = new HashMap<>();

        for(String s: split){
            String[] temp = s.split(":");
            Privilege tempPrivilege = null;
            Boolean bool = null;
            for(Privilege p: Privilege.values()){
                if(p.name().equals(temp[0])){
                    tempPrivilege = p;
                    break;
                }
            }
            if(temp[1].equals("true")){
                bool = true;
            }else if(temp[1].equals("false")){
                bool = false;
            }

            if(bool == null || tempPrivilege == null){
                System.out.println("there was a problem in deserialization");
            }else {
                map.put(tempPrivilege, bool);
            }
        }

        return new LocalUser(
                jsonObject.get("username").getAsString(),
                jsonObject.get("password").getAsString(),
                userType,
                map
        );
    }
}
