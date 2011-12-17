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
		String charID = args[0];

		String optDesc = charID;
		
		SwimmerOptimization opt = new SwimmerOptimization();
		
		//Set experiment-specific values
		switch (charID) {
			case "humanCrawl":
				optDesc = "crawl stroke with human character";
				opt.setMinStoppingCost(1);
				opt.setMaxIters(50);
				opt.setIterationsPerOutput(10);
				break;
			case "humanFly":
				optDesc = "fly stroke with human character";
				opt.setMinStoppingCost(1);
				opt.setMaxIters(150);
				opt.setIterationsPerOutput(10);
				break;
			case "paddle":
				optDesc = "paddle character";
				opt.setMinStoppingCost(0.0000000001);
				opt.setMaxIters(500);
				opt.setIterationsPerOutput(50);
				break;
		}
		
		final String HASHES = "###################";
		
		log.info(HASHES + "Running optimization for " + optDesc + "..." + HASHES);
		
		//Set cost function params
		SwimFitnessFunctionA fitFun = new SwimFitnessFunctionA(charID);
		
		double[] control = opt.optimize(fitFun);
		SwimmerOptimization.writeToCSV(control, "./controlData", charID);
		
		log.info(HASHES + "OPTIMIZATION COMPLETE FOR " + charID + HASHES);
	}

}
