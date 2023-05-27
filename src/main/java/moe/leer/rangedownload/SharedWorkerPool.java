package moe.leer.rangedownload;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SharedWorkerPool {
    /**
     * 所有下载任务共享这个分任务线程池
     */
    public static final ExecutorService PART_TASK_WORKER_POOL = Executors.newCachedThreadPool();

    private SharedWorkerPool() {
    }

    public static void shutdown() {
        List<Runnable> runnables = PART_TASK_WORKER_POOL.shutdownNow();
        for (Runnable runnable : runnables) {
            runnable.run();
        }
    }
}
