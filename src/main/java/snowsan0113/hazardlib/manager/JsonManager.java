package snowsan0113.hazardlib.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snowsan0113.hazardlib.Main;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class JsonManager {

    private final FileType type;
    private final Gson gson;

    public JsonManager(FileType type) {
        this.type = type;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public JsonElement getObjectValue(String key) throws IOException {
        JsonObject raw_json = getRawJson();
        String[] keys = key.split("\\."); // 「.」で区切る
        JsonObject now_json = raw_json; //jsonを代入する
        for (int n = 0; n < keys.length - 1; n++) { //keyの1個前未満をループする。（keyが2個だと、1回だけ実行）
            now_json = now_json.getAsJsonObject(keys[n]); // jsonを代入する
        }

        return now_json.get(keys[keys.length - 1]); //key数 - 1（最後のキー）を取得する
    }

    public JsonObject getRawJson() throws IOException {
        createJson();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(getFile().toPath()), StandardCharsets.UTF_8))) {
            return gson.fromJson(reader, JsonObject.class);
        }
    }

    public File getFile() throws IOException {
        return type.getFile();
    }

    public FileType getType() {
        return type;
    }

    private boolean createJson() throws IOException{
        if (!getFile().exists()) {
            try {
                URL resource_url = Main.class.getResource("/" + type.getFile().getName());
                File resource_file = new File(resource_url.toURI());
                Files.copy(resource_file.toPath(), type.getFile().toPath());
                return true;
            } catch (URISyntaxException | NullPointerException e) {
                throw new RuntimeException("ファイルパスが間違っているため見つかりませんでした：" + e);
            }
        }
        else {
            return true;
        }
    }

    private void writeFile(String date) {
        try (BufferedWriter write = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(getFile().toPath()), StandardCharsets.UTF_8))) {
            write.write(date);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
