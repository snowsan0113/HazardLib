package snowsan0113.hazardlib.api.tsunami;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snowsan0113.hazardlib.api.APIType;
import snowsan0113.hazardlib.util.JsonUtil;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class P2PTsunamiAPI {

    private String api_url; //現在のAPIURL
    private final int GET_CODE = 552; //取得したいコード（固定）： 552（※地震）
    private int limit; //取得したい数
    private int index; //取得したい番号

    public P2PTsunamiAPI(int limit, int index) {
        this.limit = limit;
        this.index = index;
        setURL();
    }

    public JsonArray getRawJson() throws IOException {
        URL url = new URL(api_url);
        HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();

        String inputLine;
        while((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();
        con.disconnect();
        return  (new Gson()).fromJson(content.toString(), JsonArray.class);
    }

    public Map<String, Areas> getAreas() throws IOException, ParseException {
        Map<String, Areas> map = new HashMap<>();
        JsonArray json = getRawJson();
        JsonObject index_json = json.get(index).getAsJsonObject();
        JsonArray areas_array = index_json.getAsJsonArray("areas");
        for (JsonElement area_json : areas_array) {
            JsonObject area_obj = area_json.getAsJsonObject();
            TsunamiType grade = TsunamiType.getAPIConvertType(JsonUtil.getJsonString(area_obj, "grade"));
            String name = JsonUtil.getJsonString(area_obj, "name");
            boolean immediate = area_obj.get("immediate").getAsBoolean();

            //API上で、firstHeight
            JsonObject firstHeight = area_obj.getAsJsonObject("firstHeight");
            String raw_time = JsonUtil.getJsonString(firstHeight, "arrivalTime");
            LocalDateTime firstHeight_arrivalTime = null;
            if (raw_time != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date formatDate = sdf.parse(raw_time);
                firstHeight_arrivalTime = LocalDateTime.ofInstant(formatDate.toInstant(), ZoneId.systemDefault());
            }
            String condition = JsonUtil.getJsonString(firstHeight, "condition");

            //API上で、maxHeight
            JsonObject maxHeight = area_obj.getAsJsonObject("maxHeight");
            String maxHeight_description = JsonUtil.getJsonString(maxHeight, "description");
            int maxHeight_value = maxHeight.get("value").getAsInt();

            map.put(name, new Areas(grade, immediate, name, new Areas.FirstHeight(firstHeight_arrivalTime, condition), new Areas.MaxHeight(maxHeight_description, maxHeight_value)));
        }
        return map;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
        setURL();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    private void setURL() {
        this.api_url = APIType.P2P_API_TSUNAMI.getStringURL() + "?codes=" + GET_CODE + "&limit=" + limit;
    }

    public record Areas(TsunamiType type, boolean immediate, String name, FirstHeight firstHeight, MaxHeight maxHeight) {
        @Override
        public String toString() {
            return "---------------" + "\n" +
                    "{津波の種類：" + type.getString() + ",直ちに襲来するか：" + (immediate ? "はい" : "いいえ") + ",津波予報区名：" + name + "}" + "\n" +
                    "---------------" + "\n";
        }

        public record FirstHeight(LocalDateTime arrivalTime, String condition) {
            @Override
            public String toString() {
                return "---------------" + "\n" +
                        "{第一波到達予想時刻：" + arrivalTime + ",到達表現：" + condition + "}" + "\n" +
                        "---------------" + "\n";
            }
        }

        public record MaxHeight(String description, int value) {
            @Override
            public String toString() {
                return "---------------" + "\n" +
                        "{予想される高さの文字表現：" + description + ",予想される高さの数値：" + value + "}" + "\n" +
                        "---------------" + "\n";
            }
        }
    }
}
