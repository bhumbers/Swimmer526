package ubc.swim.world;

import org.jbox2d.common.Vec2;

/**
 * Representation of a 2D edge using JBox2D's Vec2 for endpoints
 * @author Ben Humberston
 *
 */
public class Edge {
	public Vec2 pA = new Vec2();
	public Vec2 pB = new Vec2();
	
	public Edge(float pAx, float pAy, float pBx, float pBy) {
		pA.set(pAx, pAy);
		pB.set(pBx, pBy);
	}
}
