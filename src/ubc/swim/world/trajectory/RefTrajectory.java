package ubc.swim.world.trajectory;

/**
 * Provides a time/phase dependent value that is target for some
 * degree of freedom of character
 * @author Ben Humberston
 *
 */
public abstract class RefTrajectory {
	
	/**
	 * Returns current reference value, given current time and size of timestep
	 * @param runtime
	 * @param dt
	 * @return
	 */
	public abstract float getValue(float time, float dt);
}
