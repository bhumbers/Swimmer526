/*******************************************************************************
 * Copyright (c) 2011, Daniel Murphy
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
/**
 * Created at 7:47:37 PM Jan 12, 2011
 */
package ubc.swim.tests;

import java.util.Formatter;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;

import ubc.swim.gui.SwimSettings;

/**
 * Paddle wheel test for debugging fluid forces
 * 
 * @author Ben Humberston
 *
 */
public class PaddleTest extends SwimTest {
	
	private RevoluteJoint m_joint1;
	
	@Override
	public void initTest() {
		float pivotHeight = 10.0f;
		
		//Create fixed pivot point
		Body pivot = null;
		{
			BodyDef bd = new BodyDef();
			bd.position.set(0, pivotHeight);
			pivot = getWorld().createBody(bd);
	
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(0.2f, 0.2f);
			pivot.createFixture(shape, 0.0f);
		}

		// Create rotating paddle wheel
		{
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(0.5f, 4.0f);

			BodyDef bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			bd.position.set(0, pivotHeight);
			Body body = getWorld().createBody(bd);
			body.createFixture(shape, 2.0f);
			scenario.addBody(body);

			RevoluteJointDef rjd = new RevoluteJointDef();
			rjd.initialize(pivot, body, new Vec2(bd.position.x, bd.position.y));
			getWorld().createJoint(rjd);
		}
	}
	
	@Override
	public void step(SwimSettings settings) {
		
		super.step(settings);
		
		if (m_joint1 != null) {
			addTextLine("Keys: (f) toggle friction, (m) toggle motor");
			float torque = m_joint1.getMotorTorque();
			Formatter f = new Formatter();
			addTextLine(f.format("Motor Force = %5.0f, ", torque).toString());
		}

	}

	@Override
	public void keyPressed(char argKeyChar, int argKeyCode) {
		
		switch(argKeyChar){
			case 'm':
				m_joint1.enableMotor(!m_joint1.isMotorEnabled());
				getModel().getKeys()['m'] = false;
				break;
		}
	}

	@Override
	public String getTestName() {
		return "Paddle Wheel Test";
	}
	
	@Override
	public String getTestID() {
		return "paddle_wheel_test";
	}

}
