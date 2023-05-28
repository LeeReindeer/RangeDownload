package moe.leer.rangedownload;

import moe.leer.rangedownload.model.DownloadTaskInfo;
import moe.leer.rangedownload.util.FileUtil;
import moe.leer.rangedownload.util.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

public class DownloadManager {

    private Logger logger = Logger.getLogger(DownloadManager.class);

    // 最多同时允许下载任务
    private int maxDownloadTaskSize = 5;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final ExecutorService multiTaskExecutor;
    private final Semaphore semaphore = new Semaphore(maxDownloadTaskSize);

    private String baseDir = "C:\\Users\\14541\\Downloads\\test1";

    public DownloadManager() {
        this.multiTaskExecutor = Executors.newFixedThreadPool(maxDownloadTaskSize);
    }

    public DownloadManager(String baseDir) {
        this.baseDir = baseDir;
        this.multiTaskExecutor = Executors.newFixedThreadPool(maxDownloadTaskSize);
    }

    public DownloadManager(String baseDir, int maxDownloadTaskSize) {
        this.baseDir = baseDir;
        this.maxDownloadTaskSize = maxDownloadTaskSize;
        this.multiTaskExecutor = Executors.newFixedThreadPool(maxDownloadTaskSize);
    }

    public void download(DownloadTaskInfo downloadTaskInfo) {
        try {
            DownloadTask downloadTask = new DownloadTask(downloadTaskInfo.url(), FileUtil.getFilePath(this.baseDir, downloadTaskInfo.downloadDir()));
            logger.info("DownloadTask request: %s,%s", downloadTask.getUrl(), downloadTask.getTaskStatus());
//            Future<Boolean> f = executor.submit(downloadTask);
//            Boolean result = f.get();
            boolean result = downloadTask.download();
            logger.info("DownloadTask result: %s,%s", downloadTask.getUrl(), downloadTask.getTaskStatus());
            downloadTask.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("DownloadManager error: %s", e);
        }
    }

    private CountDownLatch countDownLatch;

    public void download(List<DownloadTaskInfo> downloadTaskInfos) throws InterruptedException {
        countDownLatch = new CountDownLatch(downloadTaskInfos.size());
        for (DownloadTaskInfo taskInfo : downloadTaskInfos) {
            multiTaskExecutor.execute(() -> {
                try {
                    semaphore.acquire(1);
                    download(taskInfo);
                    Thread.sleep(1);
                    countDownLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    semaphore.release(1);
                }
            });
        }
        countDownLatch.await();
    }

    public void shutdown() {
        multiTaskExecutor.shutdownNow();
        executor.shutdownNow();
        SharedWorkerPool.shutdown();
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        DownloadManager downloadManager = new DownloadManager();
        downloadManager.download(new DownloadTaskInfo("https://testingcf.jsdelivr.net/gh/nj-lizhi/song@main/audio/1701/大象.mp3", "大象"));

        System.out.println("shutdown");
        downloadManager.shutdown();
    }
}
