/**
 * 
 */
package ubc.swim.dynamics.controllers;

import java.util.ArrayList;
import java.util.List;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;

import ubc.swim.gui.SwimSettings;

/**
 * Base class for controllers. Controllers are a convenience for encapsulating
 * common per-step functionality.
 * 
 * Adapted from http://personal.boristhebrave.com/project/b2buoyancycontroller
 * 
 * @author Ben Humberston
 */
public class DynamicsController {
	public World world;
	public ArrayList<Body> bodies;

	protected DynamicsController(DynamicsControllerDef def) {
		world = null;
		bodies = new ArrayList<Body>();
	}
	
	/** Controllers override this to implement per-step functionality. */
	public void step(SwimSettings settings) {;}

	/** Controllers override this to provide debug drawing. */
	public void draw(DebugDraw debugDraw, SwimSettings settings) {;}

	/** Adds a body to the controller list. */
	public void addBody(Body body) {
		bodies.add(body);
	}

	/** Removes a body from the controller list. */
	public void removeBody(Body body) {
		bodies.remove(body);
	}

	/** Removes all bodies from the controller list. */
	public void clear() {
		bodies.clear();
	}

	/** Get the parent world of this body. */
	public World getWorld() {
		return world;
	}

	/** Get the attached body list */
	public List<Body> getBodyList() {
		return bodies;
	}
}
