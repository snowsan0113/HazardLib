package snowsan0113.hazardlib.api.quake;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snowsan0113.hazardlib.api.APIType;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class P2PQuakeHistoryAPI {

    private String api_url; //現在のAPIURL
    private final int GET_CODE = 551; //取得したいコード（固定）： 551（※地震）
    private int limit; //取得したい数
    private int index; //取得したい番号

    public P2PQuakeHistoryAPI(int limit, int index) {
        this.limit = limit;
        this.index = index;
        setURL();
    }

    @Deprecated
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

    public EarthquakeData getEarthquakeData() throws IOException {
        try {
            JsonArray raw = getRawJson();
            JsonObject quake_obj = raw.get(index).getAsJsonObject();
            JsonObject earthquake_obj = quake_obj.getAsJsonObject("earthquake");
            JsonObject hypocenter_obj = earthquake_obj.getAsJsonObject("hypocenter");

            //震源データ
            String name = hypocenter_obj.get("name").getAsString();
            int depth = hypocenter_obj.get("depth").getAsInt();
            double magnitude = hypocenter_obj.get("magnitude").getAsDouble();
            double latitude = hypocenter_obj.get("latitude").getAsDouble();
            double longitude = hypocenter_obj.get("longitude").getAsDouble();
            String domesticTsunami = earthquake_obj.get("domesticTsunami").getAsString();
            String foreignTsunami = earthquake_obj.get("foreignTsunami").getAsString();
            QuakeType max_scale = QuakeType.getRawToType(earthquake_obj.get("maxScale").getAsInt());
            String raw_time = earthquake_obj.get("time").getAsString();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date formatDate = sdf.parse(raw_time);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(formatDate.toInstant(), ZoneId.systemDefault());
            return new EarthquakeData(name, depth, magnitude, latitude, longitude, domesticTsunami, foreignTsunami, max_scale, localDateTime);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<EarthquakeData> getAsyncEarthquakeData() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getEarthquakeData();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    public List<PointData> getPointList() throws IOException {
        List<PointData> list = new ArrayList<>();
        JsonArray raw = getRawJson();
        JsonObject quake_obj = raw.get(index).getAsJsonObject();
        JsonArray points_array = quake_obj.getAsJsonArray("points");
        for (JsonElement point : points_array) {
            JsonObject point_obj = point.getAsJsonObject();
            String addr = point_obj.get("addr").getAsString();
            boolean isArea = point_obj.get("isArea").getAsBoolean();
            String pref = point_obj.get("pref").getAsString();
            int scale_raw = point_obj.get("scale").getAsInt();
            QuakeType scale = Arrays.stream(QuakeType.values())
                    .filter(type -> type.getRawData() == scale_raw)
                    .findFirst()
                    .orElse(null);
            list.add(new PointData(addr, isArea, pref, scale));
        }
        return list;
    }

    public CompletableFuture<List<PointData>> getAsyncPointList() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getPointList();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
        setURL();
    }

    public int getIndex() {
        return limit;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    private void setURL() {
        this.api_url = APIType.P2P_API_QUAKE_HISTORY.getStringURL() + "?codes=" + GET_CODE + "&limit=" + limit;
    }

    /**
     * //aa
     * @param name      震源名
     * @param depth     深さ
     * @param magnitude マグニチュード
     * @param latitude  緯度
     * @param longitude 経度
     * @param domesticTsunami 国内への津波の有無
     * @param foreignTsunami 海外での津波の有無
     * @param max_scale 最大震度
     * @param time 発生時間
     */
    public record EarthquakeData(String name, int depth, double magnitude, double latitude, double longitude,
                                 String domesticTsunami, String foreignTsunami, QuakeType max_scale, LocalDateTime time) {
        @Override
        public String toString() {
            return "---------------" + "\n" +
                    "{震源名：" + name + ",深さ：" + depth + "km,マグニチュード：" + magnitude + ",緯度：" + latitude + ",経度：" + longitude + "}" + "\n" +
                    "{国内への津波の有無：" + domesticTsunami + ",海外への津波の有無：" + foreignTsunami + ",最大震度：" + max_scale.getJPName() + ",発生時間：" + time.toString() + "}" + "\n" +
                    "---------------";
        }
    }

    /**
     * @param addr 新原名
     * @param isArea 区域内かどうか
     * @param pref 都道府県
     * @param scale //震度
     */
    public record PointData(String addr, boolean isArea, String pref, QuakeType scale) {
        @Override
        public String toString() {
            return "{震度観測点:" + addr + ",区域名か:" + isArea + ",都道府県:" + pref + ",震度:" + scale + "}";
        }
    }



}
