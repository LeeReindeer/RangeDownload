package moe.leer.rangedownload.util;

import com.google.common.net.UrlEscapers;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.*;

class URLUtilTest {

    @org.junit.jupiter.api.Test
    void testEncodeURL() {
        String encodedString = URLUtil.encodeURLGuava("https://testingcf.jsdelivr.net/gh/nj-lizhi/song@main/audio/这个世界会好吗/李志- 这个世界会好吗.mp3");
        String encodedString2 = URLUtil.encodeURL("https://testingcf.jsdelivr.net/gh/nj-lizhi/song@main/audio/这个世界会好吗/李志- 这个世界会好吗.mp3");
        Assertions.assertEquals(encodedString, encodedString2);
    }

    @org.junit.jupiter.api.Test
    void testGetFileName() {
        String encodedString = URLUtil.getFileName("https://testingcf.jsdelivr.net/gh/nj-lizhi/song@main/audio/这个世界会好吗/李志- 这个世界会好吗.mp3");
        Assertions.assertEquals("李志- 这个世界会好吗.mp3", encodedString);
    }
}