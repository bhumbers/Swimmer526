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

import ubc.swim.gui.SwimSettings;
import ubc.swim.world.trajectory.TrajectoryUtil;

/**
 * A very simple paddle-driven character that uses reference trajectories and PD controller torques
 * @author Ben Humberston
 *
 */
public class RefTrajPaddleChar extends SwimCharacter {
//	private static final Logger log = LoggerFactory.getLogger(RefTrajPaddleChar.class);
	
	protected static int NUM_REF_ANGLES = 2;
	
	protected static final float STROKE_PERIOD_SCALE = 2; //seconds
	protected static final float MIN_STROKE_PERIOD = 0.1f; 
	protected static final float MAX_DEFAULT_TORQUE = 100; //N-m
	
	protected static final int NUM_CONTROL_DIMENSIONS = NUM_REF_ANGLES;
	
	//Body params
	protected float deckLen = 3;
	protected float deckWidth = 0.5f;
	protected float propLen = 1.5f;
	protected float propWidth = 0.2f;
	
	protected float prevTorque = 0.0f;

	protected ArrayList<Joint> joints;

	public RefTrajPaddleChar() {
		super();
	
		joints = new ArrayList<Joint>();
	}
	
	@Override
	public int getNumControlDimensions() { 
		return NUM_CONTROL_DIMENSIONS;
	}
	
	@Override
	public void initialize(World world) {
		float defaultDensity = 1.1f;
		float deckDensity = 0.5f;
		
		//Create the main deck
		Body deck;
		{
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(deckLen/2, deckWidth/2);
	
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			deck = world.createBody(bd);
			deck.createFixture(shape, deckDensity);
			bodies.add(deck);
			
			rootBody = deck;
		}
		
		Body prop;
		{
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(propLen/2, propWidth/2);
	
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			bd.position.set(-deckLen/2, 0.0f);
			prop = world.createBody(bd);
			prop.createFixture(shape, defaultDensity);
			
			RevoluteJointDef rjd = new RevoluteJointDef();
			rjd.initialize(deck, prop, new Vec2(-deckLen/2, 0.0f));
			joints.add(world.createJoint(rjd));
			
			bodies.add(prop);
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
		this.controlParams = params;		
	}
	
	@Override
	public void step(SwimSettings settings, float dt) {
		if (dt == 0) return;
		
		prevTorque = 0.0f;
		
		if (dt == 0) return;
		
		float period = 2.0f;
		float timePerRefAngle = period / NUM_REF_ANGLES;
		
		final float PD_GAIN = 1.0f;
		final float PD_DAMPING = 0.01f;
		
		for (int i = 0; i < joints.size(); i++) {
			RevoluteJoint joint = (RevoluteJoint) joints.get(i);
			
			float jointAngle = joint.getJointAngle() % (float)(2 * Math.PI);
			float jointSpeed = joint.getJointSpeed();
			
			float[] targetAngles = new float[NUM_REF_ANGLES];
			for (int j = 0; j < controlParams.length; j += NUM_REF_ANGLES) {
				for (int k = 0; k < NUM_REF_ANGLES; k++)
					targetAngles[k] = (float)(2 * Math.PI * controlParams[j + k]);
				
				int prevTargIdx = (int)Math.floor((runtime % period) / timePerRefAngle);
				int nextTargIdx = (prevTargIdx + 1) % NUM_REF_ANGLES;
				
				float prevTargAngle = targetAngles[prevTargIdx];
				float nextTargAngle = targetAngles[nextTargIdx];
				while (nextTargAngle < prevTargAngle) //map next angle to be greater than prev
					nextTargAngle += (float)(2 * Math.PI);
				
				float targAngle = (prevTargAngle + nextTargAngle) * 0.5f;
				
				float distFromTargAngle = TrajectoryUtil.distanceBetweenAngles(jointAngle, targAngle); //handles cyclic nature of angles
				
				//PD controller
				float torque = -PD_GAIN * (distFromTargAngle) - PD_DAMPING * jointSpeed;
				
				joint.getBodyA().applyTorque(torque);
				joint.getBodyB().applyTorque(-torque);
				
				prevTorque += torque;
			}
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
