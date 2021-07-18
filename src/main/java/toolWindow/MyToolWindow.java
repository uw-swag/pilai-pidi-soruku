// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package toolWindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.WindowManager;
import com.noble.Main;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

public class MyToolWindow implements TreeSelectionListener {

  private JButton refreshToolWindowButton;
  private JButton hideToolWindowButton;
  private JPanel myToolWindowContent;
  private JTree filesInConnection;

  public MyToolWindow(ToolWindow toolWindow) {
    hideToolWindowButton.addActionListener(e -> toolWindow.hide(null));
    refreshToolWindowButton.addActionListener(e -> runSrcBuggy());

    this.runSrcBuggy();
  }

  public void runSrcBuggy() {
    String[] arguments = {"location"};
    String directory;
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    Project activeProject = null;
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

    Hashtable<String, ArrayList<String>> result = Main.main(arguments);

    Enumeration<String> res_to_analyze = result.keys();
    while (res_to_analyze.hasMoreElements()) {
      String key = res_to_analyze.nextElement();
      ArrayList<String> currrent_violation = result.get(key);
      DefaultMutableTreeNode current_node = new DefaultMutableTreeNode(key);
      root.add(current_node);
      currrent_violation.forEach(v->current_node.add(new DefaultMutableTreeNode(v)));
    }

    filesInConnection.addTreeSelectionListener(this);

    filesInConnection.setModel(treeModel);
    filesInConnection.setRootVisible(true);

//    timeZone.setIcon(new ImageIcon(getClass().getResource("/toolWindow/Time-zone-icon.png")));
  }

  public JPanel getContent() {
    return myToolWindowContent;
  }

  @Override
  public void valueChanged(TreeSelectionEvent e) {

  }
}
