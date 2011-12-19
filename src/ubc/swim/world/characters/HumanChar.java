package ubc.swim.world.characters;

import java.util.ArrayList;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Filter;
import org.jbox2d.dynamics.Fixture;
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
 * A roughly humanoid swimmer that uses directly optimized torque motors
 * @author Ben Humberston
 *
 */
public class HumanChar extends SwimCharacter {
	private static final Logger log = LoggerFactory.getLogger(HumanChar.class);
	
	protected static final int NUM_GAUSSIANS_PER_MOTOR = 2;
	protected static final int NUM_PARAMS_PER_GAUSSIAN = 3;
	protected static final int NUM_PARAMS_PER_MOTOR = 1 + NUM_PARAMS_PER_GAUSSIAN * NUM_GAUSSIANS_PER_MOTOR; //+1 for period value
	
	//Hard-coded, but data-driven solution is too time consuming
	//1 motor for head, 2 for arms, 2 for legs (arm & leg controls are mirrored between left & right sides)
	protected static final int NUM_CONTROL_DIMENSIONS = 5 * (1 + (NUM_PARAMS_PER_GAUSSIAN * NUM_GAUSSIANS_PER_MOTOR));
	
	protected static final int MAX_STROKE_PERIOD = 5; //seconds
	protected static final float MAX_DEFAULT_TORQUE = 50; //N-m
	
	//Body params
	protected float height = 2; //2 meters
	protected float headHeight = height / 8;
	protected float headWidth = headHeight * 0.85f;
	protected float armLen = height * 0.45f;
	protected float upperArmLen = armLen * (1 / 2.2f);
	protected float upperArmWidth = height / 16;
	protected float lowerArmLen = armLen - upperArmLen;
	protected float lowerArmWidth = upperArmWidth * 0.85f;
	protected float legLen = height * 0.54f;
	protected float upperLegLen = legLen * 0.46f;
	protected float upperLegWidth = height / 10;
	protected float lowerLegLen = legLen - upperLegLen;
	protected float lowerLegWidth = upperLegWidth * 0.7f;
	protected float footLen = height * 0.035f;
	
	protected Stroke stroke;
	
	protected ArrayList<Body> rightBodies;
	protected ArrayList<Body> leftBodies;
	
	protected ArrayList<Joint> joints;
	protected ArrayList<Joint> leftJoints;
	protected ArrayList<Joint> rightJoints;
	
	//Motors for arms and legs, split by left and right sides
	protected ArrayList<TorqueMotor> rightMotors;
	protected ArrayList<TorqueMotor> leftMotors;
	
	protected float prevTorque = 0.0f;
	
	/**
	 * Create a new human character with given stroke as goal
	 * @param stroke
	 */
	public HumanChar(Stroke stroke) {
		super();
		
		this.stroke = stroke;
	
		rightBodies = new ArrayList<Body>();
		leftBodies = new ArrayList<Body>();
		
		joints = new ArrayList<Joint>();
		
		leftJoints = new ArrayList<Joint>();
		rightJoints = new ArrayList<Joint>();
		
		rightMotors = new ArrayList<TorqueMotor>();
		leftMotors = new ArrayList<TorqueMotor>();
	}

	@Override
	public int getNumControlDimensions() { 
		return NUM_CONTROL_DIMENSIONS;
	}
	
	@Override
	public void initialize(World world) {
		float defaultDensity = 1.1f;
		float torsoDensity = 0.5f;
		
		float torsoHeight = height - legLen - headHeight;
		float torsoWidth = height / 8;
		
		//Create the main torso and head
		Body torso;
		{
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(torsoHeight/2, torsoWidth/2);
	
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			torso = world.createBody(bd);
			torso.createFixture(shape, torsoDensity);
			bodies.add(torso);
			
			rootBody = torso;
		}
		
		Body head;
		{
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(headHeight/2, headHeight/2);
	
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			bd.position.set(torsoHeight/2 + headHeight/2, 0.0f);
			head = world.createBody(bd);
			head.createFixture(shape, defaultDensity);
			
			RevoluteJointDef rjd = new RevoluteJointDef();
			rjd.initialize(torso, head, new Vec2(torsoHeight/2, 0.0f));
			rjd.enableLimit = true;
			rjd.upperAngle = (float)Math.PI/10;
			rjd.lowerAngle = (float)-Math.PI/10;
			Joint neckJoint = (world.createJoint(rjd));
			joints.add(neckJoint);
			
			bodies.add(head);
			
			//Add neck motor
			RevoluteJoint rjoint = (RevoluteJoint) neckJoint;
			GaussianTorqueMotor motor = new GaussianTorqueMotor(rjoint.getBodyA(), rjoint.getBodyB(), MAX_DEFAULT_TORQUE, NUM_GAUSSIANS_PER_MOTOR);
			motors.add(motor);
		}

		//Create arms and legs for left and right sides
		//NOTE: bit of a mess and could be simplified a good deal, but it works...
		Vec2 armJointPoint = new Vec2(torsoHeight/2, 0.0f);
		Vec2 legJointPoint = new Vec2(-torsoHeight/2, 0.0f);
		ArrayList<Joint> sideJoints = null;
		ArrayList<TorqueMotor> sideMotors=  null;
		for (int i = 0; i < 2; i++) {
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(upperArmLen/2, upperArmWidth/2);
			
			sideJoints = (i == 0) ? rightJoints : leftJoints;
			sideMotors = (i == 0) ? rightMotors : leftMotors;
	
			//Upper arm
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			//Start second arm lifted above head for crawl
			float upperArmOffset = 0, upperArmRot = 0;
			if (i == 1 && stroke == Stroke.CRAWL) {
				upperArmOffset = -upperArmLen/2;
				upperArmRot = 0;
			}
			else {
				upperArmOffset = upperArmLen/2;
				upperArmRot = (float)Math.PI;
			}
			bd.position.set(armJointPoint.x + upperArmOffset, armJointPoint.y);
			bd.angle = upperArmRot;
			Body upperArm = world.createBody(bd);
			upperArm.createFixture(shape, defaultDensity);
			
			RevoluteJointDef rjd = new RevoluteJointDef();
			rjd.initialize(torso, upperArm, new Vec2(armJointPoint.x, armJointPoint.y));
			sideJoints.add(world.createJoint(rjd));
			
			bodies.add(upperArm);
			
			//Lower Arm
			shape = new PolygonShape();
			shape.setAsBox(lowerArmLen/2, lowerArmWidth/2);
			bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			//Start second arm lifted above head for crawl
			float lowerArmOffset = 0, lowerArmRot = 0;
			float minElbowAngle = 0, maxElbowAngle = 0;
			if (i == 1 && stroke == Stroke.CRAWL) {
				lowerArmOffset = -upperArmLen/2 - lowerArmLen/2;
				lowerArmRot = 0;
				minElbowAngle = -(float)Math.PI * 0.1f;
				maxElbowAngle = (float)Math.PI * 0.1f;
			}
			else {
				lowerArmOffset = upperArmLen/2 + lowerArmLen/2;
				lowerArmRot = (float)Math.PI;
				minElbowAngle = (float)-Math.PI * 0.1f;
				maxElbowAngle = (float)Math.PI * 0.1f;
			}
			bd.position.set(upperArm.getPosition().x + lowerArmOffset, 0.0f);
			bd.angle = lowerArmRot;
			Body lowerArm = world.createBody(bd);
			lowerArm.createFixture(shape, defaultDensity);
			
			rjd = new RevoluteJointDef();
			rjd.enableLimit = true;
			rjd.upperAngle = maxElbowAngle;
			rjd.lowerAngle = minElbowAngle;
			rjd.initialize(upperArm, lowerArm, new Vec2(0.5f * (upperArm.getPosition().x + lowerArm.getPosition().x), 0.5f * (upperArm.getPosition().y + lowerArm.getPosition().y)));
			sideJoints.add(world.createJoint(rjd));
			
			bodies.add(lowerArm);
			
			//Upper leg
			shape = new PolygonShape();
			shape.setAsBox(upperLegLen/2, upperLegWidth/2);
			bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			//TODO: start in/out of phase for fly/crawl (constant for now)
			float upperLegOffset = (i == 1 && stroke == Stroke.CRAWL) ? -upperLegLen/2 : -upperLegLen/2;
			bd.position.set(legJointPoint.x + upperLegOffset, legJointPoint.y);
			Body upperLeg = world.createBody(bd);
			upperLeg.createFixture(shape, defaultDensity);
			
			rjd = new RevoluteJointDef();
			rjd.enableLimit = true;
			rjd.upperAngle = (float)Math.PI / 4;
			rjd.lowerAngle = (float)-Math.PI / 4;
			rjd.initialize(torso, upperLeg, new Vec2(legJointPoint.x, legJointPoint.y));
			sideJoints.add(world.createJoint(rjd));
			
			bodies.add(upperLeg);
			
			//Lower leg
			shape = new PolygonShape();
			shape.setAsBox(lowerLegLen/2, lowerLegWidth/2);
			bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			//TODO: lower leg offset that varies by stroke (constant for now)
			float lowerLegOffset = (i == 1 && stroke == Stroke.CRAWL) ? -upperLegLen/2 - lowerLegLen/2 : -upperLegLen/2 - lowerLegLen/2;
			bd.position.set(upperLeg.getPosition().x + lowerLegOffset, 0.0f);
			Body lowerLeg = world.createBody(bd);
			lowerLeg.createFixture(shape, defaultDensity);
			
			rjd = new RevoluteJointDef();
			rjd.enableLimit = true;
			rjd.upperAngle = (float)0;
			rjd.lowerAngle = (float)-Math.PI * 0.9f;
			rjd.initialize(upperLeg, lowerLeg, new Vec2(0.5f * (upperLeg.getPosition().x + lowerLeg.getPosition().x), 0.5f * (upperLeg.getPosition().y + lowerLeg.getPosition().y)));
			sideJoints.add(world.createJoint(rjd));
			
			bodies.add(lowerLeg);
			
			if (i == 0) { 
				rightBodies.add(upperArm); 	rightBodies.add(lowerArm);
				rightBodies.add(upperLeg); 	rightBodies.add(lowerLeg); 
			}
			else  { 
				leftBodies.add(upperArm); 	leftBodies.add(lowerArm);
				leftBodies.add(upperLeg); 	leftBodies.add(lowerLeg);
			}
			
			joints.addAll(sideJoints);
			
			//Add one motor for each appendage joint on this side
			for (Joint joint : sideJoints) {
				RevoluteJoint rjoint = (RevoluteJoint) joint;
				GaussianTorqueMotor motor = new GaussianTorqueMotor(rjoint.getBodyA(), rjoint.getBodyB(), MAX_DEFAULT_TORQUE, NUM_GAUSSIANS_PER_MOTOR);
				motors.add(motor);
				sideMotors.add(motor);
			}
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
		
		int leftSideMotorStartIdx = 1 + (motors.size() - 1) / 2;
		
		//Forward each group of params to the corresponding motor
		//NOTE: 
		//    -For some params, we map the original [0,1] range of each parameter into the final control ranges here
		//    -For arms and legs, controls are same between left and right side, except for a possible phase shift
		for (int motorIdx = 0; motorIdx < motors.size(); motorIdx++) {
			GaussianTorqueMotor motor = (GaussianTorqueMotor)motors.get(motorIdx);
			
			boolean isLeftSideControl = (motorIdx >= leftSideMotorStartIdx);
			
			int paramIdx = motorIdx * NUM_PARAMS_PER_MOTOR;
			//Reuse same params from right side control (match to right-side params)
			if (isLeftSideControl)
				paramIdx -= rightMotors.size() * NUM_PARAMS_PER_MOTOR;
			
			float period = (float) params[paramIdx] * MAX_STROKE_PERIOD;
			period = motor.setPeriod(period);
			
			//Update params of each Gaussian basis function
			for (int j = 0; j < NUM_GAUSSIANS_PER_MOTOR; j++) {
				int offset = j * NUM_PARAMS_PER_GAUSSIAN;
				
				float weight 	= (float) params[paramIdx + offset + 1]; //weight is used in range [0,1]
				//left arm and leg use inverted torques from right arm and leg
//				if (isLeftSideControl)
//					weight *= -1;
				
				//Use absolute value for std dev (CMA may pick negative param vals)
				float stdDev 	= Math.abs((float) params[paramIdx + offset + 3] * MAX_STROKE_PERIOD);
				
				float mean 		= (float) params[paramIdx + offset + 2] * MAX_STROKE_PERIOD;
				//Based on the stroke, select different left-right offsets for arms and legs
				switch (stroke) {
					case CRAWL:
						//In crawl stroke, left/right legs and arms run 180 degrees out of phase, so
						//modify means for left side of body
						//TODO: verify this doesn't cause total breakage, make it less of a total hack
						if (isLeftSideControl)
							mean = (mean + period/2) % period;
						break;
					case FLY:
						//Nothing to do... left & right sides run in synch
						break;
				}
				
				motor.setGaussianParams(j, weight, mean, stdDev);
			}
		}
		
	}
	
	@Override
	public void step(SwimSettings settings, float dt) {
		if (dt == 0) return;
		
		prevTorque = 0.0f;
		
//		//Apply each control torque
//		for (TorqueMotor motor : motors) 
//			motor.applyTorque(runtime);
	}
	
	@Override
	public float getPrevTorque() {
		return prevTorque;
	}
	
	@Override
	public void debugDraw(DebugDraw debugDraw) {
		Transform transform = new Transform();
		Color3f color = new Color3f();
		
		
		//Draw left/right coloring (note: left is drawn below right side... it's on the character side *away* from the viewer)
		for (Body body : leftBodies) {
			transform.set(body.getTransform());
			color.set(0.2f, 0.9f, 0.2f);
			for (Fixture fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext())
				drawPolygon((PolygonShape) fixture.getShape(), transform, color, debugDraw, true);
		}
		for (Body body : rightBodies) {
			transform.set(body.getTransform());
			color.set(0.9f, 0.2f, 0.2f);
			for (Fixture fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext())
				drawPolygon((PolygonShape) fixture.getShape(), transform, color, debugDraw, true);
		}
	}
}
