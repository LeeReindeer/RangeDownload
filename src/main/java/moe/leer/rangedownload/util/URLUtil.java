package moe.leer.rangedownload.util;

import com.google.common.net.UrlEscapers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class URLUtil {

    private static Logger logger = new Logger(URLUtil.class);

    public static String encodeURL(String url) {
        // replace non-ascii characters
        Matcher matcher = Pattern.compile("[\\u4e00-\\u9fa5|\\s]+").matcher(url);
        while (matcher.find()) {
            String tmp = matcher.group();
            String encode = URLEncoder.encode(tmp, StandardCharsets.UTF_8);
            logger.debug("replace " + tmp + " to " + encode);
            url = url.replaceFirst(tmp, encode).replace("+", "%20");
        }
        logger.debug("encode " + url);
        return url;
    }

    public static String getFileName(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public static void main(String[] args) {
        String encodedString = UrlEscapers.urlFragmentEscaper().escape("https://testingcf.jsdelivr.net/gh/nj-lizhi/song@main/audio/这个世界会好吗/李志- 这个世界会好吗.mp3");
        String encodedString2 = encodeURL("https://testingcf.jsdelivr.net/gh/nj-lizhi/song@main/audio/这个世界会好吗/李志- 这个世界会好吗.mp3");
        System.out.println(encodedString);
        System.out.println(encodedString2.equals(encodedString));
    }
}
