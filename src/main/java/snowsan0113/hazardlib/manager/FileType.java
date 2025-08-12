package snowsan0113.hazardlib.manager;

import java.io.File;

public enum FileType {

    ;

    private final String save_path;

    FileType(String path) {
        this.save_path = path;
    }

    public File getFile() {
        return new File(save_path);
    }

    public String getSavePath() {
        return save_path;
    }

    public String toString() {
        return save_path;
    }

}
