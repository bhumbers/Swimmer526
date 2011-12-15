package ubc.swim.gui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

/**
 * Primary window frame for Swim application. Modeled on
 * org.jbox2d.testbed.framework.TestbedFrame by Daniel Murphy
 * 
 * @author Ben Humberston
 * 
 */
public class SwimFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	
	private SwimSidePanel side;
	private SwimModel model;
	private SwimController controller;

	public SwimFrame(SwimModel argModel, SwimWorldPanel argPanel) {
		super("Swimmer App");
		setLayout(new BorderLayout());

		model = argModel;
		model.setDebugDraw(argPanel.getDebugDraw());
		controller = new SwimController(model, argPanel);
		side = new SwimSidePanel(model, controller);

		add((Component) argPanel, "Center");
		add(new JScrollPane(side), "East");
		pack();

		controller.playTest(0);
		controller.start();
	}
}
