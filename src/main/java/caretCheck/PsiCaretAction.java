package caretCheck;

import ca.uwaterloo.swag.pilaipidi.PilaiPidi;
import ca.uwaterloo.swag.pilaipidi.models.DFGNode;
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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import java.awt.Window;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class PsiCaretAction extends AnAction {

    public void runPilaiPidi(JTree filesInConnection, Project activeProject, String singleTarget) {
        filesInConnection.setVisible(false);
        String[] arguments = {"location", "-node", "singleTarget"};
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
        arguments[2] = singleTarget;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Possible Buffer Overflow Violations");
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        final PilaiPidi pilaiPidi = new PilaiPidi();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Hashtable<String, Set<java.util.List<DFGNode>>> result = pilaiPidi.invoke(arguments);
            for (Map.Entry<String, Set<java.util.List<DFGNode>>> entry : result.entrySet()) {
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
            filesInConnection.setVisible(true);
            filesInConnection.setModel(treeModel);
            filesInConnection.setRootVisible(true);
        });
    }


    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        Project project = anActionEvent.getProject();
        assert psiFile != null;
        VirtualFile vFile = psiFile.getOriginalFile().getVirtualFile();
        assert editor != null;
//        psiFile.findElementAt(currentCaret.getOffset()))
        Caret currentCaret = editor.getCaretModel().getCurrentCaret();
        PsiElement elementAt = psiFile.findElementAt(currentCaret.getOffset());
        if (elementAt == null) {
            Messages.showMessageDialog(project, psiFile.getName() +" "+ currentCaret.getOffset(), "No Sources Detected", null);
            return;
        }
        int correctedCaret = (currentCaret.getOffset() - Objects.requireNonNull(elementAt).getTextOffset()) - 1;
        String currentElement = Objects.requireNonNull(elementAt).getText();
        String fileName = vFile.getPath();
        String offset = (currentCaret.getLogicalPosition().line + 1) + ":" + (currentCaret.getLogicalPosition().column
            - correctedCaret);
        String singleTarget = String.join("@AT@", new String[]{currentElement, fileName, offset});

        assert project != null;
        ToolWindow myToolWindow = ToolWindowManager.getInstance(project).getToolWindow("PilaiPidiDataflow");
        assert myToolWindow != null;
        JPanel myStatusContent = (JPanel) Objects.requireNonNull(myToolWindow.getContentManager().getContent(0))
            .getComponent();
        final JTree filesInConnection = (JTree) ((JScrollPane) myStatusContent.getComponent(1)).getViewport()
            .getComponent(0);
        if (filesInConnection != null) {
            myToolWindow.show(null);
            runPilaiPidi(filesInConnection, project, singleTarget);
//            Messages.showMessageDialog(project, filesInConnection.toString(), "PSI Info", null);
        }
//        Messages.showMessageDialog(project, fileName+" "+offset, "PSI Info", null);
    }

    @Override
    public void update(AnActionEvent anActionEvent) {
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        anActionEvent.getPresentation().setEnabled(editor != null && psiFile != null);
    }

}