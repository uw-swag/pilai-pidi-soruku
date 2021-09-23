package caretCheck;

import ca.uwaterloo.swag.Main;
import ca.uwaterloo.swag.models.EnclNamePosTuple;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;
import java.util.*;

public class PsiCaretAction extends AnAction {

    public void runSrcBuggy(JTree filesInConnection, Project activeProject) {
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
        if (activeProject == null) return;
        directory = activeProject.getBasePath();
        arguments[0] = directory;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Possible Buffer Overflow Violations");
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Hashtable<String, Set<java.util.List<EnclNamePosTuple>>> result = Main.nonCLI(arguments);

            Enumeration<String> res_to_analyze = result.keys();
            while (res_to_analyze.hasMoreElements()) {
                String key = res_to_analyze.nextElement();
                Set<List<EnclNamePosTuple>> current_violation = result.get(key);
                DefaultMutableTreeNode current_node = new DefaultMutableTreeNode(key);
                root.add(current_node);
                current_violation.forEach(v-> {
                    DefaultMutableTreeNode inner_node = new DefaultMutableTreeNode("Violation Path");
                    v.forEach(el->inner_node.add(new DefaultMutableTreeNode(el)));
                    inner_node.add(new DefaultMutableTreeNode(key));
                    current_node.add(inner_node);
                });
            }
            filesInConnection.setVisible(true);
            filesInConnection.setModel(treeModel);
            filesInConnection.setRootVisible(true);
        });

//    timeZone.setIcon(new ImageIcon(getClass().getResource("/toolWindow/Time-zone-icon.png")));
    }


    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        Project project = anActionEvent.getProject();
        assert psiFile != null;
        VirtualFile vFile = psiFile.getOriginalFile().getVirtualFile();
        assert editor != null;
        Caret currentCaret = editor.getCaretModel().getCurrentCaret();
        @NotNull String offset = currentCaret.getLogicalPosition().toString();
        @NotNull String fileName = vFile.getPath();
        assert project != null;
        ToolWindow myToolWindow = ToolWindowManager.getInstance(project).getToolWindow("PilaiPidiDataflow");
        assert myToolWindow != null;
        JPanel myStatusContent = (JPanel) Objects.requireNonNull(myToolWindow.getContentManager().getContent(0)).getComponent();
        final JTree filesInConnection = (JTree) ((JScrollPane) myStatusContent.getComponent(1)).getViewport().getComponent(0);
        if (filesInConnection != null) {
            myToolWindow.show(null);
            runSrcBuggy(filesInConnection, project);
//            Messages.showMessageDialog(project, filesInConnection.toString(), "PSI Info", null);
        }
//        Messages.showMessageDialog(project, fileName+" "+offset, "PSI Info", null);
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(editor != null && psiFile != null);
    }

}