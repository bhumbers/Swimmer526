package ubc.swim.world.characters;

import java.util.ArrayList;
import java.util.List;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;

import ubc.swim.gui.SwimSettings;
import ubc.swim.world.motors.TorqueMotor;

/**
 * A multisegment character in the swimmer world.
 * 
 * @author Ben Humberston
 *
 */
public abstract class SwimCharacter {
	/** Body marked as root of this character */
	protected Body rootBody = null;
	
	/** 
	 * Body parts that define this character
	 */
	protected ArrayList<Body> bodies;
	
	/**
	 * Motors used to control this character
	 */
	protected ArrayList<TorqueMotor> motors;
	
	/**
	 * Control parameters used by this character
	 */
	protected double[] controlParams;
	
	/**
	 * Constructor
	 */
	public SwimCharacter() {
		bodies = new ArrayList<Body>();
		motors = new ArrayList<TorqueMotor>();
	}
	
	public List<Body> getBodies() {return bodies;}
	public List<TorqueMotor> getMotors() {return motors;}
	public Body getRootBody() {return rootBody;}
	
	/**
	 * Creates segments & joints for this char in given world.
	 * @param world
	 */
	public void initialize(World world) {
		
	}
	
	/** Updates this character to use control strategy derived from 
	 * given control parameter array 
	 * Array size must be same as character's control dimensionality
	 */
	public abstract void setControlParams(double[] params);
	
	/**
	 * Moves whole character to given world position
	 * @param x
	 * @param y
	 */
	public void moveTo(float x, float y) {
		Vec2 translation = new Vec2(x, y);
		translation.subLocal(rootBody.getPosition());
		
		for (Body body : bodies)
			body.setTransform(body.getPosition().add(translation), body.getAngle());
	}
	
	/**
	 * Update controls on this character for given time step
	 * @param settings
	 * @param dt size of simulation step
	 * @param runtime Total world simulation time so far
	 */
	public void step(SwimSettings settings, float dt, float runtime) {
		//TODO: override in subclasses
	}
	
	
	/**
	 * Returns total torque applied on previous step by this character
	 * @return
	 */
	public abstract float getPrevTorque();
	
	/**
	 * Returns dimensionality of this character's control strategy
	 * @return
	 */
	public abstract int getNumControlDimensions();
	
	/**
	 * Renders debug visuals about this character to the screen.
	 * @param debugDraw
	 */
	public abstract void debugDraw(DebugDraw debugDraw);
}
