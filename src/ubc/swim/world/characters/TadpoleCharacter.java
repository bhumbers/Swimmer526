package ubc.swim.world.characters;

import java.util.ArrayList;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Filter;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ubc.swim.gui.SwimSettings;
import ubc.swim.world.trajectory.RefTrajectory;
import ubc.swim.world.trajectory.SineTrajectory;
import ubc.swim.world.trajectory.TrajectoryUtil;

/**
 * A multi-segment tadpole-like character that uses PD-controllers with joint angle reference trajectories
 * @author Ben Humberston
 *
 */
public class TadpoleCharacter extends SwimCharacter {
	private static final Logger log = LoggerFactory.getLogger(TadpoleCharacter.class);
	
	protected static final int NUM_BASIS_FUNCS_PER_TRAJECTORY = 2; //increase or decrease to control complexity
	protected static final int NUM_PARAMS_PER_BASIS_FUNC = 3; //amplitude (weight), phase offset, and period 
	protected static final int NUM_PARAMS_PER_TRAJECTORY = NUM_PARAMS_PER_BASIS_FUNC * NUM_BASIS_FUNCS_PER_TRAJECTORY;
	
	protected static final float MIN_STROKE_PERIOD = 0.2f; 
	
	//Body params
	protected float headLen = 0.5f;
	protected float headWidth = 0.25f;
	protected float segLen = 0.5f;
	protected float segWidth = 0.1f;
	protected int numTailSegments = 1;
	
	protected float prevTorque = 0.0f;

	protected ArrayList<Joint> joints;
	protected ArrayList<RefTrajectory> trajectories;

	public TadpoleCharacter(int numPropSegments) {
		super();
	
		this.numTailSegments = numPropSegments;
		joints = new ArrayList<Joint>();
		trajectories = new ArrayList<RefTrajectory>();
	}
	
	@Override
	public int getNumControlDimensions() { 
		return numTailSegments * NUM_PARAMS_PER_TRAJECTORY;
	}
	
	@Override
	public void initialize(World world) {
		float defaultDensity = 1.1f;
		float deckDensity = 1.1f;
		
		//Create the main deck
		Body deck;
		{
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(headLen/2, headWidth/2);
	
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			deck = world.createBody(bd);
			deck.createFixture(shape, deckDensity);
			bodies.add(deck);
			
			rootBody = deck;
		}
		
		Body prevBody = rootBody;
		float xOffset = -headLen/2 - segLen/2;
		for (int i = 0; i < numTailSegments; i++) {
			Body prop;
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(segLen/2, segWidth/2);
	
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			bd.position.set(xOffset, 0.0f);
			prop = world.createBody(bd);
			prop.createFixture(shape, defaultDensity);
			
			RevoluteJointDef rjd = new RevoluteJointDef();
			rjd.initialize(prevBody, prop, new Vec2(xOffset + segLen/2, 0.0f));
			rjd.enableLimit = true;
			rjd.lowerAngle = -(float)Math.PI * 0.45f;
			rjd.upperAngle = (float)Math.PI * 0.45f;
			joints.add(world.createJoint(rjd));
			trajectories.add(new SineTrajectory(NUM_BASIS_FUNCS_PER_TRAJECTORY));
			
			bodies.add(prop);
			
			prevBody = prop;
			xOffset -= segLen;
		}

		//Set all bodies to be non-colliding with each other
		Filter filter = new Filter();
		filter.groupIndex = -1;
		for (int i = 0; i < bodies.size(); i++) {
			Body body = bodies.get(i);
			body.getFixtureList().setFilterData(filter);
		}
	}
	
	@Override
	public void setControlParams(double[] params) {
		if (params.length != getNumControlDimensions()) {
			log.error("Character expected control params of size " + getNumControlDimensions() + " but was given params of size " + params.length);
			assert(false);
		}
		
		this.controlParams = params;
		
		//Update reference trajectory params
		for (int i = 0; i < trajectories.size(); i++) {
			SineTrajectory trajectory = (SineTrajectory)trajectories.get(i);
			
			int paramsIdx = i * NUM_PARAMS_PER_TRAJECTORY; 
			
			//Set vals for each basis function of the trajectory
			for (int j = 0; j < NUM_BASIS_FUNCS_PER_TRAJECTORY; j++) {
				int paramsIdxOffset = j * NUM_PARAMS_PER_BASIS_FUNC;
				
				float weight = 		(float)params[paramsIdx + paramsIdxOffset];
				float period = 		MIN_STROKE_PERIOD + Math.abs((float)params[paramsIdx + paramsIdxOffset + 1]);		
				float phaseOffset = (float)params[paramsIdx + paramsIdxOffset + 2];
				
				trajectory.setSineParams(j, weight, period, phaseOffset);
			}
		}
	}
	
	@Override
	public void step(SwimSettings settings, float dt) {
		if (dt == 0) return;
		
		prevTorque = 0.0f;
		
		final float PD_GAIN = 0.4f;
		final float PD_DAMPING = 0.01f;
		final float TWO_PI = (float)(2 * Math.PI);
		
		for (int i = 0; i < joints.size(); i++) {
			RevoluteJoint joint = (RevoluteJoint) joints.get(i);
			RefTrajectory trajectory = trajectories.get(i);
			
			float jointAngle = joint.getJointAngle() % TWO_PI;
			float jointSpeed = joint.getJointSpeed();
			
			float targAngle = trajectory.getValue(runtime) % TWO_PI;
			
			float distFromTargAngle = TrajectoryUtil.distanceBetweenAngles(jointAngle, targAngle); //handles cyclic nature of angles
			
			//PD controller
			float torque = -PD_GAIN * (distFromTargAngle) - PD_DAMPING * jointSpeed;
			
			joint.getBodyA().applyTorque(torque);
			joint.getBodyB().applyTorque(-torque);
			
			prevTorque += torque;
		}
	}
	
	@Override
	public float getPrevTorque() {
		return prevTorque;
	}
	
	@Override
	public void debugDraw(DebugDraw debugDraw) {
		
	}
}
