package io.github.oldmanpushcart.jinx.extra.cron;

import io.github.oldmanpushcart.jinx.Constants;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * 定时任务探测器
 */
public interface CronDetector {

    Path CRON_DIR = Constants.DATA.resolve("cron").normalize().toAbsolutePath();

    /**
     * @return 所有任务定义
     */
    List<CronMeta> list();

    /**
     * 获取指定任务
     *
     * @param name 任务名
     * @return 任务定义
     */
    Optional<CronMeta> get(String name);

    /**
     * 创建任务（写入文件并立即调度）
     *
     * @param meta 任务定义
     * @return 创建后的任务定义
     */
    CronMeta create(CronMeta meta);

    /**
     * 重加载指定任务
     *
     * @param name 任务名
     * @return 重加载后的任务定义，任务不存在时返回 null
     */
    CronMeta reload(String name);

    /**
     * 移除任务
     *
     * @param name 任务名
     * @return 移除的任务定义
     */
    CronMeta remove(String name);

    /**
     * 暂停任务
     *
     * @param name 任务名
     * @return 暂停后的任务定义
     */
    CronMeta pause(String name);

    /**
     * 恢复任务
     *
     * @param name 任务名
     * @return 恢复后的任务定义
     */
    CronMeta resume(String name);

}
