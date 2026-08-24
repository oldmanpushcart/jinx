package io.github.oldmanpushcart.jinx.extra.cron;

import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.core.detector.Detector;

import java.nio.file.Path;

/**
 * 定时任务探测器
 */
public interface CronDetector extends Detector<CronMeta> {

    Path CRON_DIR = Constants.DATA.resolve("cron").normalize().toAbsolutePath();

    /**
     * 任务配置文件后缀
     */
    String CRON_FILE_SUFFIX = ".cron.json";

}
