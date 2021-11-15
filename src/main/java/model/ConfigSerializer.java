package model;

import com.google.gson.*;
import user.Privilege;
import user.UserType;

import java.lang.reflect.Type;
import java.util.*;

public class ConfigSerializer implements JsonSerializer<Config>, JsonDeserializer<Config> {
    @Override
    public JsonElement serialize(Config config, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonUser = new JsonObject();
        jsonUser.addProperty("sizeLimit", config.getSize());
        jsonUser.addProperty("fileNumberLimit", config.getFileLimit());
        jsonUser.addProperty("excludedExtensions", String.valueOf(config.getExclExt()));
        return jsonUser;
    }

    @Override
    public Config deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        String excludedExtensions = jsonObject.get("excludedExtensions").toString();
        excludedExtensions = excludedExtensions.replace("\"","");
        excludedExtensions = excludedExtensions.replace("{", "");
        excludedExtensions = excludedExtensions.replace("}", "");
        excludedExtensions = excludedExtensions.replace("]", "");
        excludedExtensions = excludedExtensions.replace("[", "");
        String[] split = excludedExtensions.split(",");

        List<String> exc = new ArrayList<>(Arrays.asList(split));

        return new Config(
                jsonObject.get("sizeLimit").getAsInt(),
                jsonObject.get("fileNumberLimit").getAsInt(),
                exc
        );
    }
}
