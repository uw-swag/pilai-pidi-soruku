// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package flowWindow;

import static com.intellij.psi.search.FilenameIndex.getFilesByName;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import popupMenu.PilaiPidiPopupMenu;

@SuppressWarnings("SpellCheckingInspection")
public class MyFlowWindow {

    private JButton hideToolWindowButton;
    private JPanel myToolWindowContent;
    private JTree filesInConnection;
    @SuppressWarnings("unused")
    private JScrollPane scrollerWindow;

    private final Project activeProject = null;
    private TreePath c_path;

    public MyFlowWindow(ToolWindow toolWindow) {
        DefaultTreeModel model = (DefaultTreeModel) filesInConnection.getModel();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(
            "Please right click at required point and start Analysis");
        model.setRoot(root);
        hideToolWindowButton.addActionListener(e -> toolWindow.hide(null));
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
                DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) event.getNewLeadSelectionPath()
                    .getLastPathComponent();
                if (lastPathComponent.toString().split(",").length > 1) {
                    String[] target = event.getNewLeadSelectionPath().getLastPathComponent().toString().split(",");
                    String pos = target[target.length - 1];
                    goToPossition(target, pos, activeProject);
                } else { // for logic below to work violated node must be in the same doc as the exception point "... at [...]"
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
                    goToPossition(modTarget, pos, activeProject);
                }
            } catch (Exception ignore) {
                System.out.println("Nothing to check");
            }
        }
    }

    public JPanel getContent() {
        return myToolWindowContent;
    }

    public static void goToPossition(String[] target, String poscol, Project activeProject) {
        String[] lineAndCol = poscol.split(":");
        String lineNumber = lineAndCol[0];
        String colNumber = lineAndCol[1];
        Path file = Paths.get(target[target.length - 2]);
        PsiFile targetFile = getFilesByName(activeProject, file.getFileName().toString(),
            GlobalSearchScope.allScope(activeProject))[0];
        if (Paths.get(targetFile.getVirtualFile().getPath()).toString().equals(file.toString())) {
            new OpenFileDescriptor(activeProject, targetFile.getVirtualFile(),
                Objects.requireNonNull(targetFile.getViewProvider().getDocument())
                    .getLineStartOffset(Integer.parseInt(lineNumber) - 1) + Integer.parseInt(colNumber) - 1)
                .navigate(true);
        }
    }
}
