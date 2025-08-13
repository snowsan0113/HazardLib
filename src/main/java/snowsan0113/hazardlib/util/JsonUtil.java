package snowsan0113.hazardlib.util;

import com.google.gson.JsonObject;

public class JsonUtil {

    public static String getJsonString(JsonObject json, String key) {
        return json.get(key) == null ? null : json.get(key).getAsString();
    }

}
