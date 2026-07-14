package com.endpointexplorer.toolwindow;

import com.endpointexplorer.model.ControllerNode;
import com.endpointexplorer.model.EndpointData;
import com.intellij.icons.AllIcons;
import com.intellij.ui.ColorUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Custom tree cell renderer that shows colored HTTP method badges.
 */
public class EndpointTreeCellRenderer extends DefaultTreeCellRenderer {

    // Method badge colors
    private static final Color GET_COLOR = new Color(0x1B8A4B);      // green
    private static final Color POST_COLOR = new Color(0x1A6FB5);     // blue
    private static final Color PUT_COLOR = new Color(0xBF8700);      // amber
    private static final Color DELETE_COLOR = new Color(0xC62828);   // red
    private static final Color PATCH_COLOR = new Color(0x8E24AA);    // purple
    private static final Color DEFAULT_COLOR = new Color(0x666666);

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        JLabel label = (JLabel) super.getTreeCellRendererComponent(
                tree, value, sel, expanded, leaf, row, hasFocus
        );

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();

        if (userObject instanceof EndpointData) {
            renderEndpoint(label, (EndpointData) userObject);
        } else if (userObject instanceof ControllerNode) {
            renderController(label, (ControllerNode) userObject);
        } else {
            // Root node
            label.setIcon(AllIcons.Nodes.Folder);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
        }

        return label;
    }

    private void renderEndpoint(JLabel label, EndpointData endpoint) {
        String method = endpoint.getHttpMethod();
        Color badgeColor = getMethodColor(method);

        // Use HTML to render a colored badge
        String hexColor = ColorUtil.toHex(badgeColor);
        String methodBadge = "<span style='color:#" + hexColor + ";font-weight:bold;font-family:monospace;'>"
                + method + "</span>";
        String pathText = endpoint.getFullPath();
        String detail = " -> " + endpoint.getMethodName() + "()";

        String html = "<html>" + methodBadge + " <b>" + pathText + "</b>"
                + "<span style='color:#888;'>" + detail + "</span></html>";
        label.setText(html);

        label.setIcon(AllIcons.Actions.IntentionBulb);
        label.setToolTipText(endpoint.getDetailText());
    }

    private void renderController(JLabel label, ControllerNode controller) {
        int count = controller.getEndpointCount();
        String text = controller.getDisplayLabel()
                + "  <span style='color:#888;font-size:90%;'>(" + count + ")</span>";
        label.setText("<html>" + text + "</html>");
        label.setIcon(AllIcons.Nodes.Folder);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setToolTipText(controller.getControllerName() + " with " + count + " endpoints");
    }

    private Color getMethodColor(String method) {
        if (method == null || method.isEmpty()) return DEFAULT_COLOR;
        String m = method.toUpperCase();
        if (m.equals("GET")) return GET_COLOR;
        if (m.equals("POST")) return POST_COLOR;
        if (m.equals("PUT")) return PUT_COLOR;
        if (m.equals("DELETE")) return DELETE_COLOR;
        if (m.equals("PATCH")) return PATCH_COLOR;
        return DEFAULT_COLOR;
    }
}
