/**
 * 
 */
package ubc.swim.dynamics.controllers;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;

import ubc.swim.gui.SwimSettings;

/**
 * Applies various fluid forces to a list of bodies per time step.
 * 
 * Adapted from http://personal.boristhebrave.com/project/b2buoyancycontroller
 * 
 * @author Ben Humberston
 *
 */
public class BuoyancyController extends DynamicsController {
	/**
	 * @param def
	 */
	protected BuoyancyController(BuoyancyControllerDef def) {
		super(def);
		normal = def.normal.clone();
		offset = def.offset;
		density = def.density;
		velocity = def.velocity.clone();
		linearDrag = def.linearDrag;
		angularDrag = def.angularDrag;
		useDensity = def.useDensity;
		useWorldGravity = def.useWorldGravity;
		gravity = def.gravity.clone();
	}
	
	/** The outer surface normal */
	public Vec2 normal = new Vec2();
	/** The height of the fluid surface along the normal */
	public float offset;
	/** The fluid density */
	public float density;
	/** Fluid velocity, for drag calculations */
	public Vec2 velocity = new Vec2();
	/** Linear drag co-efficient */
	public float linearDrag;
	/** Linear drag co-efficient */
	public float angularDrag;
	/** If false, bodies are assumed to be uniformly dense, otherwise use the shapes densities */
	public boolean useDensity; //False by default to prevent a gotcha
	/** If true, gravity is taken from the world instead of the gravity parameter. */
	public boolean useWorldGravity;
	/** Gravity vector, if the world's gravity is not used */
	public Vec2 gravity = new Vec2();
	
	@Override
	public void step(SwimSettings settings) {
		float oldDensity = density;
		
		//Update world from given settings
		density = (float)settings.getSetting(SwimSettings.FluidDensity).value;
		linearDrag = (float)settings.getSetting(SwimSettings.FluidDrag).value;
		//TODO: what's a good way to scale angular drag? -bh, 12.14.2011
		angularDrag = linearDrag;
		
		boolean densityChanged = oldDensity != density;
		
		if(bodies == null) return;
		if(useWorldGravity) {
			gravity = world.getGravity();
		}
		for (Body body : bodies) {
			
			//If density is unchanged and body is at long term rest (sleeping),
			//we can safely ignore it since buoyancy as a force is constant if position is unchanging
			if(densityChanged == false && body.isAwake() == false)
				continue;
			
			//NOTE: "sub" prefix indicates the submerged portion/version of a given quantity
			
			Vec2 subCenter = new Vec2(0,0);
			Vec2 subCenterOfMass = new Vec2(0,0);
			float subArea = 0;
			float subMass = 0;
			for (Fixture fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext()) {
				Shape shape = fixture.getShape();
				Vec2 subCenterOfShape = new Vec2(0,0);
				float subAreaOfShape = BuoyancyUtil.computeSubmergedArea(shape, normal, offset, fixture.getBody().getTransform(), subCenterOfShape);
				subArea += subAreaOfShape;
				subCenter.x += subAreaOfShape * subCenterOfShape.x;
				subCenter.y += subAreaOfShape * subCenterOfShape.y;
				
				float shapeDensity = 0;
				if(useDensity)
					shapeDensity=fixture.getDensity();
				else
					shapeDensity = 1;
				
				subMass += subAreaOfShape*shapeDensity;
				subCenterOfMass.x += subAreaOfShape * subCenterOfShape.x * shapeDensity;
				subCenterOfMass.y += subAreaOfShape * subCenterOfShape.y * shapeDensity;
			}
			
			subCenter.x/=subArea;
			subCenter.y/=subArea;
			
			subCenterOfMass.x/=subMass;
			subCenterOfMass.y/=subMass;
			
			//Ignore if total submerged area is small
			if(subArea<Settings.EPSILON) 
				continue;
			
			//Buoyancy
			Vec2 buoyancyForce = gravity.mul(-density*subArea);
			body.applyForce(buoyancyForce,subCenterOfMass);
			
			//Linear drag
			//TODO: better drag. -bh, 12.14.2011
			Vec2 dragForce = body.getLinearVelocityFromWorldPoint(subCenter).sub(velocity);
			dragForce.mulLocal(-linearDrag*subArea);
			body.applyForce(dragForce,subCenter);
			
			//Angular drag
			//TODO: Something that makes more physical sense?
			body.applyTorque(-body.getInertia()/body.getMass()*subArea*body.getAngularVelocity()*angularDrag);
			
		}
	}
	
	@Override
	public void draw(DebugDraw debugDraw) {
		float r = 1000;
		Vec2 p1 = normal.mul(offset).addLocal(Vec2.cross(normal, r));
		Vec2 p2 = normal.mul(offset).subLocal(Vec2.cross(normal, r));

		Color3f color = new Color3f(0,0,0.8f);

//		debugDraw.drawSegment(p1, p2, color);
		Vec2[] vertices = new Vec2[] { p1, p2, p2.sub(normal.mul(r)), p1.sub(normal.mul(r))};
		debugDraw.drawSolidPolygon(vertices, 4, color);
	}
}
