package com.endpointexplorer.settings;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义注解配置弹窗。
 * 点击工具栏的齿轮按钮时弹出，让用户可以添加/删除自定义注解。
 */
public class SettingsDialog extends DialogWrapper {

    // Controller 注解区域
    private final DefaultListModel<String> controllerListModel = new DefaultListModel<>();
    private final JList<String> controllerList = new JList<>(controllerListModel);
    private final JTextField controllerInput = new JTextField();

    // Mapping 注解区域
    private final DefaultListModel<String> mappingListModel = new DefaultListModel<>();
    private final JList<String> mappingList = new JList<>(mappingListModel);
    private final JTextField mappingInput = new JTextField();

    public SettingsDialog() {
        super(true); // true = 模态弹窗（弹出来必须关掉才能操作 IDEA）
        setTitle("Configure Annotations");
        setSize(500, 400);

        // 加载已保存的配置
        EndpointExplorerSettings settings = EndpointExplorerSettings.getInstance();
        for (String ann : settings.getCustomControllerAnnotations()) {
            controllerListModel.addElement(ann);
        }
        for (String ann : settings.getCustomMappingAnnotations()) {
            mappingListModel.addElement(ann);
        }

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── 上半部分：Controller 注解 ──
        root.add(createControllerSection(), BorderLayout.NORTH);

        // ── 下半部分：Mapping 注解 ──
        root.add(createMappingSection(), BorderLayout.CENTER);

        // ── 提示文字 ──
        JLabel hint = new JLabel(
                "<html><body style='color:#888;font-size:11px;'>" +
                "格式说明：<br>" +
                "• Controller 注解：输入注解名即可，如 <code>MyController</code><br>" +
                "• Mapping 注解：输入 <code>注解名:HTTP方法</code>，如 <code>MyGetMapping:GET</code>" +
                "</body></html>"
        );
        hint.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        root.add(hint, BorderLayout.SOUTH);

        return root;
    }

    private JPanel createControllerSection() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Custom Controller Annotations"));

        // 输入行：文本框 + 添加按钮
        JPanel inputRow = new JPanel(new BorderLayout(5, 0));
        controllerInput.setToolTipText("e.g. MyController (不加 @ 符号)");
        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> addControllerAnnotation());
        inputRow.add(controllerInput, BorderLayout.CENTER);
        inputRow.add(addBtn, BorderLayout.EAST);

        // 列表 + 删除按钮
        JPanel listPanel = new JPanel(new BorderLayout(5, 0));
        listPanel.add(new JBScrollPane(controllerList), BorderLayout.CENTER);
        JButton removeBtn = new JButton("Remove");
        removeBtn.addActionListener(e -> {
            int selected = controllerList.getSelectedIndex();
            if (selected >= 0) controllerListModel.remove(selected);
        });
        listPanel.add(removeBtn, BorderLayout.EAST);

        panel.add(inputRow, BorderLayout.NORTH);
        panel.add(listPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMappingSection() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Custom Mapping Annotations"));

        // 输入行：文本框 + 添加按钮
        JPanel inputRow = new JPanel(new BorderLayout(5, 0));
        mappingInput.setToolTipText("e.g. MyGetMapping:GET (注解名:HTTP方法)");
        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> addMappingAnnotation());
        inputRow.add(mappingInput, BorderLayout.CENTER);
        inputRow.add(addBtn, BorderLayout.EAST);

        // 列表 + 删除按钮
        JPanel listPanel = new JPanel(new BorderLayout(5, 0));
        listPanel.add(new JBScrollPane(mappingList), BorderLayout.CENTER);
        JButton removeBtn = new JButton("Remove");
        removeBtn.addActionListener(e -> {
            int selected = mappingList.getSelectedIndex();
            if (selected >= 0) mappingListModel.remove(selected);
        });
        listPanel.add(removeBtn, BorderLayout.EAST);

        panel.add(inputRow, BorderLayout.NORTH);
        panel.add(listPanel, BorderLayout.CENTER);
        return panel;
    }

    private void addControllerAnnotation() {
        String text = controllerInput.getText().trim();
        if (text.isEmpty()) return;
        // 去掉可能输入的 @ 符号
        if (text.startsWith("@")) text = text.substring(1);
        if (!controllerListModel.contains(text)) {
            controllerListModel.addElement(text);
        }
        controllerInput.setText("");
    }

    private void addMappingAnnotation() {
        String text = mappingInput.getText().trim();
        if (text.isEmpty()) return;
        // 去掉可能输入的 @ 符号
        if (text.startsWith("@")) text = text.substring(1);
        // 校验格式：必须有冒号
        if (!text.contains(":")) {
            mappingInput.setText("");
            return;
        }
        if (!mappingListModel.contains(text)) {
            mappingListModel.addElement(text);
        }
        mappingInput.setText("");
    }

    /**
     * 用户点击 OK 时保存配置。
     */
    @Override
    protected void doOKAction() {
        EndpointExplorerSettings settings = EndpointExplorerSettings.getInstance();

        // 把 JList 里的数据写回配置
        List<String> controllers = new ArrayList<>();
        for (int i = 0; i < controllerListModel.size(); i++) {
            controllers.add(controllerListModel.get(i));
        }
        settings.setCustomControllerAnnotations(controllers);

        List<String> mappings = new ArrayList<>();
        for (int i = 0; i < mappingListModel.size(); i++) {
            mappings.add(mappingListModel.get(i));
        }
        settings.setCustomMappingAnnotations(mappings);

        super.doOKAction();
    }
}
