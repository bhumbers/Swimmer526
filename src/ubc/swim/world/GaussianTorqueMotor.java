package ubc.swim.world;

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
	
	/** Period gives max time value for which function is evaluated
	 * If given time input is beyond the period, the input value is 
	 * evaluated modulo the period.
	 */
	protected float period;
	
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
			means.add(0.0f);
			stdDevs.add(0.0f);
		}
	}
	
	/**
	 * Updates period used by this motor
	 * @param val
	 */
	public void setPeriod(float val) {
		this.period = val;
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
			torque += maxTorque * w * (1/stdDev) * ONE_OVER_SQRT_TWO_PI * Math.exp(-(distFromMean * distFromMean) / (2 * stdDev * stdDev));
		}
		
		torque = Math.min(torque, maxTorque);
		
		//Apply equal and opposite torques to each body
		//TODO: not sure if this is quite correct yet...
		//Should the torque be applied to both, or just one body?
		//Is applying a torque at the body's CoM equivalent to applying it at the joint?
		bodyA.applyTorque(torque);
		bodyB.applyTorque(-torque);
		
		//TODO: Remove if applyTorque does the job...
//		float targetAngVel = -2 * (float)Math.PI;
//		float errorAngVel = (upperArm.m_angularVelocity - torso.m_angularVelocity) - targetAngVel;
//		float impulse = 8 * shoulderJoint.m_motorMass * (-errorAngVel);
//		
//		float maxImpulse = dt * 30;
//		impulse = MathUtils.clamp(impulse, -maxImpulse, maxImpulse);
//		
//		//TODO: apply a torque, not a direct impulse
//		torsoImpulse -= torso.m_invI * impulse;
//		upperArm.m_angularVelocity += upperArm.m_invI * impulse;
//		
//		//Modify torso ang vel afterward... doing it during the loop causes order-of-application of forces to blow things up
//		if (torso != null)
//			torso.m_angularVelocity += torsoImpulse;
		
		return torque;
	}
	
}
