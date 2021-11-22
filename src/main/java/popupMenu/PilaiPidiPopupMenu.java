package popupMenu;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.jetbrains.annotations.NotNull;

public class PilaiPidiPopupMenu extends JPopupMenu {
    private final Project activeProject;
    private final TreePath c_path;

    private void cppBreakNotification() {
        String GROUP_DISPLAY_ID = "PilaiPidi";
        String messageTitle = "Warning CPP Breakpoint";
        String messageDetails = "Method lies in a cpp files which cannot be debugged with Idea";
        Notification notification = new Notification(GROUP_DISPLAY_ID, GROUP_DISPLAY_ID + ": " + messageTitle,
            messageDetails, NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);
    }

    public PilaiPidiPopupMenu(Project activeProject, TreePath c_path) {
        this.activeProject = activeProject;
        this.c_path = c_path;
        init();
    }

    public void init() {
        JMenuItem add = new JMenuItem("breakpoint");
        add.addActionListener(ae -> {
            DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) c_path.getLastPathComponent();
            if (lastPathComponent.toString().split(",").length > 1) {
                String[] target = lastPathComponent.toString().split(",");
                addBreakPoint(target, target[target.length - 2]);
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
                addBreakPoint(target, modTarget[modTarget.length - 2]);
            }
        });
        add(add);
        add(new JSeparator());
    }

    private void addBreakPoint(String[] target, String source) {
        String pos = target[target.length - 1];
        Path file = Paths.get(source);
        if (file.toString().endsWith(".cpp")) {
            cppBreakNotification();
        } else {
            addLineBreakpoint(activeProject, file.toString(), Integer.parseInt(pos));
        }
    }

    private void addLineBreakpoint(final Project project, final String fileUrl, final int line) {
        class BreakpointProperties extends XBreakpointProperties<BreakpointProperties> {

            public String myOption;

            public BreakpointProperties() {
            }

            @Override
            public BreakpointProperties getState() {
                return this;
            }

            @Override
            public void loadState(final BreakpointProperties state) {
                myOption = state.myOption;
            }
        }

        class MyLineBreakpointType extends XLineBreakpointType<BreakpointProperties> {

            public MyLineBreakpointType() {
                super("testId", "testTitle");
            }

            @Override
            public BreakpointProperties createBreakpointProperties(@NotNull VirtualFile file, final int line) {
                return null;
            }

            @Override
            public BreakpointProperties createProperties() {
                return new BreakpointProperties();
            }
        }

        final XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
        final MyLineBreakpointType MY_LINE_BREAKPOINT_TYPE = new MyLineBreakpointType();
        final BreakpointProperties MY_LINE_BREAKPOINT_PROPERTIES = new BreakpointProperties();

        // add new line break point
        Runnable runnable = () -> breakpointManager.addLineBreakpoint(
            MY_LINE_BREAKPOINT_TYPE,
            fileUrl,
            line,
            MY_LINE_BREAKPOINT_PROPERTIES
        );
        WriteCommandAction.runWriteCommandAction(project, runnable);

        // toggle breakpoint to activate
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(new File(fileUrl));
        assert virtualFile != null;
        XDebuggerUtil.getInstance().toggleLineBreakpoint(project, virtualFile, line - 1);
    }

}