package moe.leer.rangedownload;

import moe.leer.rangedownload.util.HttpUtil;
import moe.leer.rangedownload.util.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static moe.leer.rangedownload.SharedWorkerPool.PART_TASK_WORKER_POOL;

public class DownloadTask implements Callable<Boolean> {

    private Logger logger = Logger.getLogger(DownloadTask.class);

    public static final int DEFAULT_TASK_SIZE = 4;

//    private final ExecutorService partTaskExecutor;

    private String url;
    private TaskStatus taskStatus = TaskStatus.SUBMITTED;

    private int taskSize = DEFAULT_TASK_SIZE;

    private final List<DownloadPartialTask> partialTasks;

    private final String downloadDir;

    public DownloadTask(String url, String downloadDir) {
        this.url = url;
        this.downloadDir = downloadDir;
        this.partialTasks = new ArrayList<>(taskSize);
//        this.partTaskExecutor = Executors.newFixedThreadPool(taskSize + 1);
    }

    public DownloadTask(String url, int taskSize, String downloadDir) {
        this.url = url;
        this.downloadDir = downloadDir;
        this.taskSize = taskSize;
        this.partialTasks = new ArrayList<>(taskSize);
//        this.partTaskExecutor = Executors.newFixedThreadPool(taskSize + 1);
    }

    public void splitTask(FileInfo fileInfo) {
        long chunkSize = fileInfo.contentLength() / taskSize;
        for (int i = 0; i < taskSize; i++) {
            if (i == taskSize - 1) {
                partialTasks.add(new DownloadPartialTask(fileInfo, this.url, i, chunkSize * i, -1));
            } else {
                // inclusive start, exclusive end
                partialTasks.add(new DownloadPartialTask(fileInfo, this.url, i, chunkSize * i, chunkSize * (i + 1) - 1));
            }
        }
    }

    public boolean mergeTaskFile(FileInfo fileInfo) throws ExecutionException, InterruptedException {
        logger.info("Merging file: %s", fileInfo.fileName());
        Future<Boolean> f = PART_TASK_WORKER_POOL.submit(() -> {
            // 1MB
            long bufferSize = Math.min(fileInfo.contentLength(), DownloadPartialTask.BYTE_SIZE);
            byte[] buffer = new byte[(int) bufferSize];
            try (RandomAccessFile oSavedFile = new RandomAccessFile(fileInfo.fileName(), "rw")) {
                for (int i = 0; i < this.taskSize; i++) {
                    String tempFileName = fileInfo.fileName() + "_" + i + ".tmp";
                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(tempFileName))) {
                        int len = -1;
                        while ((len = bis.read(buffer)) != -1) { // 读到文件末尾则返回-1
                            oSavedFile.write(buffer, 0, len);
                        }
                    }
                    clearTemp(tempFileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        });
        Boolean res = f.get();
        logger.info("Merged file: %s,%s", fileInfo.fileName(), res);
        return res;
    }

    private void clearTemp(String fileName) {
        for (int i = 0; i < taskSize; i++) {
            File file = new File(fileName);
            file.delete();
        }
    }


    public boolean download() {
        taskStatus = TaskStatus.DOWNLOADING;
        Optional<FileInfo> fileInfo = HttpUtil.getHttpFileInfo(this.url, this.downloadDir);
        if (fileInfo.isPresent()) {
            try {
                // 0. check downloadDir
                File file = new File(this.downloadDir);
                if (!file.exists()) {
                    file.mkdirs();
                }
                // 1.split and build DownloadPartialTask
                splitTask(fileInfo.get());
                boolean res = true;
                List<Future<Boolean>> futures = PART_TASK_WORKER_POOL.invokeAll(partialTasks);
                for (Future<Boolean> future : futures) {
                    // todo support retry
                    res &= future.get();
                }
                // 2.merge download files
                if (res) {
                    res &= mergeTaskFile(fileInfo.get());
                }
                taskStatus = res ? TaskStatus.FINISHED : TaskStatus.FAILED;
                return res;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("DownloadTask error: %s", e);
            }
            taskStatus = TaskStatus.FAILED;
            return false;
        } else {
            taskStatus = TaskStatus.FAILED;
            return false;
        }
    }

    public void shutdown() {
//        partTaskExecutor.shutdown();
    }

    @Override
    public Boolean call() throws Exception {
        return download();
    }

    public String getUrl() {
        return url;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public int getTaskSize() {
        return taskSize;
    }

    public List<DownloadPartialTask> getPartialTasks() {
        return partialTasks;
    }
}
