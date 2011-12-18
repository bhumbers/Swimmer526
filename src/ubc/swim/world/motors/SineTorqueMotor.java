package ubc.swim.world.motors;

import java.util.ArrayList;

import org.jbox2d.dynamics.Body;

/**
 * A torque motor which use a sine function to evaluate its current value
 * @author Ben Humberston
 *
 */
public class SineTorqueMotor extends TorqueMotor {

	protected float ONE_OVER_SQRT_TWO_PI;
	protected static final float MIN_PERIOD = 0.00001f;
	
	protected Body bodyA;
	protected Body bodyB;
	
	/** Sine parameters (one entry to function) */
	protected ArrayList<Float> weights;
	protected ArrayList<Float> periods;
	protected ArrayList<Float> phaseOffsets;
	
	/**
	 * Max torque that may be applied by this motor
	 */
	protected float maxTorque;
	
	/** Torque applied the last time applyTorque() was called. */
	protected float prevTorque = 0.0f;
	
	/**
	 * Creates a new motor that applies torques between given bodies using
	 * numFuncs functions to determine torque magnitude based on time.
	 * @param bodyA
	 * @param bodyB
	 * @param maxTorque
	 * @param numGaussians
	 */
	public SineTorqueMotor(Body bodyA, Body bodyB, float maxTorque, int numFuncs) {
		ONE_OVER_SQRT_TWO_PI = (float)(1 / Math.sqrt(2 * Math.PI));
		
		this.bodyA = bodyA;
		this.bodyB = bodyB;
		
		this.maxTorque = maxTorque;
		
		weights = new ArrayList<Float>();
		periods = new ArrayList<Float>();
		phaseOffsets = new ArrayList<Float>();
		
		for (int i = 0; i < numFuncs; i++) {
			weights.add(0.0f);
			periods.add(MIN_PERIOD);
			phaseOffsets.add(0.0f);
		}
	}
	
	@Override
	public float getPrevTorque() { return prevTorque; }
	
	/**
	 * Sets parameters for the funcNum'th sine function in this motor
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
	public float applyTorque(float runtime) {
		float torque = 0.0f;
		
		//Add contribution from each sine
		for (int i = 0; i < weights.size(); i++) {
			float weight = weights.get(i);
			float period = periods.get(i);
			float phaseOffset = phaseOffsets.get(i);
			
			torque += weight * (float)Math.sin((2 * Math.PI * runtime) / period + phaseOffset);
		}
		
		torque = Math.min(torque, maxTorque);
		
		//Apply equal and opposite torques to each body
		//TODO: not sure if this is quite correct yet...
		//Should the torque be applied to both, or just one body?
		//Is applying a torque at the body's CoM equivalent to applying it at the joint?
		if (torque != 0.0f) {
			bodyA.applyTorque(torque);
			bodyB.applyTorque(-torque);
		}
		
		prevTorque = torque;
		
		return torque;
	}
	
}
