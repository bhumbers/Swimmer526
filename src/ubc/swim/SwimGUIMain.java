package ubc.swim;

import javax.swing.JFrame;
import javax.swing.UIManager;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ubc.swim.gui.SwimFrame;
import ubc.swim.gui.SwimModel;
import ubc.swim.gui.SwimTestList;
import ubc.swim.gui.SwimWorldPanel;

/**
 * Entry point for GUI to view swimmers in real time
 * Use this to view results of optimization.
 * @author Ben Humberston
 *
 */
public class SwimGUIMain {
	private static final Logger log = LoggerFactory.getLogger(SwimGUIMain.class);
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
	    } 
		catch (Exception e) {
	      log.warn("Could not set the look and feel to nimbus.");
	    }
		
		
		SwimModel model = new SwimModel();
		SwimWorldPanel panel = new SwimWorldPanel(model);
	    SwimTestList.populateModel(model);
	    JFrame testbed = new SwimFrame(model, panel);
	    testbed.setVisible(true);
	    testbed.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
