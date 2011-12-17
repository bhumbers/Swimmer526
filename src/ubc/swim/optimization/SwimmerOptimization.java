package ubc.swim.optimization;
import com.sun.xml.internal.ws.policy.spi.PolicyAssertionValidator.Fitness;

import fr.inria.optimization.cmaes.CMAEvolutionStrategy;
import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;

/**
 * 
 * Runs CMA evolution to find optimal paramters for a given virtual swimmer.
 * 
 * Adapted from fr.inria.optimization.cmaes.CMAExample1
 * 
 * @author Ben Humberston 
 */
public class SwimmerOptimization {
	
	/**
	 * Executes CMA control optimization using given function, returning 
	 * best control strategy found
	 * @param fitFun
	 */
	public double[] optimize(SwimFitnessFunction fitFun) {
		CMAEvolutionStrategy cma = new CMAEvolutionStrategy();
		
		//read options, see file CMAEvolutionStrategy.properties
		cma.readProperties(); 
		
		// Set custom properties
		cma.setDimension(fitFun.getCharacter().getNumControlDimensions()); 
		cma.setInitialX(0.05); // in each dimension, also setTypicalX can be used
		cma.setInitialStandardDeviation(0.2); // also a mandatory setting 
		cma.options.stopFitness = 1e-14;       // optional setting

		// Initialize CMA and get fitness array
		double[] fitness = cma.init();

		// Write output file initial header
		cma.writeToDefaultFilesHeaders(0); // 0 == overwrites old files

		// Run optimization iterations until a stop condition is met
		while(cma.stopConditions.getNumber() == 0) {
			//Get a list of sampled control strategies
			double[][] controlPop = cma.samplePopulation(); 
			
			//Check fitness of each strategy
			for (int i = 0; i < controlPop.length; ++i) {    
				//If strategy is disallowed, resample (watch for infinite loops here...)
				while (!fitFun.isFeasible(controlPop[i]))  
					controlPop[i] = cma.resampleSingle(i);    
                                                     
                // compute fitness/objective value (to be minimized)
				fitness[i] = fitFun.valueOf(controlPop[i]); 
			}
			
			// Pass fitness array to update search distribution
			cma.updateDistribution(fitness);         

			// Update output to console/files
			cma.writeToDefaultFiles();
			int outmod = 150;
			//Print table headers and stats every so often
			if (cma.getCountIter() % (15*outmod) == 1)
				cma.printlnAnnotation(); 
			if (cma.getCountIter() % outmod == 1)
				cma.println(); 
		}
		
		// Set best-ever solution to mean control strategy (which is best guess for optimal strategy)
		cma.setFitnessOfMeanX(fitFun.valueOf(cma.getMeanX())); 

		// Write final output
		cma.writeToDefaultFiles(1);
		cma.println();
		cma.println("Terminated due to");
		for (String s : cma.stopConditions.getMessages())
			cma.println("  " + s);
		cma.println("best function value " + cma.getBestFunctionValue() 
				+ " at evaluation " + cma.getBestEvaluationNumber());
		
		return cma.getBestX();
	}
}
