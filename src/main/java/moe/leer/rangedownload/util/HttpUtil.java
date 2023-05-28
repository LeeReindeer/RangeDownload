package moe.leer.rangedownload.util;

import moe.leer.rangedownload.model.FileInfo;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

public final class HttpUtil {

    private static final Logger logger = Logger.getLogger(HttpUtil.class);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            // cachedExecutor
            .build();

    private HttpUtil() {
    }

    public static long getHttpContentLength(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .header("Want-Digest", "SHA-256")
                .uri(new URI(URLUtil.encodeURL(url))).build();
        HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(res.headers());
        return res.headers().firstValueAsLong("Content-Length").orElse(0L);
    }

    public static void main(String[] args) {
        try {
            System.out.println(getHttpContentLength("https://testingcf.jsdelivr.net/gh/nj-lizhi/song@main/audio/1701/大象.mp3"));
        } catch (Exception e) {
            logger.error("main error: %s", e);
        }
    }

    public static Optional<FileInfo> getHttpFileInfo(String url, String dir) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .uri(new URI(URLUtil.encodeURL(url))).build();
            HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String fileName = FileUtil.getFilePath(dir, URLUtil.getFileName(url));
            String contentType = res.headers().firstValue("Content-Type").orElse("plain/text");
            long contentLength = res.headers().firstValueAsLong("Content-Length").orElse(0L);
            String etag = res.headers().firstValue("etag").orElse(null);
            return Optional.of(new FileInfo(url, fileName, contentType, contentLength, etag));
        } catch (Exception e) {
            logger.error("getHttpFileInfo error: %s", e);
            return Optional.empty();
        }
    }

    public static Optional<HttpRequest> buildRangeDownloadRequest(String url, long start, long end) {
        try {
            String range = String.format("bytes=%d-%d", start, end);
            if (end == -1) {
                range = String.format("bytes=%d-", start);
            }
            return Optional.ofNullable(HttpRequest.newBuilder()
                    .uri(new URI(URLUtil.encodeURL(url)))
                    .header("Range", range)
                    .build());
        } catch (URISyntaxException e) {
            logger.error("buildRangeDownloadRequest error: %s", e);
            return Optional.empty();
        }
    }

    public static HttpClient getSharedHttpClient() {
        return httpClient;
    }
}
