package com.endpointexplorer.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件配置存储。
 * 用户在配置弹窗里添加的自定义注解会持久化到这里，
 * IDEA 重启后依然有效。
 *
 * 存储位置：C:\Users\xxx\AppData\Roaming\JetBrains\IntelliJIdea2021.1\options\endpointExplorer.xml
 */
@State(
        name = "EndpointExplorerSettings",
        storages = @Storage("endpointExplorer.xml")
)
public class EndpointExplorerSettings implements PersistentStateComponent<EndpointExplorerSettings.State> {

    public static class State {
        public List<String> customControllerAnnotations = new ArrayList<>();
        public List<String> customMappingAnnotations = new ArrayList<>();
    }

    private State state = new State();

    /**
     * 获取配置实例（单例）。
     * 和 Spring 的 @Autowired 一个道理——从 IDEA 容器里拿这个 Bean。
     */
    public static EndpointExplorerSettings getInstance() {
        return ApplicationManager.getApplication().getService(EndpointExplorerSettings.class);
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    // ========== 便捷方法 ==========

    public List<String> getCustomControllerAnnotations() {
        return state.customControllerAnnotations;
    }

    public List<String> getCustomMappingAnnotations() {
        return state.customMappingAnnotations;
    }

    public void setCustomControllerAnnotations(List<String> annotations) {
        state.customControllerAnnotations = new ArrayList<>(annotations);
    }

    public void setCustomMappingAnnotations(List<String> annotations) {
        state.customMappingAnnotations = new ArrayList<>(annotations);
    }
}
