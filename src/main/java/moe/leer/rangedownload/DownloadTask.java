package moe.leer.rangedownload;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import moe.leer.rangedownload.model.FileInfo;
import moe.leer.rangedownload.model.TaskStatus;
import moe.leer.rangedownload.util.FileUtil;
import moe.leer.rangedownload.util.HttpUtil;
import moe.leer.rangedownload.util.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static moe.leer.rangedownload.SharedWorkerPool.PART_TASK_WORKER_POOL;

public class DownloadTask implements Callable<Boolean> {

    private Logger logger = Logger.getLogger(DownloadTask.class);

    private static final Gson gson = new Gson();

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

    protected void splitTask(FileInfo fileInfo) {
        long chunkSize = fileInfo.contentLength() / taskSize;
        for (int i = 0; i < taskSize; i++) {
            if (i == taskSize - 1) {
                partialTasks.add(new DownloadPartialTask(fileInfo, this.url, i, chunkSize * i, fileInfo.contentLength()));
            } else {
                // inclusive start, exclusive end
                partialTasks.add(new DownloadPartialTask(fileInfo, this.url, i, chunkSize * i, chunkSize * (i + 1)));
            }
        }
    }

    protected boolean mergeTaskFile(FileInfo fileInfo, Set<String> tempFiles) throws ExecutionException, InterruptedException {
        logger.info("Merging file: %s", fileInfo.fileName());
        Future<Boolean> f = PART_TASK_WORKER_POOL.submit(() -> {
            File fullFile = new File(fileInfo.fileName());
            List<File> tempFilesList = tempFiles.stream().map(File::new).toList();
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(fullFile, "rw")) {
                for (File tempFile : tempFilesList) {
                    FileUtil.bufferRead(new FileInputStream(tempFile), randomAccessFile);
                }
                // delete temp file after all files are merged
                tempFilesList.forEach(File::delete);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                fullFile.delete();
                logger.error("Failed to merge file: %s", fileInfo.fileName());
                return false;
            }
        });
        Boolean res = f.get();
        logger.info("Merged file: %s,%s", fileInfo.fileName(), res);
        return res;
    }

    /**
     * Check if file already exists and maintains meta.json
     *
     * @param fileInfo
     * @return true if file already exists and etag matches
     * @throws IOException
     */
    protected boolean checkFileEtag(FileInfo fileInfo) throws IOException {
        synchronized (DownloadTask.class) {
            File metaFile = new File(FileUtil.getFilePath(this.downloadDir, "meta.json"));
            File file = new File(fileInfo.fileName());
            Set<FileInfo> fileInfos = new HashSet<>();
            boolean matchFileEtag = false;
            if (metaFile.exists()) {
                if (file.length() >= fileInfo.contentLength()) {
                    logger.error("%s,local file length: %s, meta length: %s", fileInfo.fileName(), file.length(), fileInfo.contentLength());
                }
                String metaJson = Files.readString(metaFile.toPath());
                Type listType = new TypeToken<Set<FileInfo>>() {
                }.getType();
                fileInfos = gson.fromJson(metaJson, listType);
                matchFileEtag = fileInfos.stream().filter(f -> f.fileName().equals(fileInfo.fileName()))
                        .anyMatch(f -> f.contentLength() == fileInfo.contentLength()
                                && file.isFile() && file.exists()
                                && file.length() >= f.contentLength()
                                && StringUtils.equals(f.etag(), fileInfo.etag()));
            }
            fileInfos.add(fileInfo);
            Files.writeString(metaFile.toPath(), gson.toJson(fileInfos));
            return matchFileEtag;
        }
    }

    public boolean download() {
        taskStatus = TaskStatus.DOWNLOADING;
        Optional<FileInfo> fileInfoOpt = HttpUtil.getHttpFileInfo(this.url, this.downloadDir);
        logger.info("FileInfo: %s", fileInfoOpt.isPresent() ? fileInfoOpt.get() : "null");
        if (fileInfoOpt.isPresent()) {
            FileInfo fileInfo = fileInfoOpt.get();
            try {
                // 0. check downloadDir
                if (!FileUtil.checkAndCreateDir(this.downloadDir)) {
                    logger.error("Failed to create download dir: %s", this.downloadDir);
                }
                // 1. check file etag
                if (checkFileEtag(fileInfo)) {
                    logger.error("File already exists: %s", fileInfo.fileName());
                    taskStatus = TaskStatus.FINISHED;
                    return true;
                }
                // 2. split and build DownloadPartialTask
                splitTask(fileInfo);
                boolean res = true;
                List<Future<Pair<String, Boolean>>> futures = PART_TASK_WORKER_POOL.invokeAll(partialTasks);
                Set<String> tempFiles = new HashSet<>();
                for (Future<Pair<String, Boolean>> future : futures) {
                    // todo support retry
                    Pair<String, Boolean> result = future.get();
                    tempFiles.add(result.getLeft());
                    res &= result.getRight();
                }
                // 3. merge download files
                if (res) {
                    res &= mergeTaskFile(fileInfo, tempFiles);
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
