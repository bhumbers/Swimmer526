package ubc.swim.world;

import java.util.ArrayList;

/**
 * A multisegment character in the swimmer world.
 * 
 * @author Ben Humberston
 *
 */
public class Character {
	/** 
	 * Segments that define this character
	 */
	protected ArrayList<Segment> segments;
	
	/**
	 * Inter-segment muscles
	 */
	protected ArrayList<Muscle> muscles;
	
	/**
	 * Constructor
	 */
	public Character() {
		segments = new ArrayList<Segment>();
		muscles = new ArrayList<Muscle>();
	}
}
