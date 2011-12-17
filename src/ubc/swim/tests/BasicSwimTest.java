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
import org.jbox2d.dynamics.joints.RevoluteJoint;

import ubc.swim.gui.SwimSettings;
import ubc.swim.optimization.SwimmerOptimization;
import ubc.swim.world.SwimCharacter;

/**
 * Basic swimming test environment
 * 
 * Adapted from SliderCrankTest.java by Daniel Murphy
 * @author Ben Humberston
 *
 */
public class BasicSwimTest extends SwimTest {
	
	private RevoluteJoint m_joint1;
	private SwimCharacter mainChar;
	
	
	public BasicSwimTest() {
		super();
		
		charIDs.add("paddle");
	}
	
	@Override
	public float getDefaultCameraScale() {
		return 100;
	}
	
	@Override
	public void initTest() {		
		Body ground = null;
		{
			BodyDef bd = new BodyDef();
			ground = getWorld().createBody(bd);

			PolygonShape shape = new PolygonShape();
			shape.setAsEdge(new Vec2(-40.0f, 0.0f), new Vec2(40.0f, 0.0f));
			ground.createFixture(shape, 0.0f);
		}
		
//		//TODO: remove. Testing controls
//		for (SwimCharacter character : scenario.getCharacters()) {
//			double[] control = new double[character.getNumControlDimensions()];
//			for (int i = 0; i < 7; i += 7) {
//				control[i] = 1.0f;
//				control[i+1] = 1.0f;
//				control[i+2] = 1.0f;
//				control[i+3] = 0.1f;
//				control[i+4] = 1.0f;
//				control[i+5] = 0.7f;
//				control[i+6] = 0.1f;
//			}
//
//			//Testing IO
//			SwimmerOptimization.writeToCSV(control, "./controlData", "blah");
//			control = SwimmerOptimization.readFromCSV("./controlData", "blah");
//			
//			character.setControlParams(control);
//		}
		
		//Apply optimized control strategy
		if (scenario.getCharacters().size() > 0) {
			mainChar = scenario.getCharacters().get(0);
			mainChar.setControlParams(SwimmerOptimization.readFromCSV("./controlData", charIDs.get(0)));
		}
	}
	
	@Override
	public void step(SwimSettings settings) {
		super.step(settings);

		//Add debug info
		if (mainChar != null) {
			addTextLine("Keys: (f) toggle friction, (m) toggle motor");
			float swimSpeed = mainChar.getRootBody().getLinearVelocity().x;
			float torque = mainChar.getMotors().get(0).getPrevTorque();
			Formatter f = new Formatter();
			addTextLine(f.format("Swim Speed: %f, Motor Force = %f, ", swimSpeed, torque).toString());
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
		return "Basic Swim Test";
	}

}
