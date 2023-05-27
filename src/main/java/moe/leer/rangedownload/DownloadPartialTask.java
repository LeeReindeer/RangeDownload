package moe.leer.rangedownload;

import moe.leer.rangedownload.util.HttpUtil;
import moe.leer.rangedownload.util.Logger;

import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Callable;

public class DownloadPartialTask implements Callable<Boolean> {

    private Logger logger = Logger.getLogger(DownloadPartialTask.class);

    /**
     * 1MB
     */
    public static final int BYTE_SIZE = 1024 * 1024;
    private FileInfo fileInfo;

    private String url;
    private int part;

    /**
     * inclusive start
     */
    private long startByte;

    /**
     * exclusive end
     */
    private long endByte;

    public DownloadPartialTask(FileInfo fileInfo, String url, int part, long startByte, long endByte) {
        this.fileInfo = fileInfo;
        this.url = url;
        this.part = part;
        this.startByte = startByte;
        this.endByte = endByte;
    }

    public boolean download() {
        HttpClient httpClient = HttpUtil.getSharedHttpClient();
        HttpRequest request = HttpUtil.buildRangeDownloadRequest(this.url, this.startByte, this.endByte);
        String tempFileName = this.fileInfo.fileName() + "_" + this.part + ".tmp";
        try (RandomAccessFile oSavedFile = new RandomAccessFile(tempFileName, "rw")) {
            oSavedFile.seek(0);
            logger.info("Downloading part file: " + tempFileName + " from " + this.startByte + " to " + this.endByte);
            HttpResponse<InputStream> send = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (BufferedInputStream bis = new BufferedInputStream(send.body())) {
                long bufferSize = Math.min(fileInfo.contentLength(), BYTE_SIZE);
                byte[] buffer = new byte[(int) bufferSize];
                int len = -1;
                while ((len = bis.read(buffer)) != -1) { // 读到文件末尾则返回-1
                    oSavedFile.write(buffer, 0, len);
                }
                return true;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            logger.error("DownloadPartialTask error: %s", e);
            return false;
        }
    }

    @Override
    public Boolean call() throws Exception {
        return download();
    }

    @Override
    public String toString() {
        return "DownloadPartialTask{" +
                "url='" + url + '\'' +
                ", part=" + part +
                ", startByte=" + startByte +
                ", endByte=" + endByte +
                '}';
    }
}
