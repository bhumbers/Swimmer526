package ubc.swim.world.characters;

import java.util.ArrayList;
import java.util.List;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.pooling.arrays.Vec2Array;

import ubc.swim.gui.SwimSettings;
import ubc.swim.world.motors.TorqueMotor;

/**
 * A multisegment character in the swimmer world.
 * 
 * @author Ben Humberston
 *
 */
public abstract class SwimCharacter {
	public static final float ROOT_BODY_ANGLE_DEVIATION_THRESHOLD = (float)Math.PI * 0.1f;
	
	protected static final float TWO_PI = (float)(2 * Math.PI);
	
	/** Body marked as root of this character */
	protected Body rootBody = null;
	
	protected float rootAngleOrig = 0.0f;
	protected boolean rootAngleOrigSet = false;
	
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
	 * An identifier suffix for this character
	 */
	protected String suffix;
	
	
	/** Sum of absolute torque magnitudes applied thus far by this character */
	protected float totalTorque;
	/** Sum of total root body rotation outside allowed threshold for this character */
	protected float totalRootOrientationDeviation;
	/** Sum of linear horizontal root body velocities multiplied by dt at each simulation step */
	protected float totalRootBodySpeedTimesDt;
	/** Total simulation time run by this character */
	protected float runtime = 0.0f;
	
	/** 
	 * Color used to draw this character in GUI
	 */
	protected Color3f debugColor;
	
	private final Vec2Array tlvertices = new Vec2Array(); //used for debug drawing
	
	/**
	 * Constructor
	 */
	public SwimCharacter() {
		bodies = new ArrayList<Body>();
		motors = new ArrayList<TorqueMotor>();
		
		debugColor = new Color3f(1.0f, 0, 0); //default to red
	}
	
	public List<Body> getBodies() {return bodies;}
	public List<TorqueMotor> getMotors() {return motors;}
	public Body getRootBody() {return rootBody;}
	
	public float getTotalTorque() {return totalTorque;}
	public float getTotalRootOrientationDeviation() {return totalRootOrientationDeviation;}
	public float getAvgRootBodySpeed() {
		float avgSpeed = totalRootBodySpeedTimesDt;
		if (runtime > 0) 
			avgSpeed /= runtime;
		else avgSpeed = 0;
		return avgSpeed;
	}
	
	/**
	 * Creates segments & joints for this char in given world.
	 * @param world
	 */
	public abstract void initialize(World world);
	
	/**
	 * Sets the debug rendering color of this character
	 * @param color
	 */
	public void setDebugColor(Color3f color) { this.debugColor.set(color);}
	
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
	public void step(SwimSettings settings, float dt) {
		
	}
	
	/**
	 * Updates running stats for this character after a step() call has occurred
	 * @param dt size of simulation step
	 */
	public void updateStats(float dt) {
		if (rootAngleOrigSet == false) {
			rootAngleOrig = getRootBody().getAngle();
			rootAngleOrigSet = true;
		}
		
		//Add to total torques
		float torquesApplied = Math.abs(getPrevTorque());
		totalTorque += torquesApplied;
		
		//Add to total deviation of root body if outside threshold angle range
		float rootAngleDeviation = (float)Math.abs((getRootBody().getAngle() % TWO_PI) - rootAngleOrig);
		if (rootAngleDeviation > SwimCharacter.ROOT_BODY_ANGLE_DEVIATION_THRESHOLD)
			rootAngleDeviation += rootAngleDeviation;
		
		//Update avg speed backing data (actual avg speed found by dividing this value by runtime)
		totalRootBodySpeedTimesDt += getRootBody().getLinearVelocity().x * dt;
		
		runtime += dt;
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
	 * Returns a string containing various useful stats
	 * @return
	 */
	public String getStatisticsString() {
		String stats = "";
		stats += "  Sim Time:                   " + String.format("%.1f%n", runtime);
		stats += "  Avg speed:                  " + String.format("%.3f%n", getAvgRootBodySpeed());
		stats += "  Total torques:              " + String.format("%.1f%n", getTotalTorque());
		stats += "  Total Root Angle Deviation: " + String.format("%.1f%n", getTotalRootOrientationDeviation());
		return stats;
	}
	
	/**
	 * Renders debug visuals for this character to the screen.
	 * Default is to draw body shapes, but can be changed by subclasses
	 * @param debugDraw
	 */
	public void debugDraw(DebugDraw debugDraw) {
		Transform transform = new Transform();
		
		for (Body body : bodies) {
			transform.set(body.getTransform());
			for (Fixture fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext())
				drawPolygon((PolygonShape) fixture.getShape(), transform, debugColor, debugDraw, true);
		}
	}
	
	/** Useful debug drawing routine */
	protected void drawPolygon(PolygonShape shape, Transform transform, Color3f color, DebugDraw debugDraw, boolean shapeInWorldSpace) {
		int vertexCount = shape.m_vertexCount;
		assert (vertexCount <= Settings.maxPolygonVertices);
		Vec2[] vertices = tlvertices.get(Settings.maxPolygonVertices);
		
		for (int i = 0; i < vertexCount; ++i) {
			Transform.mulToOut(transform, shape.m_vertices[i], vertices[i]);
		}
		
		debugDraw.drawSolidPolygon(vertices, vertexCount, color, shapeInWorldSpace);
	}
}
