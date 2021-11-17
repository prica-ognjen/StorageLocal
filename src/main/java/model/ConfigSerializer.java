package model;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class ConfigSerializer implements JsonSerializer<Config>, JsonDeserializer<Config> {
    @Override
    public JsonElement serialize(Config config, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonUser = new JsonObject();
        jsonUser.addProperty("sizeLimit", config.getSizeLimit());
        jsonUser.addProperty("fileNumLimit", config.getFileNumLimit());
        jsonUser.addProperty("blockedExtensions", config.getBlockedExtensions().toString());
        return jsonUser;
    }

    @Override
    public Config deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        String tempExtensions = jsonObject.get("blockedExtensions").toString();
        tempExtensions = tempExtensions.replace("\"","");
        tempExtensions = tempExtensions.replace("{", "");
        tempExtensions = tempExtensions.replace("}", "");
        tempExtensions = tempExtensions.replace("[", "");
        tempExtensions = tempExtensions.replace("]", "");
        String[] split = tempExtensions.split(",");

        ArrayList<String> list = new ArrayList<>();

        for(String s: split){
            if(!s.equals(""))
                list.add(s);
        }

        return new Config(
                jsonObject.get("sizeLimit").getAsInt(),
                jsonObject.get("fileNumLimit").getAsInt(),
                list
        );
    }
}
