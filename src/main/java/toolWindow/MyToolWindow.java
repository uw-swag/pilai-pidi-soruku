// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package toolWindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.noble.Main;
import com.noble.models.Encl_name_pos_tuple;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

import static com.intellij.psi.search.FilenameIndex.getFilesByName;

public class MyToolWindow implements TreeSelectionListener {

  private JButton refreshToolWindowButton;
  private JButton hideToolWindowButton;
  private JPanel myToolWindowContent;
  private JTree filesInConnection;
  @SuppressWarnings("unused")
  private JScrollPane scrollerWindow;

  private Project activeProject = null;
  public MyToolWindow(ToolWindow toolWindow) {
    hideToolWindowButton.addActionListener(e -> toolWindow.hide(null));
    refreshToolWindowButton.addActionListener(e -> runSrcBuggy());

    this.runSrcBuggy();
  }

  public void runSrcBuggy() {
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
    MyToolWindow that = this;
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      Hashtable<String, Set<List<Encl_name_pos_tuple>>> result = Main.main(arguments);

      Enumeration<String> res_to_analyze = result.keys();
      while (res_to_analyze.hasMoreElements()) {
        String key = res_to_analyze.nextElement();
        Set<List<Encl_name_pos_tuple>> current_violation = result.get(key);
        DefaultMutableTreeNode current_node = new DefaultMutableTreeNode(key);
        root.add(current_node);
        current_violation.forEach(v-> {
//          DefaultMutableTreeNode inner_node = new DefaultMutableTreeNode("Violation Path");
          v.forEach(el->current_node.add(new DefaultMutableTreeNode(el)));
//          current_node.add(inner_node);
        });
        current_node.add(new DefaultMutableTreeNode(key));
      }
      filesInConnection.setVisible(true);
      filesInConnection.addTreeSelectionListener(that);
      filesInConnection.setModel(treeModel);
      filesInConnection.setRootVisible(true);
    });

//    timeZone.setIcon(new ImageIcon(getClass().getResource("/toolWindow/Time-zone-icon.png")));
  }

  public JPanel getContent() {
    return myToolWindowContent;
  }

  @Override
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

  private void goToPos(String[] target, String pos) {
    Path file = Paths.get(target[target.length - 2]);
    PsiFile targetFile = getFilesByName(activeProject,file.getFileName().toString(), GlobalSearchScope.allScope(activeProject))[0];
    if(Paths.get(targetFile.getVirtualFile().getPath()).toString().equals(file.toString())){
      new OpenFileDescriptor(activeProject,targetFile.getVirtualFile(), Objects.requireNonNull(targetFile.getViewProvider().getDocument()).getLineStartOffset(Integer.parseInt(pos)-1)).navigate(true);
    }
  }
}
