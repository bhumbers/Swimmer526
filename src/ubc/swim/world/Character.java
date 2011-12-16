package ubc.swim.world;

import java.util.ArrayList;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;

import ubc.swim.gui.SwimSettings;

/**
 * A multisegment character in the swimmer world.
 * 
 * @author Ben Humberston
 *
 */
public class Character {
	/** Body marked as root of this character */
	protected Body rootBody = null;
	
	/** 
	 * Body parts that define this character
	 */
	protected ArrayList<Body> bodies;
	
	/**
	 * Control parameters used by this character
	 */
	protected double[] controlParams;
	
	/**
	 * Constructor
	 */
	public Character() {
		bodies = new ArrayList<Body>();
	}
	
	/**
	 * Creates segments & joints for this char in given world.
	 * @param world
	 */
	public void initialize(World world) {
		
	}
	
	/** Updates this character to use control strategy derived from 
	 * given control parameter array */
	public void applyControlParams(double[] params) {
		
	}
	
	/**
	 * Resets this character to original configuration.
	 */
	public void reset() {
		//TODO: override in subclasses
	}
	
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
}
