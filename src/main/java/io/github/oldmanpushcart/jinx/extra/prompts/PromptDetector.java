package io.github.oldmanpushcart.jinx.extra.prompts;

import io.github.oldmanpushcart.jinx.core.detector.Detector;

/**
 * 提示词探测器
 * <p>
 * 探测{@code {jinx.data}/prompts/{stage}}目录下的{@code *.md}提示词文件。
 * </p>
 */
public interface PromptDetector extends Detector<PromptMeta> {

    /**
     * @return 探测的阶段
     */
    PromptPhase phase();

}
