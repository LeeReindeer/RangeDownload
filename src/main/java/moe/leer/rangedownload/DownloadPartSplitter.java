package moe.leer.rangedownload;

import java.util.List;

public class DownloadPartSplitter {

    private String size;
    private String url;

    public DownloadPartSplitter(String size, String url) {
        this.size = size;
        this.url = url;
    }


    public List<DownloadPartialTask> split() {
        return null;
    }

    public static void main(String[] args) {
    }
}
