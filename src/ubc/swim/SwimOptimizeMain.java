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
		String charID = "paddle"; //default to paddle, unless specified in args
		if (args.length > 0)
			charID = args[0];

		String optDesc = charID;
		
		SwimmerOptimization opt = new SwimmerOptimization();
		SwimFitnessFunctionA fitFun = new SwimFitnessFunctionA(charID);
		
		//Set experiment-specific values
		switch (charID) {
			case "paddle":
				optDesc = "paddle character";
				opt.setMinStoppingCost(0.0000000001);
				opt.setMaxIters(200);
				opt.setIterationsPerOutput(50);
				break;
			case "tadpole":
				optDesc = "tadpole character";
				opt.setMinStoppingCost(0.0000000001);
				opt.setMaxIters(100);
				opt.setIterationsPerOutput(10);
				fitFun.setGoalDisplacement(5.0f); //meters
				fitFun.setMaxRuntime(10); //simulate for a relatively long time to avoid instabilities after fitness test ends
				fitFun.setDisplacementErrorTermWeight(1.0f);
				fitFun.setSpeedTermWeight(0.0f); //ignore speed for this one; use displacment
				fitFun.setEnergyTermWeight(1.0f);
				fitFun.setRootAngleTermWeight(1.0f);
				break;
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
			case "humanCrawlRefTraj":
				optDesc = "crawl stroke with human character (reference trajectory version)";
				opt.setMinStoppingCost(1);
				opt.setMaxIters(50);
				opt.setIterationsPerOutput(10);
				fitFun.setSpeedTermWeight(10.0f);
				fitFun.setEnergyTermWeight(1.0f);
				fitFun.setRootAngleTermWeight(1000.0f);
				break;
			case "humanFlyRefTraj":
				optDesc = "fly stroke with human character (reference trajectory version)";
				opt.setMinStoppingCost(1);
				opt.setMaxIters(50);
				opt.setIterationsPerOutput(10);
				fitFun.setSpeedTermWeight(10.0f);
				fitFun.setEnergyTermWeight(1.0f);
				fitFun.setRootAngleTermWeight(1000.0f);
				break;
		}
		
		final String HASHES = "###################";
		
		log.info(HASHES + "Running optimization for " + optDesc + "..." + HASHES);
		
		double[] control = opt.optimize(fitFun);
		SwimmerOptimization.writeToCSV(control, "./controlData", charID);
		
		log.info(HASHES + "OPTIMIZATION COMPLETE FOR " + charID + HASHES);
	}

}
