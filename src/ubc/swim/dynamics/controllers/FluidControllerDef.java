/**
 * 
 */
package ubc.swim.dynamics.controllers;

import org.jbox2d.common.Vec2;

/**
 * Definition for a fluid buoyancy controller
 * 
 * Adapted from http://personal.boristhebrave.com/project/b2buoyancycontroller
 * 
 * @author Ben Humberston
 *
 */
public class FluidControllerDef extends DynamicsControllerDef {
	/// The outer surface normal
	public Vec2 normal;
	/// The height of the fluid surface along the normal
	public float offset;
	/// The fluid density
	public float density;
	/// Fluid velocity, for drag calculations
	public Vec2 velocity;
	/// Linear drag co-efficient
	public float linearDrag;
	/// If false, bodies are assumed to be uniformly dense, otherwise use the shapes densities
	public boolean useDensity; //False by default to prevent a gotcha
	/// If true, gravity is taken from the world instead of the gravity parameter.
	public boolean useWorldGravity;
	/// Gravity vector, if the world's gravity is not used
	public Vec2 gravity;

	public FluidControllerDef() {
		normal = new Vec2(0,1);
		offset = 0;
		density = 0;
		velocity = new Vec2(0,0);
		linearDrag = 0;
		useDensity = false;
		useWorldGravity = true;
		gravity = new Vec2(0,0);
	}

	/**
	 * @see org.jbox2d.dynamics.DynamicsControllerDef.ControllerDef#create()
	 */
	@Override
	public DynamicsController create() {
		return new FluidController(this);
	}
}
