package moe.leer.rangedownload;

public record FileInfo(String url, String fileName, String contentType, long contentLength, String etag) {
}
