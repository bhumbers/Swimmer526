package ubc.swim.world.characters;

import java.util.ArrayList;

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
import ubc.swim.world.motors.GaussianTorqueMotor;
import ubc.swim.world.motors.TorqueMotor;

/**
 * A very simple paddle-driven character that uses directly optimized torques
 * @author Ben Humberston
 *
 */
public class PaddleChar extends SwimCharacter {
	private static final Logger log = LoggerFactory.getLogger(PaddleChar.class);
	
	protected static int NUM_REF_ANGLES = 2;
	
	protected static final float STROKE_PERIOD_SCALE = 2; //seconds
	protected static final float MIN_STROKE_PERIOD = 0.1f; 
	
	protected static final int NUM_GAUSSIANS_PER_MOTOR = 2;
	protected static final int NUM_PARAMS_PER_GAUSSIAN = 3;
	protected static final int NUM_PARAMS_PER_MOTOR = 1 + NUM_PARAMS_PER_GAUSSIAN * NUM_GAUSSIANS_PER_MOTOR; //+1 for period value
	protected static final int NUM_CONTROL_DIMENSIONS = 1 * (1 + (NUM_PARAMS_PER_GAUSSIAN * NUM_GAUSSIANS_PER_MOTOR));
	
	protected static final int MAX_STROKE_PERIOD = 5; //seconds
	protected static final float MAX_DEFAULT_TORQUE = 500; //N-m
	
	//Body params
	protected float deckLen = 3;
	protected float deckWidth = 0.5f;
	protected float propLen = 1.5f;
	protected float propWidth = 0.2f;
	
	protected float prevTorque = 0.0f;

	protected ArrayList<Joint> joints;
	
	public PaddleChar() {
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
			bd.position.set(-deckLen/2 - propLen/2, 0.0f);
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
		
		//Add motor for each joint
		for (Joint joint : joints) {
			RevoluteJoint rjoint = (RevoluteJoint) joint;
			GaussianTorqueMotor motor = new GaussianTorqueMotor(rjoint.getBodyA(), rjoint.getBodyB(), MAX_DEFAULT_TORQUE, NUM_GAUSSIANS_PER_MOTOR);
			motors.add(motor);
		}
	}
	
	@Override
	public void setControlParams(double[] params) {
		this.controlParams = params;
		
		if (params.length != getNumControlDimensions()) {
			log.error("Character expected control params of size " + getNumControlDimensions() + " but was given params of size " + params.length);
			assert(false);
		}
		
		//Forward each group of params to the corresponding motor
		//NOTE: 
		//    -For some params, we map the original [0,1] range of each parameter into the final control ranges here
		int motorIdx = 0;
		for (int i = 0; i < params.length; i += NUM_PARAMS_PER_MOTOR) {
			GaussianTorqueMotor motor = (GaussianTorqueMotor)motors.get(motorIdx);
			
			float period = (float) params[i] * MAX_STROKE_PERIOD;
			period = motor.setPeriod(period);
			
			//Update params of each Gaussian basis function
			for (int j = 0; j < NUM_GAUSSIANS_PER_MOTOR; j++) {
				int offset = j * NUM_PARAMS_PER_GAUSSIAN;
				
				float weight 	= (float) params[i + offset + 1]; //weight is used in range [0,1]
				float stdDev 	= Math.abs((float) params[i + offset + 3] * MAX_STROKE_PERIOD);
				float mean 		= (float) params[i + offset + 2] * MAX_STROKE_PERIOD;

				motor.setGaussianParams(j, weight, mean, stdDev);
			}
			
			motorIdx++;
		}
		
	}
	
	@Override
	public void step(SwimSettings settings, float dt) {
		if (dt == 0) return;
		
		prevTorque = 0.0f;
		
		//Apply each control torque
		for (TorqueMotor motor : motors) 
			prevTorque += Math.abs(motor.applyTorque(runtime));
	}
	
	@Override
	public float getPrevTorque() {
		return prevTorque;
	}
}
