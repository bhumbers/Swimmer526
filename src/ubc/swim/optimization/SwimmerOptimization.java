package ubc.swim.optimization;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import fr.inria.optimization.cmaes.CMAEvolutionStrategy;

/**
 * 
 * Runs CMA evolution to find optimal paramters for a given virtual swimmer.
 * 
 * Adapted from fr.inria.optimization.cmaes.CMAExample1
 * 
 * @author Ben Humberston 
 */
public class SwimmerOptimization {
	protected int maxIters = 100;
	protected double minStoppingCost = 1e-14;
	protected int iterationsPerOutput = 150;
	
	/** Sets maximum number of CMA iterations used by this optimizer. */
	public void setMaxIters(int val) { this.maxIters = val;}
	
	/** Sets minimum cost value that is reached before optimization stops (ie: the goal minimized cost is found). */
	public void setMinStoppingCost(double val) { this.minStoppingCost = val;}
	
	/** Sets how many CMA iterations are run between each logging output of optimizer progress */
	public void setIterationsPerOutput(int val) { this.iterationsPerOutput = val;}
	
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
		cma.setDimension(fitFun.getNumControlDimensions()); 
		cma.setInitialX(0.05); // in each dimension, also setTypicalX can be used
		cma.setInitialStandardDeviation(0.2); // also a mandatory setting 
		cma.options.stopFitness = minStoppingCost;       // optional setting
		cma.options.stopMaxIter = maxIters;
		cma.options.diagonalCovarianceMatrix = 1; //keep diagonal covariance

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
			//Print table headers and stats every so often
			if (cma.getCountIter() % (15*iterationsPerOutput) == 1)
				cma.printlnAnnotation(); 
			if (cma.getCountIter() % iterationsPerOutput == 1)
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
	
	/**
	 * Writes given control values to a comma-separated value file with given path and name
	 * (.csv extension will be automatically appended)
	 * @param x
	 * @param filePath location folder of file (no trailing "/")
	 * @param fileName name of file (no .csv extension)
	 */
	public static void writeToCSV(double[] x, String filePath, String fileName) {
		try
		{
		    FileWriter writer = new FileWriter(filePath + "/" + fileName + ".csv");
		    
		    for (double val : x) {
		    	writer.append(Double.toString(val));
		    	writer.append(",\n");
		    }
	 
		    writer.flush();
		    writer.close();
		}
		catch(IOException e)
		{
		     e.printStackTrace();
		} 
	}
	
	/**
	 * Reads control values from a comma-separated value file with given path
	 * and name and returns them in a double array (.csv extension will be
	 * automatically appended)
	 * 
	 * @param filePath
	 *            location folder of file (no trailing "/")
	 * @param fileName
	 *            name of file (no .csv extension)
	 */
	public static double[] readFromCSV(String filePath, String fileName) {
		List<Double> loadedVals = new ArrayList<Double>();
		
		try
		{
			BufferedReader reader  = new BufferedReader(new FileReader(filePath + "/" + fileName + ".csv"));
			String line = null;
			
			//Each line contains 1 control param (mainly for human readability)
			while((line = reader.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line,",");
				loadedVals.add(Double.parseDouble(st.nextToken()));
			}
			
			reader.close();
		}
		catch(IOException e)
		{
		     e.printStackTrace();
		}
		
		double[] x = new double[loadedVals.size()];
		int numVals = loadedVals.size();
		for (int i = 0; i < numVals; i++) {
			double val = loadedVals.get(i);
			x[i] = val;
		}
		return x;
	}
}
