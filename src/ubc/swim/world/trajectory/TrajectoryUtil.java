package ubc.swim.world.trajectory;

/** Static util functions for PD control trajectories
 * @author Ben Humberston
 **/
public class TrajectoryUtil {
	protected static final float TWO_PI = (float)(2 * Math.PI);
	
	/** Returns shortest distance in radians between given angles. Value will be
	 * in the range [-PI, PI] */
	public static float distanceBetweenAngles(float a, float b)
	{
	    float delta = (b - a) % TWO_PI;
	    if (delta > Math.PI) 
	    	delta = -TWO_PI + delta;
	    if (delta < -Math.PI) 
	    	delta = TWO_PI + delta;
	    return delta;
	}
}
