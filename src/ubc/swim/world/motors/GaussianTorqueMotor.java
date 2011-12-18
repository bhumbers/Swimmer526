package ubc.swim.world.motors;

import java.util.ArrayList;

import org.jbox2d.dynamics.Body;

/**
 * A torque motor which uses a mixture of Gaussian functions 
 * to evaluate its current value
 * @author Ben Humberston
 *
 */
public class GaussianTorqueMotor extends TorqueMotor {

	protected float ONE_OVER_SQRT_TWO_PI;
	protected static final float MIN_PERIOD = 0.00001f;
	protected static final float MIN_STD_DEV = 0.00001f;
	
	protected Body bodyA;
	protected Body bodyB;
	
	/** Gaussian parameters (one entry to Gaussian function) */
	protected ArrayList<Float> weights;
	protected ArrayList<Float> means;
	protected ArrayList<Float> stdDevs;
	
	/**
	 * Max torque that may be applied by this motor
	 */
	protected float maxTorque;
	
	/** Torque applied the last time applyTorque() was called. */
	protected float prevTorque = 0.0f;
	
	/** Period gives max time value for which function is evaluated
	 * If given time input is beyond the period, the input value is 
	 * evaluated modulo the period.
	 */
	protected float period = MIN_PERIOD;
	
	/**
	 * Creates a new motor that applies torques between given bodies using
	 * numGaussian functions to determine torque magnitude based on time.
	 * @param bodyA
	 * @param bodyB
	 * @param maxTorque
	 * @param numGaussians
	 */
	public GaussianTorqueMotor(Body bodyA, Body bodyB, float maxTorque, int numGaussians) {
		ONE_OVER_SQRT_TWO_PI = (float)(1 / Math.sqrt(2 * Math.PI));
		
		this.bodyA = bodyA;
		this.bodyB = bodyB;
		
		this.maxTorque = maxTorque;
		
		weights = new ArrayList<Float>();
		means = new ArrayList<Float>();
		stdDevs = new ArrayList<Float>();
		
		for (int i = 0; i < numGaussians; i++) {
			weights.add(0.0f);
			means.add(0.0f);
			stdDevs.add(MIN_STD_DEV);
		}
	}
	
	@Override
	public float getPrevTorque() { return prevTorque; }
	
	/**
	 * Updates period used by this motor.
	 * If given value is less than MIN_PERIOD, sets period to MIN_PERIOD
	 * Returns final period value
	 * @param val
	 */
	public float setPeriod(float val) {
		this.period = Math.abs(val); //only positive period allowed
		//Never allow tiny periods
		if (this.period < MIN_PERIOD) 
			this.period = MIN_PERIOD;
		return this.period;
	}
	
	/**
	 * Sets parameters for the funcNum'th Gaussian in this motor
	 * @param funcNum
	 * @param weight
	 * @param mean
	 * @param stdDev
	 */
	public void setGaussianParams(int funcNum, float weight, float mean, float stdDev)
	{
		weights.set(funcNum, weight);
		means.set(funcNum, mean);
		
		if (stdDev < MIN_STD_DEV) 
			stdDev = MIN_STD_DEV;
		stdDevs.set(funcNum, stdDev);
	}
	
	@Override
	public float applyTorque(float runtime) {
		float time = runtime;
		if (time > period) 
			time = time % period;
		
		float torque = 0.0f;
		
		//Add contribution from each Gaussian
		for (int i = 0; i < weights.size(); i++) {
			float w = weights.get(i);
			float mean = means.get(i);
			float stdDev = stdDevs.get(i);
			
			float distFromMean = time - mean;
			//The torque contribution is max torque weighted by Gaussian value (maps from [0,1] control range into larger torques)
			float expTerm = -(distFromMean * distFromMean) / (2 * stdDev * stdDev);
			float amplitudeTerm = maxTorque * w * (1/stdDev) * ONE_OVER_SQRT_TWO_PI;
			torque += amplitudeTerm * Math.exp(expTerm);
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
