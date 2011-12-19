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
import ubc.swim.world.trajectory.PolynomialTrajectory;
import ubc.swim.world.trajectory.RefTrajectory;
import ubc.swim.world.trajectory.SineTrajectory;
import ubc.swim.world.trajectory.TrajectoryUtil;

/**
 * A roughly humanoid swimmer that uses PD-controllers with optimized reference trajectories
 * @author Ben Humberston
 *
 */
public class RefTrajHumanChar extends SwimCharacter {
	private static final Logger log = LoggerFactory.getLogger(RefTrajHumanChar.class);
	
	protected static final float TWO_PI = (float)(2 * Math.PI);
	
	protected static final int NUM_SINE_TRAJECTORIES_PER_SIDE = 3; //one each for elbows, hips, and knees (shoulders use poly trajectory)
	
	protected static final int NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY = 2; //increase or decrease to control complexity
	protected static final int NUM_PARAMS_PER_SINE_BASIS_FUNC = 3;	 	//amplitude (weight), phase offset, and period 
	protected static final int NUM_PARAMS_PER_SINE_TRAJECTORY = NUM_PARAMS_PER_SINE_BASIS_FUNC * NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY;
	
	protected static final int NUM_BASIS_FUNCS_PER_SHOULDER_TRAJECTORY = 2; //increase or decrease to control complexity 
	protected static final int NUM_PARAMS_PER_SHOULDER_BASIS_FUNC = 2; 		//coefficient & exponent 
	protected static final int NUM_PARAMS_PER_SHOULDER_TRAJECTORY = NUM_PARAMS_PER_SHOULDER_BASIS_FUNC * NUM_BASIS_FUNCS_PER_SHOULDER_TRAJECTORY;
	
	protected static final float MIN_STROKE_PERIOD = 0.2f; 
	
	//if true, the normalized angle of character's shoulder is used as phase input to other joint trajectories
	//NOTE: if this changes, character must be re-optimized
	protected static final boolean USE_RIGHT_SHOULDER_ANGLE_AS_PHASE = true;  
	
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
	
	protected float shoulderPeriod; //time period over which shoulders go through full rotation
	
	protected ArrayList<Body> rightBodies;
	protected ArrayList<Body> leftBodies;
	
	protected ArrayList<Joint> joints;
	protected ArrayList<Joint> leftJoints;
	protected ArrayList<Joint> rightJoints;
	
	protected RevoluteJoint rightShoulderJoint = null; //angle of this joint is optionally used to drive phase
	
	protected float prevTorque = 0.0f;
	
	protected float targetRightShoulderPhase = 0.0f; //phase goal for right shoulder after last step()
	
	protected ArrayList<RefTrajectory> trajectories;
	protected ArrayList<SineTrajectory> sineTrajectories;
	protected ArrayList<PolynomialTrajectory> shoulderTrajectories;
	
	/**
	 * Create a new human character with given stroke
	 * shoulderPeriod specifies the time interval over which shoulder joints try to 
	 * execute a full rotation.
	 * @param stroke
	 */
	public RefTrajHumanChar(Stroke stroke, float shoulderPeriod) {
		super();
		
		this.stroke = stroke;
		this.shoulderPeriod = shoulderPeriod;
	
		rightBodies = new ArrayList<Body>();
		leftBodies = new ArrayList<Body>();
		
		joints = new ArrayList<Joint>();
		
		leftJoints = new ArrayList<Joint>();
		rightJoints = new ArrayList<Joint>();
		
		trajectories = new ArrayList<RefTrajectory>();
		sineTrajectories = new ArrayList<SineTrajectory>();
		shoulderTrajectories = new ArrayList<PolynomialTrajectory>();
	}

	@Override
	public int getNumControlDimensions() {
		//NOTE: shoulder trajectory is manually coded and not included in control dims
		
		int numJoints = 1; //1 for head
		numJoints += 1; //elbow 
		numJoints += 2; //hip & knee
		return numJoints * NUM_PARAMS_PER_SINE_TRAJECTORY;
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
			RevoluteJoint neckJoint = (RevoluteJoint)(world.createJoint(rjd));
			joints.add(neckJoint);
			SineTrajectory neckTrajectory = new SineTrajectory(NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY);
			neckTrajectory.setJoint(neckJoint);
			sineTrajectories.add(neckTrajectory);
			
			bodies.add(head);
		}

		//Create arms and legs for left and right sides
		//NOTE: bit of a mess and could be simplified a good deal, but it works...
		Vec2 armJointPoint = new Vec2(torsoHeight/2, 0.0f);
		Vec2 legJointPoint = new Vec2(-torsoHeight/2, 0.0f);
		ArrayList<Joint> sideJoints = null;
		for (int i = 0; i < 2; i++) {
			boolean definingLeftSide = (i == 1);
			
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(upperArmLen/2, upperArmWidth/2);
			
			sideJoints = (i == 0) ? rightJoints : leftJoints;
	
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
			RevoluteJoint shoulderJoint = (RevoluteJoint)world.createJoint(rjd);
			if (definingLeftSide == false) 
				rightShoulderJoint = shoulderJoint;
			sideJoints.add(shoulderJoint);
			PolynomialTrajectory shoulderTraj = new PolynomialTrajectory();
			shoulderTraj.setJoint(shoulderJoint);
			//Manually-defined linear trajectory for shoulder through a full rotation
			for (int j = 0; j < NUM_BASIS_FUNCS_PER_SHOULDER_TRAJECTORY; j++) {
				//Slope term (exponent == 1). 
				if (j == 1) {
					float shoulderFuncSlope = -TWO_PI / shoulderPeriod;
					//If this is the left shoulder trajectory, we'll use right shoulder angle
					//as its phase input, so use normalized slope (phase goes from 0 to 1)
					if (USE_RIGHT_SHOULDER_ANGLE_AS_PHASE && definingLeftSide )
						shoulderFuncSlope = -TWO_PI;
					shoulderTraj.setTermCoefficient(j, shoulderFuncSlope);
				}
				else //all other terms set to zero for now
					shoulderTraj.setTermCoefficient(j, 0); 
			}
			shoulderTrajectories.add(shoulderTraj);
			
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
			
			//Elbow joint
			{
				rjd = new RevoluteJointDef();
				rjd.enableLimit = true;
				rjd.upperAngle = maxElbowAngle;
				rjd.lowerAngle = minElbowAngle;
				rjd.initialize(upperArm, lowerArm, new Vec2(0.5f * (upperArm.getPosition().x + lowerArm.getPosition().x), 0.5f * (upperArm.getPosition().y + lowerArm.getPosition().y)));
				sideJoints.add(world.createJoint(rjd));
				RevoluteJoint elbowJoint = (RevoluteJoint)(world.createJoint(rjd));
				sideJoints.add(elbowJoint);
				SineTrajectory elbowTrajectory = new SineTrajectory(NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY);
				elbowTrajectory.setJoint(elbowJoint);
				sineTrajectories.add(elbowTrajectory);
			}
			
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
			
			//Hip joint
			{
				rjd = new RevoluteJointDef();
				rjd.enableLimit = true;
				rjd.upperAngle = (float)Math.PI / 4;
				rjd.lowerAngle = (float)-Math.PI / 4;
				rjd.initialize(torso, upperLeg, new Vec2(legJointPoint.x, legJointPoint.y));
				sideJoints.add(world.createJoint(rjd));
				RevoluteJoint hipJoint = (RevoluteJoint)(world.createJoint(rjd));
				sideJoints.add(hipJoint);
				SineTrajectory hipTrajectory = new SineTrajectory(NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY);
				hipTrajectory.setJoint(hipJoint);
				sineTrajectories.add(hipTrajectory);
			}
			
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
			
			//Knee joint
			{
				rjd = new RevoluteJointDef();
				rjd.enableLimit = true;
				rjd.upperAngle = (float)0;
				rjd.lowerAngle = (float)-Math.PI * 0.9f;
				rjd.initialize(upperLeg, lowerLeg, new Vec2(0.5f * (upperLeg.getPosition().x + lowerLeg.getPosition().x), 0.5f * (upperLeg.getPosition().y + lowerLeg.getPosition().y)));
				sideJoints.add(world.createJoint(rjd));
				RevoluteJoint kneeJoint = (RevoluteJoint)(world.createJoint(rjd));
				sideJoints.add(kneeJoint);
				SineTrajectory kneeTrajectory = new SineTrajectory(NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY);
				kneeTrajectory.setJoint(kneeJoint);
				sineTrajectories.add(kneeTrajectory);
			}
			
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
		}
		
		trajectories.addAll(sineTrajectories);
		trajectories.addAll(shoulderTrajectories);
		
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
		
		//Trajectory order:
		// Sine trajectories
		//    neck
		//    For left & right sides (split by side)
		//	    elbows
		//	    hips
		//      knees
		// Poly trajectories
		//    shoulders (note: manually controlled; input params do not modify these trajectories)
		
		//Update sine-based reference trajectory params
		int leftSideParamsStartIdx = 1 + (sineTrajectories.size() - 1) / 2; //left hand controls start in second half of sublist once neck is removed
		for (int i = 0; i < sineTrajectories.size(); i++) {
			boolean isLeftSideTraj = i >= leftSideParamsStartIdx;
			
			SineTrajectory trajectory = (SineTrajectory)sineTrajectories.get(i);
			
			int paramsIdx = i * NUM_PARAMS_PER_SINE_TRAJECTORY;
			//Mirror right side controls onto corresponding left side joints
			if (isLeftSideTraj) 
				paramsIdx -= NUM_SINE_TRAJECTORIES_PER_SIDE * NUM_PARAMS_PER_SINE_TRAJECTORY;
			
			//Set vals for each basis function of the trajectory
			for (int j = 0; j < NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY; j++) {
				int paramsIdxOffset = j * NUM_PARAMS_PER_SINE_BASIS_FUNC;
				
				float weight = 		(float)params[paramsIdx + paramsIdxOffset];	
				float phaseOffset = (float)params[paramsIdx + paramsIdxOffset + 1];

				float period = shoulderPeriod;
				//Use normalized period if driven by right shoulder phase
				if (USE_RIGHT_SHOULDER_ANGLE_AS_PHASE)
					period = 1.0f;	
				
				//If using crawl stroke, add additional 180 degree phase offset to left side vs. right side
				if (stroke == Stroke.CRAWL && j > leftSideParamsStartIdx)
					phaseOffset += (float) Math.PI;
				
				trajectory.setSineParams(j, weight, period, phaseOffset);
			}
		}
		
	}
	
	@Override
	public void step(SwimSettings settings, float dt) {
		if (dt == 0) return;
		
		prevTorque = 0.0f;
		
		final float PD_GAIN = 1.0f;
		final float PD_DAMPING = 0.001f;
		
		float phase = runtime; //normally, use time as phase
		//May use normalized angle of right shoulder as phase instead
		if (USE_RIGHT_SHOULDER_ANGLE_AS_PHASE) {
			targetRightShoulderPhase = runtime % shoulderPeriod;
			phase = getRightShoulderPhase();
		}
		
		//TODO: use time to drive right shoulder, but use right shoulder phase 
		//to drive other trajectories (requires tweaking left should trajectory period)
		
		//Update shoulder trajectories
		//TODO: pretty much same update as for other trajs; merge?
		for (int i = 0; i < shoulderTrajectories.size(); i++) {
			boolean updatingLeftShoulder = (i == 1);
			
			float shoulderPhase = updatingLeftShoulder ? phase : runtime;
			
			RefTrajectory trajectory = shoulderTrajectories.get(i);
			RevoluteJoint joint = trajectory.getJoint();
			
			float jointAngle = joint.getJointAngle() % TWO_PI;
			float jointSpeed = joint.getJointSpeed();
			
			float targAngle = trajectory.getValue(shoulderPhase) % TWO_PI;
			
			float distFromTargAngle = TrajectoryUtil.distanceBetweenAngles(jointAngle, targAngle); //handles cyclic nature of angles
			
			//PD controller
			float torque = -PD_GAIN * (distFromTargAngle) - PD_DAMPING * jointSpeed;
			
			joint.getBodyA().applyTorque(torque);
			joint.getBodyB().applyTorque(-torque);
			
			prevTorque += torque;
		}
		
		//Update the other trajectories (neck, elbows, knees, hips)
		for (int i = 0; i < sineTrajectories.size(); i++) {
			RefTrajectory trajectory = sineTrajectories.get(i);
			RevoluteJoint joint = trajectory.getJoint();
			
			float jointAngle = joint.getJointAngle() % TWO_PI;
			float jointSpeed = joint.getJointSpeed();
			
			float targAngle = trajectory.getValue(phase) % TWO_PI;
			
			float distFromTargAngle = TrajectoryUtil.distanceBetweenAngles(jointAngle, targAngle); //handles cyclic nature of angles
			
			//PD controller
			float torque = -PD_GAIN * (distFromTargAngle) - PD_DAMPING * jointSpeed;
			
			joint.getBodyA().applyTorque(torque);
			joint.getBodyB().applyTorque(torque);
			
			prevTorque += torque;
		}
	}
	
	@Override
	public float getPrevTorque() {
		return prevTorque;
	}
	
	protected float getRightShoulderPhase() {
		//Map shoulder angle into [0,2PI] range
		float rightShoulderAngle = rightShoulderJoint.getJointAngle() % TWO_PI;
		if (rightShoulderAngle < 0) 
			rightShoulderAngle += TWO_PI;
		rightShoulderAngle = TWO_PI - rightShoulderAngle; //have increasing, not decreasing, phase
		//Then map again into [0,1]
		float phase = rightShoulderAngle / TWO_PI;
		
		return phase;
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
		
//		//Draw current phase if using right shoulder phase
//		if (USE_RIGHT_SHOULDER_ANGLE_AS_PHASE) {
//			float phase = getRightShoulderPhase();
//			
//			float phaseBoxWidth = 40;
//			float phaseBoxHeight = 200;
//			float targetPhaseLevelWidth = 5;
//			float phaseLevel = phaseBoxHeight * phase;
//			float targetPhaseLevel = phaseBoxHeight * targetRightShoulderPhase;
//			
//			//Background holder
//			PolygonShape box = new PolygonShape();
//			Transform phaseBoxTransform = new Transform(new Vec2(20, 300), new Mat22(1, 0, 0, 1));
//			box.setAsBox(phaseBoxWidth/2, phaseBoxHeight/2);
//			drawPolygon(box, phaseBoxTransform, new Color3f(1, 1, 1), debugDraw, false);
//			
//			//Phase level
//			box = new PolygonShape();
//			Transform phaseLevelTransform = new Transform(phaseBoxTransform);
//			phaseLevelTransform.position.y += (phaseBoxHeight/2 - phaseLevel/2);
//			box.setAsBox(phaseBoxWidth/2, phaseLevel/2);
//			drawPolygon(box, phaseLevelTransform, new Color3f(1,0,1), debugDraw, false);
//			
//			//Target phase level
//			box = new PolygonShape();
//			Transform targetLevelTransform = new Transform(phaseBoxTransform);
//			targetLevelTransform.position.y += (targetPhaseLevelWidth/2 + phaseBoxHeight/2 - targetPhaseLevel);
//			box.setAsBox(phaseBoxWidth/2, targetPhaseLevelWidth/2); //just a moving horizontal level line
//			drawPolygon(box, targetLevelTransform, new Color3f(1,1,0), debugDraw, false);
//		}
	}
}
