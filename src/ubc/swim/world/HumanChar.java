package ubc.swim.world;

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

import ubc.swim.gui.SwimSettings;

/**
 * A roughly humanoid swimmer
 * @author Ben Humberston
 *
 */
public class HumanChar extends Character {
	
	public enum Stroke {
		CRAWL,
		FLY
	}
	
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
	
	protected ArrayList<Joint> shoulderJoints;
	protected ArrayList<Joint> elbowJoints;
	protected ArrayList<Joint> hipJoints;
	protected ArrayList<Joint> kneeJoints;
	
	/**
	 * Create a new human character with given stroke as goal
	 * @param stroke
	 */
	public HumanChar(Stroke stroke) {
		super();
		
		this.stroke = stroke;
	
		shoulderJoints = new ArrayList<Joint>();
		elbowJoints = new ArrayList<Joint>();
		
		hipJoints = new ArrayList<Joint>();
		kneeJoints = new ArrayList<Joint>();
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
			rjd.upperAngle = (float)Math.PI/6;
			rjd.lowerAngle = (float)-Math.PI/2 * 0.8f;
			world.createJoint(rjd);
			
			bodies.add(head);
		}

		//Create arms
		//Note that we start them either in phase or 180 degrees out of phase with each other,
		//depending on whether we'll be doing crawl or butter stroke
		Vec2 armJointPoint = new Vec2(torsoHeight/2, 0.0f);
		for (int i = 0; i < 2; i++) {
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(upperArmLen/2, upperArmWidth/2);
	
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
			shoulderJoints.add(world.createJoint(rjd));
			
			bodies.add(upperArm);
			
			//Lower Arm
			bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			//Start second arm lifted above head for crawl
			float lowerArmOffset = 0, lowerArmRot = 0;
			if (i == 1 && stroke == Stroke.CRAWL) {
				lowerArmOffset = -upperArmLen/2 - lowerArmLen/2;
				lowerArmRot = 0;
			}
			else {
				lowerArmOffset = upperArmLen/2 + lowerArmLen/2;
				lowerArmRot = (float)Math.PI;
			}
			bd.position.set(upperArm.getPosition().x + lowerArmOffset, 0.0f);
			bd.angle = lowerArmRot;
			Body lowerArm = world.createBody(bd);
			lowerArm.createFixture(shape, defaultDensity);
			
			rjd = new RevoluteJointDef();
			rjd.enableLimit = true;
			rjd.upperAngle = (float)0;
			rjd.lowerAngle = (float)-Math.PI * 0.9f;
			rjd.initialize(upperArm, lowerArm, new Vec2(0.5f * (upperArm.getPosition().x + lowerArm.getPosition().x), 0.5f * (upperArm.getPosition().y + lowerArm.getPosition().y)));
			elbowJoints.add(world.createJoint(rjd));
			
			bodies.add(lowerArm);
		}
		
		//Create legs
		Vec2 legJointPoint = new Vec2(-torsoHeight/2, 0.0f);
		for (int i = 0; i < 2; i++) {
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(upperLegLen/2, upperLegWidth/2);
	
			//Upper leg
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			//TODO: start in/out of phase for fly/crawl (constant for now)
			float upperLegOffset = (i == 1 && stroke == Stroke.CRAWL) ? -upperLegLen/2 : -upperLegLen/2;
			bd.position.set(legJointPoint.x + upperLegOffset, legJointPoint.y);
			Body upperLeg = world.createBody(bd);
			upperLeg.createFixture(shape, defaultDensity);
			
			RevoluteJointDef rjd = new RevoluteJointDef();
			rjd.enableLimit = true;
			rjd.upperAngle = (float)Math.PI / 4;
			rjd.lowerAngle = (float)-Math.PI / 4;
			rjd.initialize(torso, upperLeg, new Vec2(legJointPoint.x, legJointPoint.y));
			hipJoints.add(world.createJoint(rjd));
			
			bodies.add(upperLeg);
			
			//Lower Arm
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
			kneeJoints.add(world.createJoint(rjd));
			
			bodies.add(lowerLeg);
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
	public void step(SwimSettings settings, float runtime) {
//		//TODO: use control params. Just debugging for now
//		for (int i = 0; i < shoulderJoints.size(); i++) {
//			RevoluteJoint shoulderJoint = (RevoluteJoint)shoulderJoints.get(i);
//			Body upperArm = shoulderJoint.getBodyB();
//			float shoulderForce = 1.0f + 3.0f * (float)Math.abs(Math.sin(upperArm.getTransform().getAngle()));
//			upperArm.applyForce(upperArm.getWorldVector(new Vec2(0, shoulderForce)), upperArm.getWorldPoint(new Vec2(0, -upperArmLen/2)));
//		}
	}
}
