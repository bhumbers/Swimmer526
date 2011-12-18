package ubc.swim.world.trajectory;

import org.jbox2d.dynamics.joints.RevoluteJoint;

/**
 * Provides a time/phase dependent value that is target for some
 * degree of freedom of character
 * @author Ben Humberston
 *
 */
public abstract class RefTrajectory {
	
	protected RevoluteJoint joint; //optional joint associated with this trajectory
	
	/**
	 * Returns current reference value, given current time
	 * @param runtime
	 * @return
	 */
	public abstract float getValue(float time);
	
	/** Optionally stores a joint associated with this trajectory. */
	public void setJoint(RevoluteJoint joint) { this.joint = joint; }
	
	/** Returns stored joint associated with this trajectory */
	public RevoluteJoint getJoint() { return joint; }
}
