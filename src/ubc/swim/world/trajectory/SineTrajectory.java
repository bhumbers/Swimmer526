package ubc.swim.world.trajectory;

import java.util.ArrayList;

/**
 * A reference trajectory that uses sinusoidal basis functions
 * @author Ben Humberston
 *
 */
public class SineTrajectory extends RefTrajectory {
	protected static final float MIN_PERIOD = 0.00001f;
	
	/** Sine parameters (one entry to function) */
	protected ArrayList<Float> weights;
	protected ArrayList<Float> periods;
	protected ArrayList<Float> phaseOffsets;
	
	/**
	 * Initializes with given number of basis funcs
	 * @param numFuncs
	 */
	public SineTrajectory(int numFuncs) {
		weights = new ArrayList<Float>();
		periods = new ArrayList<Float>();
		phaseOffsets = new ArrayList<Float>();
		
		for (int i = 0; i < numFuncs; i++) {
			weights.add(0.0f);
			periods.add(MIN_PERIOD);
			phaseOffsets.add(0.0f);
		}
	}
	
	/**
	 * Sets parameters for the funcNum'th sine function in this trajectory
	 * @param funcNum
	 * @param weight
	 * @param mean
	 * @param stdDev
	 */
	public void setSineParams(int funcNum, float weight, float period, float phaseOffset)
	{
		weights.set(funcNum, weight);
		
		if (period < MIN_PERIOD) 
			period = MIN_PERIOD;
		periods.set(funcNum, period);
		
		phaseOffsets.set(funcNum, phaseOffset);
	}
	
	@Override
	public float getValue(float time) {
		float val = 0.0f;
		//Add contribution from each sine
		for (int i = 0; i < weights.size(); i++) {
			float weight = weights.get(i);
			float period = periods.get(i);
			float phaseOffset = phaseOffsets.get(i);
			
			val += weight * (float)Math.sin((2 * Math.PI * time) / period + phaseOffset);
		}
		
		return val;
	}
}
