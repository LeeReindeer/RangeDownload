package moe.leer.rangedownload;

import moe.leer.rangedownload.model.FileInfo;
import moe.leer.rangedownload.util.FileUtil;
import moe.leer.rangedownload.util.HttpUtil;
import moe.leer.rangedownload.util.Logger;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Splits a file into multiple parts download
 * callable returns a pair of (temp filename, success)
 */
public class DownloadPartialTask implements Callable<Pair<String, Boolean>> {

    private Logger logger = Logger.getLogger(DownloadPartialTask.class);

    private final FileInfo fileInfo;

    private final String url;
    private final int part;

    /**
     * inclusive start
     */
    private final long startByte;

    /**
     * exclusive end
     */
    private final long endByte;

    public DownloadPartialTask(FileInfo fileInfo, String url, int part, long startByte, long endByte) {
        this.fileInfo = fileInfo;
        this.url = url;
        this.part = part;
        this.startByte = startByte;
        this.endByte = endByte;
    }

    public Pair<String, Boolean> download() throws ExecutionException {
        String tempFileName = this.fileInfo.fileName() + "_" + this.part + ".tmp";
        File tempFile = new File(tempFileName);
        long localFileSize = tempFile.exists() ? tempFile.length() : 0;
        if (localFileSize >= this.endByte - this.startByte) {
            logger.info("Partial file already downloaded: " + tempFileName);
            return Pair.of(tempFileName, true);
        }
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(tempFile, "rw")) {
            randomAccessFile.seek(localFileSize);
            if (localFileSize != 0) {
                logger.error("Continuing partial download: " + tempFileName);
            }
            HttpClient httpClient = HttpUtil.getSharedHttpClient();
            long downloadStartPos = localFileSize + this.startByte;
            Optional<HttpRequest> requestOpt = HttpUtil.buildRangeDownloadRequest(this.url, downloadStartPos, this.endByte);
            if (requestOpt.isEmpty()) {
                return Pair.of("", false);
            }
            HttpRequest request = requestOpt.get();
            logger.info("Downloading part file: " + tempFileName + " from " + downloadStartPos + " to " + this.endByte);
//            CompletableFuture<HttpResponse<InputStream>> httpResponseCompletableFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
//            HttpResponse<InputStream> httpResponse = httpResponseCompletableFuture.get();
            HttpResponse<InputStream> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            FileUtil.bufferRead((int) fileInfo.contentLength(), httpResponse.body(), randomAccessFile);
            return Pair.of(tempFileName, true);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            logger.error("DownloadPartialTask error: %s", e);
            return Pair.of("", false);
        }
    }

    @Override
    public Pair<String, Boolean> call() throws Exception {
        return download();
    }

    @Override
    public String toString() {
        return "DownloadPartialTask{" + "url='" + url + '\'' + ", part=" + part + ", startByte=" + startByte + ", endByte=" + endByte + '}';
    }
}
