package popupMenu;

import static com.intellij.psi.search.FilenameIndex.getFilesByName;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class TreeNodeSelector implements TreeSelectionListener {

    private Project activeProject;

    public void valueChanged(TreeSelectionEvent event) {
        try {
            DefaultMutableTreeNode lastPathComponent =
                (DefaultMutableTreeNode) event.getNewLeadSelectionPath().getLastPathComponent();
            String lastCompStr = lastPathComponent.toString();
            if (lastCompStr.contains("at")) {
                String[] posParts = lastCompStr.split("at");
                String pos = posParts[posParts.length - 1].trim();
                String[] parts = pos.split(",");
                if (parts.length > 1) {
                    goToPosition(parts, parts[parts.length - 1], activeProject);
                }
            } else if (lastCompStr.split(",").length > 1) {
                String[] target = lastCompStr.split(",");
                goToPosition(target, target[target.length - 1], activeProject);
            } else { //for logic below to work violated node must be in the same doc as the exception point "... at [...]"
                String[] modTarget;
                TreeNode parent = lastPathComponent.getParent();
                int index = lastPathComponent.getChildCount() - 2;
                if (parent.toString().equals("Possible data flow paths")) {
                    modTarget = lastPathComponent.getChildAt(index).toString().split(",");
                } else {
                    modTarget = parent.getChildAt(index).toString().split(",");
                }
                String[] target = lastCompStr.split(" ");
                goToPosition(modTarget, target[target.length - 1], activeProject);
            }
        } catch (Exception ignore) {
            System.out.println("Nothing to check");
        }
    }

    public static void goToPosition(String[] target, String lineAndColPos, Project activeProject) {
        String[] lineAndCol = lineAndColPos.split(":");
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

    public void setActiveProject(Project activeProject) {
        this.activeProject = activeProject;
    }
}
