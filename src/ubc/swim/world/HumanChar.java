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
import org.jbox2d.dynamics.joints.RevoluteJointDef;

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
	
	protected Stroke stroke;
	
	protected ArrayList<Joint> rArmJoints;
	protected ArrayList<Joint> lArmJoints;
	protected ArrayList<Joint> rLegJoints;
	protected ArrayList<Joint> lLegJoints;
	
	/**
	 * Create a new human character with given stroke as goal
	 * @param stroke
	 */
	public HumanChar(Stroke stroke) {
		super();
		
		this.stroke = stroke;
	
		rArmJoints = new ArrayList<Joint>();
		lArmJoints = new ArrayList<Joint>();
		
		rLegJoints = new ArrayList<Joint>();
		lLegJoints = new ArrayList<Joint>();
	}
	
	
	@Override
	public void initialize(World world) {
		//Setup params
		float height = 2; //2 meters
		float headHeight = height / 8;
		float headWidth = headHeight * 0.85f;
		float armLen = height * 0.45f;
		float upperArmLen = armLen * (1 / 2.2f);
		float upperArmWidth = height / 16;
		float lowerArmLen = armLen - upperArmLen;
		float lowerArmWidth = upperArmWidth * 0.85f;
		float legLen = height * 0.54f;
		float upperLegLen = legLen * 0.46f;
		float upperLegWidth = height / 10;
		float lowerLegLen = legLen - upperLegLen;
		float lowerLegWidth = upperLegWidth * 0.7f;
		float footLen = height * 0.035f;
		
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
			//Start second arm by side for crawl
			float upperArmOffset = (i == 1 && stroke == Stroke.CRAWL) ? -upperArmLen/2 : upperArmLen/2;
			bd.position.set(armJointPoint.x + upperArmOffset, armJointPoint.y);
			Body upperArm = world.createBody(bd);
			upperArm.createFixture(shape, defaultDensity);
			
			RevoluteJointDef rjd = new RevoluteJointDef();
			rjd.initialize(torso, upperArm, new Vec2(armJointPoint.x, armJointPoint.y));
			world.createJoint(rjd);
			
			bodies.add(upperArm);
			
			//Lower Arm
			bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			//Start second arm by side for crawl
			float lowerArmOffset = (i == 1 && stroke == Stroke.CRAWL) ? -upperArmLen/2 - lowerArmLen/2 : upperArmLen/2 + lowerArmLen/2;
			bd.position.set(upperArm.getPosition().x + lowerArmOffset, 0.0f);
			Body lowerArm = world.createBody(bd);
			lowerArm.createFixture(shape, defaultDensity);
			
			rjd = new RevoluteJointDef();
			rjd.enableLimit = true;
			rjd.upperAngle = (float)0;
			rjd.lowerAngle = (float)-Math.PI * 0.9f;
			rjd.initialize(upperArm, lowerArm, new Vec2(0.5f * (upperArm.getPosition().x + lowerArm.getPosition().x), 0.5f * (upperArm.getPosition().y + lowerArm.getPosition().y)));
			world.createJoint(rjd);
			
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
			world.createJoint(rjd);
			
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
			world.createJoint(rjd);
			
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
}
