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
 * Main entry point for program
 * @author Ben Humberston
 *
 */
public class SwimMain {
	private static final Logger log = LoggerFactory.getLogger(SwimMain.class);
	
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
