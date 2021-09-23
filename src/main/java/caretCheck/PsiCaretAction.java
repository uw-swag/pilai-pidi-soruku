package caretCheck;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;

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
        @NotNull String offset = currentCaret.getLogicalPosition().toString();
        @NotNull String fileName = vFile.getPath();
        assert project != null;
        ToolWindow myToolWindow = ToolWindowManager.getInstance(project).getToolWindow("Pilai Pidi");
        assert myToolWindow != null;
        final JTree filesInConnection = (JTree) ((JScrollPane) Objects.requireNonNull(myToolWindow.getContentManager().getContent(0)).getComponent().getComponent(2)).getViewport().getComponent(0);
        if (filesInConnection != null) {
            myToolWindow.show(null);
            filesInConnection.setVisible(true);
            Messages.showMessageDialog(project, filesInConnection.toString(), "PSI Info", null);
        }
        Messages.showMessageDialog(project, fileName+" "+offset, "PSI Info", null);
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(editor != null && psiFile != null);
    }

}