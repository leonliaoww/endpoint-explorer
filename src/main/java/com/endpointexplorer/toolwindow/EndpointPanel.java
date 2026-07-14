package com.endpointexplorer.toolwindow;

import com.endpointexplorer.model.ControllerNode;
import com.endpointexplorer.model.EndpointData;
import com.endpointexplorer.scanner.EndpointScanner;
import com.endpointexplorer.settings.SettingsDialog;
import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Main panel of the Endpoint Explorer tool window.
 */
public class EndpointPanel extends SimpleToolWindowPanel implements DumbAware {

    private final Project project;
    private final Tree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
    private final JLabel statusLabel;
    private final SearchTextField searchField;

    private List<ControllerNode> controllers;

    public EndpointPanel(Project project) {
        super(true, true); // vertical layout, no horizontal scroll

        this.project = project;

        // ---- Tree ----
        rootNode = new DefaultMutableTreeNode("Spring Endpoints");
        treeModel = new DefaultTreeModel(rootNode);
        tree = new Tree(treeModel);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new EndpointTreeCellRenderer());
        tree.setRowHeight(24);

        // ---- Search field ----
        searchField = new SearchTextField();
        searchField.getTextEditor().setToolTipText("Search endpoints by path, method, or controller...");
        searchField.addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterTree(); }
            @Override public void removeUpdate(DocumentEvent e) { filterTree(); }
            @Override public void changedUpdate(DocumentEvent e) { filterTree(); }
        });

        // ---- Toolbar (native IntelliJ style via ActionToolbar) ----
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBorder(null);

        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new AnAction("Refresh", "Rescan all controllers", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                scanProject();
            }
        });

        actionGroup.addSeparator();

        actionGroup.add(new AnAction("Expand All", "Expand all controller nodes", AllIcons.Actions.Expandall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                expandAll();
            }
        });

        actionGroup.add(new AnAction("Collapse All", "Collapse all controller nodes", AllIcons.Actions.Collapseall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                collapseAll();
            }
        });

        actionGroup.addSeparator();

        actionGroup.add(new AnAction("Settings", "Configure custom annotations", AllIcons.General.Settings) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                SettingsDialog dialog = new SettingsDialog();
                dialog.show();
                // 点 OK 后重新扫描
                if (dialog.isOK()) {
                    scanProject();
                }
            }
        });

        ActionToolbar actionToolbar = ActionManager.getInstance()
                .createActionToolbar("EndpointExplorer", actionGroup, true);
        actionToolbar.setTargetComponent(this);
        toolbarPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);
        toolbarPanel.add(searchField, BorderLayout.CENTER);

        setToolbar(toolbarPanel);

        // ---- Status label ----
        statusLabel = new JLabel("Click Refresh to scan endpoints");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC, 11f));

        // ---- Tree + status in scroll pane ----
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(new JScrollPane(tree), BorderLayout.CENTER);
        contentPanel.add(statusLabel, BorderLayout.SOUTH);

        setContent(contentPanel);

        // ---- Double-click to navigate ----
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    navigateToSource();
                }
            }
        });

        // ---- Auto scan on first open ----
        scanProject();
    }

    // ========== Scanning ==========

    private void scanProject() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Scanning Spring Endpoints...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Scanning Spring Boot controllers...");

                EndpointScanner scanner = new EndpointScanner(project);
                controllers = scanner.scan(indicator);

                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    String debugInfo = "Java files: " + scanner.getJavaFileCount()
                            + " | " + countEndpoints() + " endpoints in "
                            + controllers.size() + " controllers";
                    statusLabel.setText(debugInfo);
                    buildTree();
                });
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Scan failed: " + error.getMessage());
                    NotificationGroup notificationGroup = new NotificationGroup("Endpoint Explorer", NotificationDisplayType.BALLOON, true);
                    notificationGroup.createNotification("Scan failed: " + error.getMessage(), NotificationType.ERROR)
                            .notify(project);
                });
            }
        });
    }

    // ========== Tree building ==========

    private void buildTree() {
        rootNode.removeAllChildren();

        if (controllers == null || controllers.isEmpty()) {
            rootNode.add(new DefaultMutableTreeNode("No Spring Boot controllers found"));
            treeModel.reload();
            return;
        }

        for (ControllerNode controller : controllers) {
            DefaultMutableTreeNode controllerNode = new DefaultMutableTreeNode(controller);

            for (EndpointData endpoint : controller.getEndpoints()) {
                controllerNode.add(new DefaultMutableTreeNode(endpoint));
            }

            rootNode.add(controllerNode);
        }

        treeModel.reload();
        expandFirstLevel();
    }

    private void filterTree() {
        String query = searchField.getText().trim().toLowerCase();
        rootNode.removeAllChildren();

        if (controllers == null || controllers.isEmpty()) {
            treeModel.reload();
            return;
        }

        for (ControllerNode controller : controllers) {
            DefaultMutableTreeNode controllerNode = new DefaultMutableTreeNode(controller);
            boolean controllerMatches = controller.getDisplayLabel().toLowerCase().contains(query);
            boolean hasChildMatch = false;

            for (EndpointData endpoint : controller.getEndpoints()) {
                boolean matches = controllerMatches
                        || endpoint.getDisplayLabel().toLowerCase().contains(query)
                        || endpoint.getMethodName().toLowerCase().contains(query)
                        || endpoint.getHttpMethod().toLowerCase().contains(query);
                if (matches) {
                    controllerNode.add(new DefaultMutableTreeNode(endpoint));
                    hasChildMatch = true;
                }
            }

            if (controllerMatches || hasChildMatch) {
                rootNode.add(controllerNode);
            }
        }

        treeModel.reload();
        if (!query.isEmpty()) {
            expandAll();
        }
    }

    // ========== Navigation ==========

    private void navigateToSource() {
        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();

        if (userObject instanceof EndpointData) {
            EndpointData endpoint = (EndpointData) userObject;
            PsiMethod psiMethod = endpoint.getPsiMethod();
            if (psiMethod != null && psiMethod.isValid()) {
                psiMethod.navigate(true);
            }
        } else if (userObject instanceof ControllerNode) {
            ControllerNode controller = (ControllerNode) userObject;
            // Navigate to first endpoint of this controller
            List<EndpointData> endpoints = controller.getEndpoints();
            if (!endpoints.isEmpty()) {
                PsiMethod psiMethod = endpoints.get(0).getPsiMethod();
                if (psiMethod != null && psiMethod.isValid()) {
                    psiMethod.navigate(true);
                }
            }
        }
    }

    // ========== Tree state helpers ==========

    private void expandFirstLevel() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void collapseAll() {
        for (int i = tree.getRowCount() - 1; i >= 0; i--) {
            tree.collapseRow(i);
        }
    }

    private int countEndpoints() {
        if (controllers == null) return 0;
        return controllers.stream().mapToInt(ControllerNode::getEndpointCount).sum();
    }
}
