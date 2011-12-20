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
	
	private static final String HASHES = "###################";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int numCharsToOptimize = args.length / 2;
		
		//Default just to optimizing paddle char
		if (numCharsToOptimize == 0) {
			numCharsToOptimize = 1;
			if (args.length == 0)
				args = new String[]{"paddle", ""};
			else
				args = new String[]{args[0], ""};
		}
			
		//Run optimization based on given arg pair
		//First arg in pair is char ID; second is char ID suffix
		
		String[] charIDs = new String[numCharsToOptimize];
		String[] suffixes = new String[numCharsToOptimize];
		
		for (int i = 0; i < numCharsToOptimize; i++) {
			int argsIdx = 2 * i;
			charIDs[i] = args[argsIdx];
			if (argsIdx + 1 < args.length)
				suffixes[i] = args[argsIdx+1];
			else
				suffixes[i] = "";
		}
		
		String[] fitnessDetails = new String[numCharsToOptimize];
		
		//Run optimization for each character in the list
		for (int i = 0; i < numCharsToOptimize; i++) {
			String charID = charIDs[i];
			String suffix = suffixes[i];
			
			String finalCharID = charID;
			if (suffix.length() > 0) finalCharID += "_" + suffix;
			
			SwimmerOptimization opt = new SwimmerOptimization();
			SwimFitnessFunctionA fitFun = new SwimFitnessFunctionA(charID);
			
			//Set experiment-specific values
			if  (charID.equals("paddle")) {
				float goalSpeed1 = 0.5f;
				float goalSpeed2 = 2.0f;
				float goalSpeed3 = 5.0f;
				
				//Paddle experiments: use suffices to specify different experiments. Kinda hacky, but meh...
				//SPEED EXPERIMENT: Optimize for slow, medium, and fast speeds without caring about anything else
				if (suffix.equals("speed1") || suffix.equals("speed2") || suffix.equals("speed3")) {
					fitFun.setSpeedTermWeight(1.0f);
					fitFun.setDisplacementErrorTermWeight(0);
					fitFun.setEnergyTermWeight(0);
					fitFun.setRootAngleTermWeight(0);
					if (suffix.equals("speed1")) 		fitFun.setGoalSpeed(goalSpeed1);
					else if (suffix.equals("speed2")) 	fitFun.setGoalSpeed(goalSpeed2);
					else if (suffix.equals("speed3")) 	fitFun.setGoalSpeed(goalSpeed3);
				}
				//ENERGY EXPERIMENT: Optimize for slow, medium, and fast speeds while minimizing energy
				else if (suffix.equals("energy1") || suffix.equals("energy2") || suffix.equals("energy3")) {
					fitFun.setSpeedTermWeight(1.0f);
					fitFun.setDisplacementErrorTermWeight(0);
					fitFun.setEnergyTermWeight(0.1f);
					fitFun.setRootAngleTermWeight(0);
					if (suffix.equals("energy1")) 		fitFun.setGoalSpeed(goalSpeed1);
					else if (suffix.equals("energy2")) 	fitFun.setGoalSpeed(goalSpeed2);
					else if (suffix.equals("energy3")) 	fitFun.setGoalSpeed(goalSpeed3);
				}
				//ROOT ORIENTATION EXPERIMENT: Optimize for slow, medium, and fast speeds while keeping deck level
				else if (suffix.equals("orientation1") || suffix.equals("orientation2") || suffix.equals("orientation3")) {
					fitFun.setSpeedTermWeight(1.0f);
					fitFun.setDisplacementErrorTermWeight(0);
					fitFun.setEnergyTermWeight(0.0f);
					fitFun.setRootAngleTermWeight(10);
					if (suffix.equals("orientation1")) 			fitFun.setGoalSpeed(goalSpeed1);
					else if (suffix.equals("orientation2")) 	fitFun.setGoalSpeed(goalSpeed2);
					else if (suffix.equals("orientation3")) 	fitFun.setGoalSpeed(goalSpeed3);
				}
				fitFun.setMaxRuntime(10);
				opt.setMinStoppingCost(0.0000000001);
				opt.setMaxIters(200);
				opt.setIterationsPerOutput(50);
			}
			else if (charID.equals("tadpole")) {
				opt.setMinStoppingCost(0.0000000001);
				
				//OPTIMIZATION TIME EXPERIMENT: Show controller after a few, a moderate number, and many iterations
				if (suffix.equals("lowIters")) 			opt.setMaxIters(1);
				else if (suffix.equals("medIters")) 	opt.setMaxIters(5);
				else if (suffix.equals("highIters")) 	opt.setMaxIters(50);
				else opt.setMaxIters(300);
				
				opt.setIterationsPerOutput(10);
				fitFun.setGoalDisplacement(5.0f); //meters
				fitFun.setMaxRuntime(10); //simulate for a relatively long time to avoid instabilities after fitness test ends
				fitFun.setDisplacementErrorTermWeight(1.0f);
				fitFun.setSpeedTermWeight(0.0f); //ignore speed for this one; use displacment
				fitFun.setEnergyTermWeight(0.0f);
				fitFun.setRootAngleTermWeight(0.0f);
			}
			else if (charID.equals("humanCrawl")) {
				opt.setMinStoppingCost(1);
				opt.setMaxIters(50);
				opt.setIterationsPerOutput(10);
				break;
			}
			else if (charID.equals("humanFly")) {
				opt.setMinStoppingCost(1);
				opt.setMaxIters(300);
				opt.setIterationsPerOutput(10);
			}
			else if (charID.equals( "humanCrawlRefTraj")) {
				opt.setMinStoppingCost(1);
				opt.setMaxIters(50);
				opt.setIterationsPerOutput(10);
				fitFun.setSpeedTermWeight(10.0f);
				fitFun.setEnergyTermWeight(1.0f);
				fitFun.setRootAngleTermWeight(1000.0f);
			}
			else if (charID.equals("humanFlyRefTraj")) {
				opt.setMinStoppingCost(1);
				opt.setMaxIters(50);
				opt.setIterationsPerOutput(10);
				fitFun.setSpeedTermWeight(10.0f);
				fitFun.setEnergyTermWeight(1.0f);
				fitFun.setRootAngleTermWeight(1000.0f);
			}
			
			log.info(HASHES + " Running optimization for " + finalCharID + "... " + HASHES);
			
			double[] control = opt.optimize(fitFun);
			SwimmerOptimization.writeToCSV(control, "./controlData", finalCharID);
			
			log.info(HASHES + " OPTIMIZATION COMPLETE FOR " + charID + " " + HASHES);
			
			//Save some useful experimental results about the chosen controls
			fitFun.valueOf(control); //replay best control, then gather stats...
			fitnessDetails[i] = finalCharID + ":\n" + fitFun.getCharacter().getStatisticsString();
		}
		
		log.info(HASHES + " OPTIMIZATIONS: COMPLETED " + HASHES);
		
		System.out.println(HASHES + " FITNESS DETAILS: " + HASHES);
		for (int i = 0; i < numCharsToOptimize; i++) {
			System.out.println(fitnessDetails[i]);
		}
	}

}
