package moe.leer.rangedownload;

import moe.leer.rangedownload.util.HttpUtil;
import moe.leer.rangedownload.util.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

    static Logger logger = new Logger(Test.class);

    public static void main(String[] args) throws Exception {
//        logger.info("length:%s ", getHttpFileContentLength("https://testingcf.jsdelivr.net/gh/nj-lizhi/song@main/audio/108个关键词/倒影.mp3"));
        String url = "https://testingcf.jsdelivr.net/gh/nj-lizhi/song@main/audio/这个世界会好吗/李志- 这个世界会好吗.mp3";
        Optional<FileInfo> httpFileInfo = HttpUtil.getHttpFileInfo(url,   ".");
        logger.info((double)httpFileInfo.get().contentLength() / 1024 / 1024 + "MB");
        logger.info(httpFileInfo.get().toString());
//        logger.error("length:%s", getHttpFileContentLength("https://testingcf.jsdelivr.net/gh/nj-lizhi/song@main/audio/%E7%94%B5%E5%A3%B0%E4%B8%8E%E7%AE%A1%E5%BC%A6%E4%B9%90II/%E5%B1%B1%E9%98%B4%E8%B7%AF%E7%9A%84%E5%A4%8F%E5%A4%A9%20(%E7%9B%B8%E4%BF%A1%E6%9C%AA%E6%9D%A5%E7%89%88).mp3"));
//        logger.info("length:%s", getHttpFileContentLength("https://github.com/nj-lizhi/song/raw/main/audio/108%E4%B8%AA%E5%85%B3%E9%94%AE%E8%AF%8D/%E5%80%92%E5%BD%B1.mp3"));
    }

    public static long getHttpFileContentLength(String url) throws IOException {
        HttpURLConnection httpUrlConnection = getHttpUrlConnection(url);
        int contentLength = httpUrlConnection.getContentLength();
        httpUrlConnection.disconnect();
        return contentLength;
    }

    public static HttpURLConnection getHttpUrlConnection(String url) throws IOException {
        URL httpUrl = new URL(encodeValue(url));
        System.out.println(httpUrl.toString());
        HttpURLConnection httpConnection = (HttpURLConnection) httpUrl.openConnection();
        httpConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36");
        return httpConnection;
    }

    private static String encodeValue(String url) {
//        String result = URLEncoder.encode(url, StandardCharsets.UTF_8);
//        result = result.replaceAll("%3A", ":")
//                .replaceAll("%2F", "/")
//                .replaceAll("\\+", "%20");//+实际上是 空格 url encode而来

        Matcher matcher = Pattern.compile("[\\u4e00-\\u9fa5]+").matcher(url);
        while (matcher.find()) {
            String tmp = matcher.group();
            System.out.println("replace " + tmp + " to " + URLEncoder.encode(tmp, StandardCharsets.UTF_8));
            url = url.replaceFirst(tmp, URLEncoder.encode(tmp, StandardCharsets.UTF_8));
        }
        return url;
    }

}
