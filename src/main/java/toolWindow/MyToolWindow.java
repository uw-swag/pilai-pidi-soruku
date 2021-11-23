// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package toolWindow;

import ca.uwaterloo.swag.pilaipidi.PilaiPidi;
import ca.uwaterloo.swag.pilaipidi.models.DFGNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.WindowManager;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import net.miginfocom.swing.MigLayout;
import popupMenu.TreeNodeSelector;
import popupMenu.PilaiPidiPopupMenu;

public class MyToolWindow {

    private JButton refreshToolWindowButton;
    private JButton hideToolWindowButton;
    private JPanel myToolWindowContent;
    private JTree filesInConnection;
    @SuppressWarnings("unused")
    private JScrollPane scrollerWindow;
    private JPanel myStatusContent;
    private final JLabel label = new JLabel("Loading...");
    private final TreeNodeSelector nodeSelector = new TreeNodeSelector();

    private Project activeProject = null;
    private TreePath selectedPath;

    private void loadingPanel() {
        JPanel panel = myStatusContent;
        panel.setVisible(false);
        panel.setLayout(new MigLayout());
        java.net.URL imageURL = this.getClass().getClassLoader().getResource("img/load.gif");
        assert imageURL != null;
        ImageIcon imageIcon = new ImageIcon(imageURL);
        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(imageIcon);
        imageIcon.setImageObserver(iconLabel);
        panel.add(label, "push, align center");
        panel.add(iconLabel, "push, align center");
    }

    public MyToolWindow(ToolWindow toolWindow) {
        DefaultTreeModel model = (DefaultTreeModel) filesInConnection.getModel();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Please press run to begin Analysis");
        model.setRoot(root);
        hideToolWindowButton.addActionListener(actionEvent -> toolWindow.hide(null));
        refreshToolWindowButton.addActionListener(actionEvent -> runPilaiPidi());
        final PilaiPidiPopupMenu pilaiPidiPopupMenu = new PilaiPidiPopupMenu(activeProject, selectedPath);
        filesInConnection.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent mouseEvent) {
                selectedPath = filesInConnection.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
                if (mouseEvent.isPopupTrigger() &&
                    selectedPath.getLastPathComponent().toString().matches(".*\\d.*")) {
                    pilaiPidiPopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        filesInConnection.getSelectionModel().addTreeSelectionListener(nodeSelector);
        filesInConnection.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    public void runPilaiPidi() {
        myStatusContent.setVisible(true);
        filesInConnection.setVisible(false);
        String[] arguments = {"location"};
        activeProject = getActiveProject();
        if (activeProject == null) {
            return;
        }
        nodeSelector.setActiveProject(activeProject);
        arguments[0] = activeProject.getBasePath();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Possible Buffer Overflow Violations");
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        final PilaiPidi pilaiPidi = new PilaiPidi();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Hashtable<String, Set<List<DFGNode>>> sourceToSinkPaths = pilaiPidi.invoke(arguments);
            for (Map.Entry<String, Set<List<DFGNode>>> entry : sourceToSinkPaths.entrySet()) {
                populateTreeView(root, entry);
            }
            myStatusContent.setVisible(false);
            filesInConnection.setVisible(true);
            filesInConnection.setModel(treeModel);
            filesInConnection.setRootVisible(true);
        });
    }

    public static Project getActiveProject() {
        Project activeProject = null;
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            Window window = WindowManager.getInstance().suggestParentWindow(project);
            if (window != null && window.isActive()) {
                activeProject = project;
            }
        }

        return activeProject;
    }

    public static void populateTreeView(DefaultMutableTreeNode root,
                                        Entry<String, Set<List<DFGNode>>> sourceToSinkPath) {
        String violationStr = sourceToSinkPath.getKey();
        Set<List<DFGNode>> currentViolation = sourceToSinkPath.getValue();
        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(violationStr);
        root.add(currentNode);
        currentViolation.forEach(violations -> {
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode("Violation Path");
            violations.forEach(el -> treeNode.add(new DefaultMutableTreeNode(el)));
            treeNode.add(new DefaultMutableTreeNode(violationStr));
            currentNode.add(treeNode);
        });
    }

    public JPanel getContent() {
        loadingPanel();
        return myToolWindowContent;
    }
}
