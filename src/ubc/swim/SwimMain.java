package ubc.swim;

import javax.swing.JFrame;
import javax.swing.UIManager;

import org.jbox2d.testbed.framework.j2d.TestPanelJ2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		
		
	    TestbedModel model = new TestbedModel();
	    TestbedPanel panel = new TestPanelJ2D(model);
	    TestList.populateModel(model);
	    JFrame testbed = new TestbedFrame(model, panel);
	    testbed.setVisible(true);
	    testbed.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	  }
	}


}
