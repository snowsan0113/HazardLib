package snowsan0113.hazardlib.api;

import java.net.MalformedURLException;
import java.net.URL;

public enum APIType {

    P2P_API("https://api.p2pquake.net/v2"),
    P2P_API_QUAKE_HISTORY(P2P_API.getStringURL() + "/history"),
    P2P_API_QUAKE_SOCKET(P2P_API.getStringURL() + "/ws"),
    P2P_API_TSUNAMI(P2P_API.getStringURL() + "/jma/tsunami");

    private final String url;

    APIType(String url) {
        this.url = url;
    }

    public String getStringURL() {
        return url;
    }

    public URL getURL() throws MalformedURLException {
        return new URL(url);
    }
}
