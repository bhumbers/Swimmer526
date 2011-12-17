package ubc.swim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ubc.swim.optimization.SwimFitnessFunctionA;
import ubc.swim.optimization.SwimmerOptimization;

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
			case "humanCrawl":
				//TODO
//				log.info("Running optimization for crawl stroke with human character...");
				log.info("TODO: Human crawl optimization is not yet implemented");
				break;
			case "paddle":
				log.info("Running optimization for paddle character...");
				
				SwimmerOptimization opt = new SwimmerOptimization();
				
				SwimFitnessFunctionA fitFun = new SwimFitnessFunctionA("paddle");
				
				double[] control = opt.optimize(fitFun);
				
				SwimmerOptimization.writeToCSV(control, "./controlData", "paddle");
				
				
				break;
		}
		
		log.info("OPTIMIZATION COMPLETE");
	}

}
