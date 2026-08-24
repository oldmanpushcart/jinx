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
     * 任务配置文件后缀
     */
    String CRON_FILE_SUFFIX = ".cron.json";

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
     * 重加载指定任务
     *
     * @param name 任务名
     * @return 重加载后的任务定义，任务不存在时返回 null
     */
    CronMeta reload(String name);

}
