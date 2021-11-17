import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Test {

    public static void main(String[] args) throws IOException {

        String test = "C:/Users/Ognjen/Desktop";
        String test2 = "C\\:users\\ognjen\\desktop\\storagetests";

        String name = "jsonFileObject.json";

        File f  = new File(test+"/"+name);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();

        Gson gson = gsonBuilder.create();

        Map<String, Config> map = new HashMap<>();

        map.put("key1", new Config());
        map.put("key2", new Config());

        BufferedWriter out = new BufferedWriter(new FileWriter(f));

        out.write(gson.toJson(map));

        out.close();

    }

}
