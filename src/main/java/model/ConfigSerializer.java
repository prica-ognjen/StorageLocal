package model;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class ConfigSerializer implements JsonSerializer<Config>, JsonDeserializer<Config> {
    @Override
    public JsonElement serialize(Config config, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonUser = new JsonObject();

        return jsonUser;
    }

    @Override
    public Config deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        return new Config(

        );
    }
}
