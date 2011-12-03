package ubc.swim.gui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.jbox2d.testbed.framework.TestbedController;
import org.jbox2d.testbed.framework.TestbedModel;
import org.jbox2d.testbed.framework.TestbedPanel;
import org.jbox2d.testbed.framework.j2d.TestbedSidePanel;

/**
 * Primary window frame for Swim application.
 * Modeled on org.jbox2d.testbed.framework.TestbedFrame by Daniel Murphy
 * @author Ben Humberston
 *
 */
public class SwimFrame extends JFrame {
	  private TestbedSidePanel side;
	  private SwimModel model;
	  private SwimController controller;

	  public TestbedFrame(SwimModel argModel, SwimController argPanel) {
	    super("Swimmer App");
	    setLayout(new BorderLayout());

	    model = argModel;
	    model.setDebugDraw(argPanel.getDebugDraw());
	    controller = new TestbedController(model, argPanel);
	    side = new TestbedSidePanel(model, controller);
	    
	    add((Component) argPanel, "Center");
	    add(new JScrollPane(side), "East");
	    pack();

	    controller.playTest(0);
	    controller.start();
	  }
}
