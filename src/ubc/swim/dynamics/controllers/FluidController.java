/**
 * 
 */
package ubc.swim.dynamics.controllers;

import java.util.ArrayList;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;

import ubc.swim.gui.SwimSettings;
import ubc.swim.world.Edge;

/**
 * Applies various fluid forces to a list of bodies per time step.
 * 
 * Adapted from http://personal.boristhebrave.com/project/b2buoyancycontroller
 * 
 * @author Ben Humberston
 *
 */
public class FluidController extends DynamicsController {
	/**
	 * @param def
	 */
	protected FluidController(FluidControllerDef def) {
		super(def);
		normal = def.normal.clone();
		fluidSurfaceOffset = def.offset;
		fluidDensity = def.density;
		fluidVel = def.velocity.clone();
		linearDrag = def.linearDrag;
		useDensity = def.useDensity;
		useWorldGravity = def.useWorldGravity;
		gravity = def.gravity.clone();
	}
	
	/** The outer surface normal */
	public Vec2 normal = new Vec2();
	/** The height of the fluid surface along the normal */
	public float fluidSurfaceOffset;
	/** The fluid density */
	public float fluidDensity;
	/** Fluid velocity, for drag calculations */
	public Vec2 fluidVel = new Vec2();
	/** Simple fluid drag coefficient */
	public float linearDrag;
	/** If false, bodies are assumed to be uniformly dense, otherwise use the shapes densities */
	public boolean useDensity; //False by default to prevent a gotcha
	/** If true, gravity is taken from the world instead of the gravity parameter. */
	public boolean useWorldGravity;
	/** Gravity vector, if the world's gravity is not used */
	public Vec2 gravity = new Vec2();
	
	/** A list of edges below the water line for all bodies; updated on each step */
	ArrayList<Edge> subEdges = new ArrayList<Edge>();
	/** List of drag forces from last step of this controller, represented as edges (difference of point is direction, length is magnitude) */
	ArrayList<Edge> dragForces = new ArrayList<Edge>();
	
	@Override
	public void step(SwimSettings settings) {
		
		//Update world from given settings
		fluidDensity = (float)settings.getSetting(SwimSettings.FluidDensity).value;
		linearDrag = (float)settings.getSetting(SwimSettings.FluidDrag).value;
		fluidVel.x = (float)settings.getSetting(SwimSettings.FluidVelocity).value;
		
		if(bodies == null) return;
		if(useWorldGravity)
			gravity = world.getGravity();
		
		subEdges.clear();
		dragForces.clear();
		
		ArrayList<Edge> subEdgesForShape = new ArrayList<Edge>();
		
		//A vector perpendicular to the relative fluid velocity for a submerged edge (used in drag calcs)
		Vec2 perpToVelRelToFluid = new Vec2();
		//Vector giving component of relative fluid velocity normal to a submerged edge (used in drag calcs)
		Vec2 velRelToFluidAlongNormal = new Vec2();
		//Vector giving a specific drag force (used in drag calcs)
		Vec2 dragForce = new Vec2();
		
		for (Body body : bodies) {
			if (body.m_type == BodyType.STATIC)
				continue;
			
			subEdgesForShape.clear();
			
			//DISABLING FOR NOW. Causes bugs when fluid vel or other params change at runtime. -bh, 12.15.2011
//			//If density is unchanged and body is at long term rest (sleeping),
//			//we can safely ignore it since buoyancy as a force is constant if position is unchanging
//			if(/*densityChanged == false &&*/ body.isAwake() == false)
//				continue;
			
			//NOTE: "sub" prefix indicates the submerged portion/version of a given quantity
			
			Vec2 subCenter = new Vec2(0,0);
			Vec2 subCenterOfMass = new Vec2(0,0);
			float subArea = 0;
			float subMass = 0;
			for (Fixture fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext()) {
				Shape shape = fixture.getShape();
				Vec2 subCenterOfShape = new Vec2(0,0);
				
				float subAreaOfShape = FluidUtil.computeSubmergedArea(shape, normal, fluidSurfaceOffset, fixture.getBody().getTransform(), subCenterOfShape, subEdgesForShape);
				
				subEdges.addAll(subEdgesForShape);
				
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
				
				//Drag forces
				//Note that the drag force can vary substantially across an edge due to rotation of the body and
				//varying distances from the center or rotation. To approximate total variation of drag across the
				//edge, we break the edge into a number of points and apply point-specific drag forces.
				//Here, we'll just break the edge into the two endpoints and apply the scaled forces at each end.
				for (Edge edge : subEdgesForShape) {
					int numEdgePoints = 2;
					float invNumEdgePoints = 1.0f / numEdgePoints;
					Vec2[] edgePoints = new Vec2[numEdgePoints];
					edgePoints[0] = edge.pA;
					edgePoints[1] = edge.pB;
					
					Vec2 edgeDir = edge.pB.sub(edge.pA);
					Vec2 edgeNorm = new Vec2(edge.pB.y - edge.pA.y, -(edge.pB.x - edge.pA.x));
					
					//Apply force at each sampled edge point
					for (int i = 0; i < edgePoints.length; i++) {
						Vec2 edgePoint = edgePoints[i];
						Vec2 velRelToFluid = body.getLinearVelocityFromWorldPoint(edgePoint);
						velRelToFluid.subLocal(fluidVel);
						float velDotNorm = Vec2.dot(velRelToFluid, edgeNorm);
						if (velDotNorm > 0) {
							edgeNorm.normalize();
							//Find length of the edge when projected onto perpendicular to fluid vel
							perpToVelRelToFluid.set(velRelToFluid.y, -velRelToFluid.x);
							perpToVelRelToFluid.normalize();
							float projectedEdgeLength = Math.abs(Vec2.dot(edgeDir, perpToVelRelToFluid));
							
							//Find drag force directed along velocity component normal to the edge
							velRelToFluidAlongNormal.set(edgeNorm);
							velRelToFluidAlongNormal.mulLocal(Vec2.dot(velRelToFluid, edgeNorm));
							
							dragForce.set(velRelToFluidAlongNormal);
							//TODO: use force proportional to vel squared? (if so, normalize drag force before multiplying by lenSqrd)
							dragForce.mulLocal(-linearDrag*projectedEdgeLength*invNumEdgePoints);
							body.applyForce(dragForce,edgePoint);
							
							//DEBUGGING: Save force for debug drawing later
							Vec2 dragForcePoint = dragForce.mul(0.1f);
							dragForcePoint.addLocal(edgePoint);
							dragForces.add(new Edge(edgePoint.x, edgePoint.y, dragForcePoint.x, dragForcePoint.y));
						}
					}
				}
			}
			
			subCenter.x/=subArea;
			subCenter.y/=subArea;
			
			subCenterOfMass.x/=subMass;
			subCenterOfMass.y/=subMass;
			
			//Ignore if total submerged area is small
			if(subArea<Settings.EPSILON) 
				continue;
			
			//Buoyancy
			Vec2 buoyancyForce = gravity.mul(-fluidDensity*subArea);
			body.applyForce(buoyancyForce,subCenterOfMass);
			
			//Linear drag
			//TODO: better drag that is applied per edge? -bh, 12.14.2011
//			Vec2 dragForce = body.getLinearVelocityFromWorldPoint(subCenter).sub(velocity);
//			dragForce.mulLocal(-linearDrag*subArea);
//			body.applyForce(dragForce,subCenter);
//			
//			//Angular drag
//			//TODO: Something that makes more physical sense?
//			body.applyTorque(-body.getInertia()/body.getMass()*subArea*body.getAngularVelocity()*angularDrag);
			
		}
	}
	
	@Override
	public void draw(DebugDraw debugDraw, SwimSettings settings) {
		boolean drawDragForces = settings.getSetting(SwimSettings.DrawDragForces).enabled;
		
		float r = 1000;
		Vec2 p1 = normal.mul(fluidSurfaceOffset).addLocal(Vec2.cross(normal, r));
		Vec2 p2 = normal.mul(fluidSurfaceOffset).subLocal(Vec2.cross(normal, r));

		Color3f color = new Color3f(0,0,0.8f);

//		debugDraw.drawSegment(p1, p2, color);
		Vec2[] vertices = new Vec2[] { p1, p2, p2.sub(normal.mul(r)), p1.sub(normal.mul(r))};
		debugDraw.drawSolidPolygon(vertices, 4, color);
		
		
//		//DEBUG: Draw submerged edges
//		for (Edge subEdge : subEdges)
//			debugDraw.drawSegment(subEdge.pA, subEdge.pB, new Color3f(0,0.2f,1));
		
		if (drawDragForces) {
			//DEBUG: Draw drag forces
			for (Edge dragForce : dragForces)
				debugDraw.drawSegment(dragForce.pA, dragForce.pB, new Color3f(1,1,0));
		}
	}
}
