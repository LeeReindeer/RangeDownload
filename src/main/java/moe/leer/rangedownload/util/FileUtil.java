package moe.leer.rangedownload.util;

import java.io.File;

public class FileUtil {
    public static String getFilePath(String dir, String fileName) {
        if (!dir.endsWith(File.separator)) {
            dir += File.separator;
        }
        return dir + fileName;
    }
}
