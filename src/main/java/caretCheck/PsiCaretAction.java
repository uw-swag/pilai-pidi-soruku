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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import toolWindow.MyToolWindow;

public class PsiCaretAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        Project project = anActionEvent.getProject();
        assert psiFile != null;
        VirtualFile vFile = psiFile.getOriginalFile().getVirtualFile();
        assert editor != null;
        Caret currentCaret = editor.getCaretModel().getCurrentCaret();
        PsiElement elementAt = psiFile.findElementAt(currentCaret.getOffset());
        if (elementAt == null) {
            Messages.showMessageDialog(project, psiFile.getName() + " " + currentCaret.getOffset(),
                "No Sources Detected", null);
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
        }
    }

    @Override
    public void update(AnActionEvent anActionEvent) {
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        anActionEvent.getPresentation().setEnabled(editor != null && psiFile != null);
    }

    public void runPilaiPidi(JTree filesInConnection, Project currentProject, String singleTarget) {
        filesInConnection.setVisible(false);
        String[] arguments = {"location", "-node", "singleTarget"};
        Project activeProject = MyToolWindow.getActiveProject();
        if (activeProject == null) {
            activeProject = currentProject;
        }
        arguments[0] = activeProject.getBasePath();
        arguments[2] = singleTarget;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Possible Buffer Overflow Violations");
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        final PilaiPidi pilaiPidi = new PilaiPidi();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Hashtable<String, Set<java.util.List<DFGNode>>> sourceToSinkPaths = pilaiPidi.invoke(arguments);
            for (Map.Entry<String, Set<java.util.List<DFGNode>>> sourceToSinkPath : sourceToSinkPaths.entrySet()) {
                MyToolWindow.populateTreeView(root, sourceToSinkPath);
            }
            filesInConnection.setVisible(true);
            filesInConnection.setModel(treeModel);
            filesInConnection.setRootVisible(true);
        });
    }


}