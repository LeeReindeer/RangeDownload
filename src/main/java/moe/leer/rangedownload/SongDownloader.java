package moe.leer.rangedownload;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import moe.leer.rangedownload.model.DownloadTaskInfo;
import moe.leer.rangedownload.model.Song;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class SongDownloader {
    public static void main(String[] args) throws InterruptedException, IOException {
        String json = new String(DownloadManager.class.getClassLoader().getResourceAsStream("lizhi.json").readAllBytes());
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Song>>() {
        }.getType();
        List<Song> songs = gson.fromJson(json, listType);

        List<DownloadTaskInfo> downloadTaskInfos = songs.stream().map(song -> new DownloadTaskInfo(song.url(), song.artist())).toList();
        DownloadManager downloadManager = new DownloadManager();
        downloadManager.download(downloadTaskInfos);
        System.out.println("shutdown");
        downloadManager.shutdown();
    }
}
