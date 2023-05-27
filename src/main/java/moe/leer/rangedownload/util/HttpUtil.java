package moe.leer.rangedownload.util;

import moe.leer.rangedownload.FileInfo;

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
                .uri(new URI(URLUtil.encodeURL(url))).build();
        HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return res.headers().firstValueAsLong("Content-Length").orElse(0L);
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

    public static HttpRequest buildRangeDownloadRequest(String url, long start, long end) {
        try {
            String range = String.format("bytes=%d-%d", start, end);
            if (end == -1) {
                range = String.format("bytes=%d-", start);
            }
            return HttpRequest.newBuilder()
                    .uri(new URI(URLUtil.encodeURL(url)))
                    .header("Range", range)
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpClient getSharedHttpClient() {
        return httpClient;
    }
}
