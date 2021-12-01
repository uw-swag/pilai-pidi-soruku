// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package flowWindow;

import com.intellij.openapi.wm.ToolWindow;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import popupMenu.TreeNodeSelector;
import popupMenu.PilaiPidiPopupMenu;

public class MyFlowWindow {

    private JButton hideToolWindowButton;
    private JPanel myToolWindowContent;
    private JTree filesInConnection;
    @SuppressWarnings("unused")
    private JScrollPane scrollerWindow;

    private TreePath selectedPath;


    public MyFlowWindow(ToolWindow toolWindow) {
        DefaultTreeModel model = (DefaultTreeModel) filesInConnection.getModel();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(
            "Please right click at required point and start Analysis");
        model.setRoot(root);
        hideToolWindowButton.addActionListener(event -> toolWindow.hide(null));
        final PilaiPidiPopupMenu pilaiPidiPopupMenu = new PilaiPidiPopupMenu(selectedPath);
        filesInConnection.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent mouseEvent) {
                selectedPath = filesInConnection.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
                if (mouseEvent.isPopupTrigger() &&
                    selectedPath.getLastPathComponent().toString().matches(".*\\d.*")) {
                    pilaiPidiPopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        TreeNodeSelector nodeSelector = new TreeNodeSelector();
        filesInConnection.getSelectionModel().addTreeSelectionListener(nodeSelector);
        filesInConnection.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    public JPanel getContent() {
        return myToolWindowContent;
    }
}
