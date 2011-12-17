package ubc.swim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for swimmer optimization
 * @author Ben Humberston
 *
 */
public class SwimOptimizeMain {
	private static final Logger log = LoggerFactory.getLogger(SwimOptimizeMain.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//Run optimization based on given args
		String charName = args[0];
		
		switch (charName) {
			case "human":
				log.info("Running optimization for human character...");
				
				//TODO: run optimization HERE
				
				
				break;
		}
		
		log.info("OPTIMIZATION COMPLETE");
	}

}
