// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package flowWindow;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;

import static com.intellij.psi.search.FilenameIndex.getFilesByName;

@SuppressWarnings("SpellCheckingInspection")
public class MyFlowWindow {

  private JButton hideToolWindowButton;
  private JPanel myToolWindowContent;
  private JTree filesInConnection;
  @SuppressWarnings("unused")
  private JScrollPane scrollerWindow;

  private Project activeProject = null;
  private TreePath c_path;

  class TreePopup extends JPopupMenu {
    private void cppBreakNotification() {
      String GROUP_DISPLAY_ID = "SrcBuggy";
      String messageTitle = "Warning CPP Breakpoint";
      String messageDetails = "Method lies in a cpp files which cannot be debugged with Idea";
      Notification notification = new Notification(GROUP_DISPLAY_ID, GROUP_DISPLAY_ID + ": " + messageTitle, messageDetails, NotificationType.INFORMATION);
      Notifications.Bus.notify(notification);
    }
    public TreePopup() {
      JMenuItem add = new JMenuItem("breakpoint");
      add.addActionListener(ae -> {
        DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) c_path.getLastPathComponent();
        if (lastPathComponent.toString().split(",").length>1){
          String[] target = lastPathComponent.toString().split(",");
          String pos = target[target.length-1];
          Path file = Paths.get(target[target.length - 2]);
          if(file.toString().endsWith(".cpp")){
            cppBreakNotification();
          }
          else{
            addLineBreakpoint(activeProject,file.toString(),Integer.parseInt(pos));
          }
        }
//    for logic below to work violated node must be in the same doc as the exception point "... at [...]"
        else {
          String[] modTarget;
          if(lastPathComponent.getParent().toString().equals("Possible Buffer Overflow Violations")){
            modTarget = lastPathComponent.getChildAt(Collections.list(lastPathComponent.children()).size()-2).toString().split(",");
          }
          else{
            modTarget = lastPathComponent.getParent().getChildAt(Collections.list(lastPathComponent.getParent().children()).size()-2).toString().split(",");
          }
          String[] target = lastPathComponent.toString().split(" ");
          String pos = target[target.length-1];
          Path file = Paths.get(modTarget[modTarget.length - 2]);
          if(file.toString().endsWith(".cpp")){
            cppBreakNotification();
          }
          else{
            addLineBreakpoint(activeProject,file.toString(),Integer.parseInt(pos));
          }
        }
      });
      add(add);
      add(new JSeparator());
    }
  }

  public MyFlowWindow(ToolWindow toolWindow) {
    DefaultTreeModel model =(DefaultTreeModel) filesInConnection.getModel();
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Please right click at required point and start Analysis");
    model.setRoot(root);
    hideToolWindowButton.addActionListener(e -> toolWindow.hide(null));
    final TreePopup treePopup = new TreePopup();
    filesInConnection.addMouseListener(new MouseAdapter() {
      public void mouseReleased(MouseEvent e) {
        c_path = filesInConnection.getPathForLocation(e.getX(), e.getY());
        if(e.isPopupTrigger() && c_path.getLastPathComponent().toString().matches(".*\\d.*")) {
          treePopup.show(e.getComponent(), e.getX(), e.getY());
        }
      }
    });
    filesInConnection.getSelectionModel().addTreeSelectionListener(new Selector());
    filesInConnection.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
//    this.runSrcBuggy();
  }

  public class Selector implements TreeSelectionListener {
    public void valueChanged(TreeSelectionEvent e) {
      try{
        DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) e.getNewLeadSelectionPath().getLastPathComponent();
        if (lastPathComponent.toString().split(",").length>1){
          String[] target = e.getNewLeadSelectionPath().getLastPathComponent().toString().split(",");
          String pos = target[target.length-1];
          goToPos(target, pos);
        }
//    for logic below to work violated node must be in the same doc as the exception point "... at [...]"
        else {
          String[] modTarget;
          if(lastPathComponent.getParent().toString().equals("Possible Buffer Overflow Violations")){
            modTarget = lastPathComponent.getChildAt(Collections.list(lastPathComponent.children()).size()-2).toString().split(",");
          }
          else{
            modTarget = lastPathComponent.getParent().getChildAt(Collections.list(lastPathComponent.getParent().children()).size()-2).toString().split(",");
          }
          String[] target = lastPathComponent.toString().split(" ");
          String pos = target[target.length-1];
          goToPos(modTarget, pos);
        }
      }
      catch (Exception error){
        System.out.println("Nothing to check");
      }
    }

  }

  public JPanel getContent() {
    return myToolWindowContent;
  }

  private void addLineBreakpoint(final Project project, final String fileUrl, final int line) {
    class MyBreakpointProperties extends XBreakpointProperties<MyBreakpointProperties> {
      public String myOption;

      public MyBreakpointProperties() {}

      @Override
      public MyBreakpointProperties getState() {
        return this;
      }

      @Override
      public void loadState(final MyBreakpointProperties state) {
        myOption = state.myOption;
      }
    }

    class MyLineBreakpointType extends XLineBreakpointType<MyBreakpointProperties> {
      public MyLineBreakpointType() {
        super("testId", "testTitle");
      }

      @Override
      public MyBreakpointProperties createBreakpointProperties(@NotNull VirtualFile file, final int line) {
        return null;
      }

      @Override
      public MyBreakpointProperties createProperties() {
        return new MyBreakpointProperties();
      }
    }

    final XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
    final MyLineBreakpointType MY_LINE_BREAKPOINT_TYPE = new MyLineBreakpointType();
    final MyBreakpointProperties MY_LINE_BREAKPOINT_PROPERTIES = new MyBreakpointProperties();

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
    XDebuggerUtil.getInstance().toggleLineBreakpoint(project, virtualFile, line-1);
  }

  private void goToPos(String[] target, String poscol) {
    String[] poscolarr = poscol.split(":");
    String pos = poscolarr[0];
    String col = poscolarr[1];
    Path file = Paths.get(target[target.length - 2]);
    PsiFile targetFile = getFilesByName(activeProject,file.getFileName().toString(), GlobalSearchScope.allScope(activeProject))[0];
    if(Paths.get(targetFile.getVirtualFile().getPath()).toString().equals(file.toString())){
      new OpenFileDescriptor(activeProject,targetFile.getVirtualFile(), Objects.requireNonNull(targetFile.getViewProvider().getDocument()).getLineStartOffset(Integer.parseInt(pos)-1)+Integer.parseInt(col)-1).navigate(true);
    }
  }
}
