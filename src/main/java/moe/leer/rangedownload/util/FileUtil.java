package moe.leer.rangedownload.util;

import java.io.*;

public class FileUtil {

    /**
     * 1MB
     */
    public static final int BYTE_SIZE = 1024 * 1024;

    public static String getFilePath(String dir, String fileName) {
        if (!dir.endsWith(File.separator)) {
            dir += File.separator;
        }
        return dir + fileName;
    }

    public static void bufferRead(InputStream in, DataOutput out) throws IOException {
        bufferRead(in.available(), in, out);
    }

    public static void bufferRead(int size, InputStream in, DataOutput out) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(in)) {
            long bufferSize = size != 0 ? Math.min(size, BYTE_SIZE) : BYTE_SIZE;
            byte[] buffer = new byte[(int) bufferSize];
            int len = -1;
            while ((len = bis.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }

    public static boolean checkAndCreateDir(String downloadDir) {
        File file = new File(downloadDir);
        if (!file.exists()) {
            return file.mkdirs();
        }
        return true;
    }
}
