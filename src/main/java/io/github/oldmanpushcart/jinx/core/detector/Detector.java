package io.github.oldmanpushcart.jinx.core.detector;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * 探测器
 * <p>
 * 探测并管理一组有名称的对象，提供列举、查询与重加载能力。
 * </p>
 *
 * @param <T> 探测对象类型
 */
public interface Detector<T> {

    /**
     * @return 已探测到的对象
     */
    List<T> list();

    /**
     * 获取指定名称的对象
     *
     * @param name 名称
     * @return 对象
     */
    Optional<T> get(String name);

    /**
     * 重加载指定名称的对象
     *
     * @param name 名称
     * @return 重加载后的对象
     */
    CompletionStage<T> reload(String name);

}
