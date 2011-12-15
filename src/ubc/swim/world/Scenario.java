package ubc.swim.world;

import java.util.ArrayList;

/** A world scenario for the swimmer app
 * 
 * @author Ben Humberston
 *
 */
public class Scenario {
	/**
	 * List of characters in scenario
	 */
	protected ArrayList<Character> chars;
	
	/**
	 * Fluid density
	 */
	protected float fluidDensity;
	
	/** 
	 * Constructor
	 */
	public Scenario() {
		chars = new ArrayList<Character>();
	}
}
