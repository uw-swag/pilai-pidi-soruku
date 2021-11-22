// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package toolWindow;

import ca.uwaterloo.swag.pilaipidi.PilaiPidi;
import ca.uwaterloo.swag.pilaipidi.models.DFGNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.WindowManager;
import flowWindow.MyFlowWindow;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import net.miginfocom.swing.MigLayout;
import popupMenu.PilaiPidiPopupMenu;

@SuppressWarnings("SpellCheckingInspection")
public class MyToolWindow {

    private JButton refreshToolWindowButton;
    private JButton hideToolWindowButton;
    private JPanel myToolWindowContent;
    private JTree filesInConnection;
    @SuppressWarnings("unused")
    private JScrollPane scrollerWindow;
    private JPanel myStatusContent;
    private final JLabel label = new JLabel("Loading...");

    private Project activeProject = null;
    private TreePath c_path;

    private void loadingPanel() {
        JPanel panel = myStatusContent;
        panel.setVisible(false);
        panel.setLayout(new MigLayout());

        ClassLoader cldr = this.getClass().getClassLoader();
        java.net.URL imageURL = cldr.getResource("img/load.gif");
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
        final PilaiPidiPopupMenu pilaiPidiPopupMenu = new PilaiPidiPopupMenu(activeProject, c_path);
        filesInConnection.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent mouseEvent) {
                c_path = filesInConnection.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
                if (mouseEvent.isPopupTrigger() && c_path.getLastPathComponent().toString().matches(".*\\d.*")) {
                    pilaiPidiPopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        filesInConnection.getSelectionModel().addTreeSelectionListener(new Selector());
        filesInConnection.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    public class Selector implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent event) {
            try {
                DefaultMutableTreeNode lastPathComponent =
                    (DefaultMutableTreeNode) event.getNewLeadSelectionPath().getLastPathComponent();
                if (lastPathComponent.toString().split(",").length > 1) {
                    String[] target = event.getNewLeadSelectionPath().getLastPathComponent().toString().split(",");
                    String pos = target[target.length - 1];
                    goToPos(target, pos);
                } else { //for logic below to work violated node must be in the same doc as the exception point "... at [...]"
                    String[] modTarget;
                    if (lastPathComponent.getParent().toString().equals("Possible Buffer Overflow Violations")) {
                        modTarget = lastPathComponent
                            .getChildAt(Collections.list(lastPathComponent.children()).size() - 2).toString()
                            .split(",");
                    } else {
                        modTarget = lastPathComponent.getParent()
                            .getChildAt(Collections.list(lastPathComponent.getParent().children()).size() - 2)
                            .toString().split(",");
                    }
                    String[] target = lastPathComponent.toString().split(" ");
                    String pos = target[target.length - 1];
                    goToPos(modTarget, pos);
                }
            } catch (Exception ignore) {
                System.out.println("Nothing to check");
            }
        }

    }

    public void runPilaiPidi() {
        myStatusContent.setVisible(true);
        filesInConnection.setVisible(false);
        String[] arguments = {"location"};
        String directory;
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            Window window = WindowManager.getInstance().suggestParentWindow(project);
            if (window != null && window.isActive()) {
                activeProject = project;
            }
        }
        if (activeProject == null) {
            return;
        }
        directory = activeProject.getBasePath();
        arguments[0] = directory;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Possible Buffer Overflow Violations");
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        final PilaiPidi pilaiPidi = new PilaiPidi();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Hashtable<String, Set<List<DFGNode>>> result = pilaiPidi.invoke(arguments);
            for (Map.Entry<String, Set<List<DFGNode>>> entry : result.entrySet()) {
                String key = entry.getKey();
                Set<List<DFGNode>> current_violation = entry.getValue();
                DefaultMutableTreeNode current_node = new DefaultMutableTreeNode(key);
                root.add(current_node);
                current_violation.forEach(v -> {
                    DefaultMutableTreeNode inner_node = new DefaultMutableTreeNode("Violation Path");
                    v.forEach(el -> inner_node.add(new DefaultMutableTreeNode(el)));
                    inner_node.add(new DefaultMutableTreeNode(key));
                    current_node.add(inner_node);
                });
            }
            myStatusContent.setVisible(false);
            filesInConnection.setVisible(true);
            filesInConnection.setModel(treeModel);
            filesInConnection.setRootVisible(true);
        });
    }

    public JPanel getContent() {
        loadingPanel();
        return myToolWindowContent;
    }

    private void goToPos(String[] target, String poscol) {
        MyFlowWindow.goToPossition(target, poscol, activeProject);
    }
}
