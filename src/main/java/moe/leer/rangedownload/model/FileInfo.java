package moe.leer.rangedownload.model;

public record FileInfo(String url, String fileName, String contentType, long contentLength, String etag) {
}
