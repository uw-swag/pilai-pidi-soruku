// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package toolWindow;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.WindowManager;
import com.noble.Main;
import com.noble.models.Encl_name_pos_tuple;
import com.noble.util.XmlUtil;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultEdge;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
    String directory = null;
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    Project activeProject = null;
    for (Project project : projects) {
      Window window = WindowManager.getInstance().suggestParentWindow(project);
      if (window != null && window.isActive()) {
        activeProject = project;
      }
    }
    assert activeProject != null;
    directory = activeProject.getBasePath();
    arguments[0] = directory;

    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Possible Buffer Overflow Violations");
    DefaultTreeModel treeModel = new DefaultTreeModel(root);

    XmlUtil.MyResult result = Main.main(arguments);
    int violations_count = 0;
    for(Encl_name_pos_tuple source_node: result.getSource_nodes()){
      Enumeration<Encl_name_pos_tuple> violationE = result.getDetected_violations().keys();
      while (violationE.hasMoreElements()) {
        Encl_name_pos_tuple violated_node_pos_pair = violationE.nextElement();
        ArrayList<String> violations = result.getDetected_violations().get(violated_node_pos_pair);
        AllDirectedPaths<Encl_name_pos_tuple, DefaultEdge> allDirectedPaths = new AllDirectedPaths<>(result.getDg());
        List<GraphPath<Encl_name_pos_tuple,DefaultEdge>> requiredPath = allDirectedPaths.getAllPaths(source_node, violated_node_pos_pair, true, null);
        if(!requiredPath.isEmpty()){

          requiredPath.get(0).getVertexList().forEach(x->System.out.print(x + " -> "));
          System.out.println();
          violations.forEach(violation-> root.add(new DefaultMutableTreeNode(violation)));

          violations_count = violations_count + violations.size();
        }
      }
    }
    System.out.println("No of files analyzed "+ result.getJava_slice_profiles_info().size());
    System.out.println("Detected violations "+ violations_count);



    DefaultMutableTreeNode lv1 = new DefaultMutableTreeNode("lv1");
    DefaultMutableTreeNode lv12 = new DefaultMutableTreeNode("lv12");
    DefaultMutableTreeNode lv2 = new DefaultMutableTreeNode("lv2");

    root.add(lv1);
    lv1.add(lv12);
    root.add(lv2);

    lv2.add( new DefaultMutableTreeNode("lv21"));
    lv2.add( new DefaultMutableTreeNode("lv22"));
    lv2.add( new DefaultMutableTreeNode("lv23"));

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
