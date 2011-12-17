package ubc.swim.dynamics.controllers;

import java.util.List;

import org.jbox2d.collision.shapes.MassData;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Mat22;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.pooling.TLMassData;
import org.jbox2d.pooling.TLVec2;

import ubc.swim.world.Edge;

/**
 * Static functions used for fluid calculations
 * 
 * @author Ben Humberston
 *
 */
public class FluidUtil {
	// some thread-safe pooled vectors used during computation
	private static final TLVec2 tlNormalL = new TLVec2();
	private static final TLMassData tlMd = new TLMassData();
	private static final TLVec2 tlIntoVec = new TLVec2();
	private static final TLVec2 tlOutoVec = new TLVec2();
	private static final TLVec2 tlP2b = new TLVec2();
	private static final TLVec2 tlP3 = new TLVec2();

	private static final TLVec2 tlE1 = new TLVec2();
	private static final TLVec2 tlE2 = new TLVec2();
	private static final TLVec2 tlCenter = new TLVec2();

	/**
	 * Computes the volume and cetroid of part of shape that intersects with fluid half-plane 
	 * whose top edge is at given y offset
	 * 
	 * BH, 12.14.2011: Only works for PolygonShape objects at the moment
	 *  
	 * Adapted from
	 * http://personal.boristhebrave.com/project/b2buoyancycontroller
	 * 
	 * @param shape the shape whose submerged area will be calculated
	 * @param normal the fluid surface normal
	 * @param offset the fluid surface offset along normal
	 * @param outputSubmergedCenter set to be the centerpoint of the submerged part of the given shape
	 * @param outputSubmergedEdges all edges that are under water are added to this list (where edges on the original shape that are entering/exiting the fluid surface are spliced at the point of entry/exit)
	 * @return the total volume less than offset along normal
	 */
	public static float computeSubmergedArea(Shape shape, Vec2 normal, float offset, Transform transform, Vec2 outputSubmergedCenter, List<Edge> outputSubmergedEdges) {
		float area = 0;
		final Vec2 subCenter = tlCenter.get();
		subCenter.setZero();
		
		//Currently, only implemented for polygons
		if (shape instanceof PolygonShape) {
			PolygonShape pshape = (PolygonShape) shape;
			
			final Vec2 normalL = tlNormalL.get();
			final MassData md = tlMd.get();
	
			// Transform fluid plane into shape coordinates
			Mat22.mulTransToOut(transform.R, normal, normalL);
			float offsetL = offset - Vec2.dot(normal, transform.position);
	
			float[] depths = new float[Settings.maxPolygonVertices];
			int diveCount = 0;
			int intoIndex = -1;
			int outoIndex = -1;
	
			boolean lastSubmerged = false;
			int i = 0;
			for (i = 0; i < pshape.m_vertexCount; ++i) {
				depths[i] = Vec2.dot(normalL, pshape.m_vertices[i]) - offsetL;
				boolean isSubmerged = depths[i] < -Settings.EPSILON;
				if (i > 0) {
					if (isSubmerged) {
						if (!lastSubmerged) {
							intoIndex = i - 1;
							diveCount++;
						}
					} else {
						if (lastSubmerged) {
							outoIndex = i - 1;
							diveCount++;
						}
					}
				}
				lastSubmerged = isSubmerged;
			}
	
			//If no edge crossed the water line, then finding submerged area is either all or none
			if (diveCount == 0) {
				//if all vertices were underwater, then all area and edges are submerged
				if (lastSubmerged) {
					//note: assuming density = 1.0f means mass == area; we want to find the latter, so make that assumption
					pshape.computeMass(md, 1.0f);
					Transform.mulToOut(transform, md.center, outputSubmergedCenter);
					
					//Add all shape edges to submerged list
					for (int j = 0; j < pshape.m_vertexCount; j++) {
						int nextVertInd = (j < pshape.m_vertexCount - 1) ? j+1 : 0;
						Vec2 vertA = pshape.m_vertices[j];
						Vec2 vertB = pshape.m_vertices[nextVertInd];
						outputSubmergedEdges.add(new Edge(vertA.x, vertA.y, vertB.x, vertB.y));
					}
					
					area = md.mass; //ie: area, since mass == area with 1.0 density
				}
				//Otherwise, no area or edges are submerged
				else
					area = 0;
			}
			//Otherwise, at least some area is submerged, so find how much
			else {
				//If only one edge crossed the water line, tweak into/outo indices
				if (diveCount == 1) {
					if (intoIndex == -1)
						intoIndex = pshape.m_vertexCount - 1;
					else
						outoIndex = pshape.m_vertexCount - 1;
				}
		
				final Vec2 intoPoint = tlIntoVec.get();
				final Vec2 outoPoint = tlOutoVec.get();
				final Vec2 e1 = tlE1.get();
				final Vec2 e2 = tlE2.get();
		
				int intoIndex2 = (intoIndex + 1) % pshape.m_vertexCount;
				int outoIndex2 = (outoIndex + 1) % pshape.m_vertexCount;
		
				//Find normalized distances along incoming/outgoing edges where shape enters/exits fluid
				float intoLambda = (0 - depths[intoIndex])
						/ (depths[intoIndex2] - depths[intoIndex]);
				float outoLambda = (0 - depths[outoIndex])
						/ (depths[outoIndex2] - depths[outoIndex]);
		
				//Define the 2 points of intersection with fluid surface (1 incoming, 1 outgoing)
				intoPoint.set(pshape.m_vertices[intoIndex].x * (1 - intoLambda) + pshape.m_vertices[intoIndex2].x * intoLambda,
							pshape.m_vertices[intoIndex].y * (1 - intoLambda) + pshape.m_vertices[intoIndex2].y * intoLambda);
				outoPoint.set(pshape.m_vertices[outoIndex].x * (1 - outoLambda) + pshape.m_vertices[outoIndex2].x * outoLambda,
							pshape.m_vertices[outoIndex].y * (1 - outoLambda) + pshape.m_vertices[outoIndex2].y * outoLambda);
		
				// Initialize accumulator
				final Vec2 p2 = tlP2b.get().set(pshape.m_vertices[intoIndex2]);
				final Vec2 p3 = tlP3.get();
				p3.setZero();
		
				float oneThird = 1.0f / 3.0f;
		
				// Iterate over pairs of submerged vertices (2nd index of pair ranges from intoIndex2+1 to outIndex2)
				// and add area of triangle formed by intoPoint and those 2 vertices
				i = intoIndex2;
				int startingInd = (intoIndex2 + 1) % pshape.m_vertexCount;
				while (i != outoIndex2) {
					i = (i + 1) % pshape.m_vertexCount;
					
					if (i == outoIndex2)
						p3.set(outoPoint);
					else
						p3.set(pshape.m_vertices[i]);
		
					e1.set(p2).subLocal(intoPoint);
					e2.set(p3).subLocal(intoPoint);
					
					// Add the area of the triangle formed by intoPoint,p2,p3
					{
						float D = Vec2.cross(e1, e2);
		
						float triangleArea = 0.5f * D;
		
						area += triangleArea;
		
						// Update area-weighted submerged center point
						subCenter.x += triangleArea * ((intoPoint.x + p2.x + p3.x) * oneThird);
						subCenter.y += triangleArea * ((intoPoint.y + p2.y + p3.y) * oneThird);
					}
					
					//Add initial submerged edge on first iteration
					if (i == startingInd)
						outputSubmergedEdges.add(new Edge(intoPoint.x, intoPoint.y, p2.x, p2.y));
					//Add current submerged edge to list on each iteration
					outputSubmergedEdges.add(new Edge(p2.x, p2.y, p3.x, p3.y));
	
					p2.set(p3);
				}
				
				//Normalize submerged center point
				subCenter.x *= 1.0f / area;
				subCenter.y *= 1.0f / area;
				
			} //END: at least 1 edge crossed water line
		
		} //END: polygon shape area-finding
		
		//Transform submerged center and edges to world coords
		Transform.mulToOut(transform, subCenter, outputSubmergedCenter);
		for (Edge subEdge : outputSubmergedEdges) {
			Transform.mulToOut(transform, subEdge.pA, subEdge.pA);
			Transform.mulToOut(transform, subEdge.pB, subEdge.pB);
		}

		return area;
	}
}
