package ubc.swim.dynamics.controllers;

import org.jbox2d.collision.shapes.MassData;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Mat22;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.pooling.TLMassData;
import org.jbox2d.pooling.TLVec2;

/**
 * Static functions used for fluid calculations
 * 
 * @author Ben Humberston
 *
 */
public class BuoyancyUtil {
	// djm pooling, and from above
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
	 * Compute the volume and centroid of given shape, intersected with fluid half-plane 
	 * BH, 12.14.2011: Only works for PolygonShape objects at the moment
	 *  
	 * Adapted from
	 * http://personal.boristhebrave.com/project/b2buoyancycontroller
	 * 
	 * @param shape the shape whose submerged area will be calculated
	 * @param normal
	 *            the surface normal
	 * @param offset
	 *            the surface offset along normal
	 * @param c
	 *            the computed centroid of the submerged area
	 * @return the total volume less than offset along normal
	 */
	public static float computeSubmergedArea(Shape shape, Vec2 normal, float offset, Transform transform, Vec2 c) {
		float area = 0;
		
		//Currently, only implemented for polygons
		if (shape instanceof PolygonShape) {
			PolygonShape pshape = (PolygonShape) shape;
			
			final Vec2 normalL = tlNormalL.get();
			final MassData md = tlMd.get();
	
			// Transform plane into shape co-ordinates
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
	
			switch (diveCount) {
			case 0:
				if (lastSubmerged) {
					// Completely submerged
					pshape.computeMass(md, 1.0f);
					Transform.mulToOut(transform, md.center, c);
					return md.mass;
				} else {
					return 0;
				}
	
			case 1:
				if (intoIndex == -1) {
					intoIndex = pshape.m_vertexCount - 1;
				} else {
					outoIndex = pshape.m_vertexCount - 1;
				}
				break;
			}
	
			final Vec2 intoVec = tlIntoVec.get();
			final Vec2 outoVec = tlOutoVec.get();
			final Vec2 e1 = tlE1.get();
			final Vec2 e2 = tlE2.get();
	
			int intoIndex2 = (intoIndex + 1) % pshape.m_vertexCount;
			int outoIndex2 = (outoIndex + 1) % pshape.m_vertexCount;
	
			float intoLambda = (0 - depths[intoIndex])
					/ (depths[intoIndex2] - depths[intoIndex]);
			float outoLambda = (0 - depths[outoIndex])
					/ (depths[outoIndex2] - depths[outoIndex]);
	
			intoVec.set(pshape.m_vertices[intoIndex].x * (1 - intoLambda)
					+ pshape.m_vertices[intoIndex2].x * intoLambda,
					pshape.m_vertices[intoIndex].y * (1 - intoLambda)
							+ pshape.m_vertices[intoIndex2].y * intoLambda);
			outoVec.set(pshape.m_vertices[outoIndex].x * (1 - outoLambda)
					+ pshape.m_vertices[outoIndex2].x * outoLambda,
					pshape.m_vertices[outoIndex].y * (1 - outoLambda)
							+ pshape.m_vertices[outoIndex2].y * outoLambda);
	
			// Initialize accumulator
			final Vec2 center = tlCenter.get();
			center.setZero();
			final Vec2 p2b = tlP2b.get().set(pshape.m_vertices[intoIndex2]);
			final Vec2 p3 = tlP3.get();
			p3.setZero();
	
			float k_inv3 = 1.0f / 3.0f;
	
			// An awkward loop from intoIndex2+1 to outIndex2
			i = intoIndex2;
			while (i != outoIndex2) {
				i = (i + 1) % pshape.m_vertexCount;
				if (i == outoIndex2) {
					p3.set(outoVec);
				} else {
					p3.set(pshape.m_vertices[i]);
				}
	
				// Add the triangle formed by intoVec,p2,p3
				{
					e1.set(p2b).subLocal(intoVec);
					e2.set(p3).subLocal(intoVec);
	
					float D = Vec2.cross(e1, e2);
	
					float triangleArea = 0.5f * D;
	
					area += triangleArea;
	
					// Area weighted centroid
					center.x += triangleArea * k_inv3 * (intoVec.x + p2b.x + p3.x);
					center.y += triangleArea * k_inv3 * (intoVec.y + p2b.y + p3.y);
				}
				//
				p2b.set(p3);
			}
	
			// Normalize and transform centroid
			center.x *= 1.0f / area;
			center.y *= 1.0f / area;
	
			Transform.mulToOut(transform, center, c);
		}

		return area;
	}
}
