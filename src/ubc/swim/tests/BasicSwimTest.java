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
import java.util.List;

import org.jbox2d.common.Color3f;

import ubc.swim.gui.SwimSettings;
import ubc.swim.optimization.SwimmerOptimization;
import ubc.swim.world.characters.SwimCharacter;

/**
 * Basic swimming test environment
 * 
 * Adapted from SliderCrankTest.java by Daniel Murphy
 * @author Ben Humberston
 *
 */
public class BasicSwimTest extends SwimTest {
	
	private SwimCharacter mainChar;
	private String ID = "basic_test";
	
	/**
	 * Creates a new swim test with chars specified by given IDs.
	 * Suffix list is used to load control strategy variants for the corresponding char in the charID list
	 * NOTE: charIDs and suffixes lists MUST be same length; sorry, but no time to implement handling for other cases
	 * @param ID A technical identifier used by this test; used to identify output images
	 * @param charIDs
	 * @param suffixes
	 */
	public BasicSwimTest(String ID, List<String> charIDs, List<String> suffixes) {
		super();
		
		this.ID = ID;
		
		for (int i = 0; i < charIDs.size(); i++) {
			this.charIDs.add(charIDs.get(i));
			this.suffixes.add(suffixes.get(i));
		}
	}
	
	@Override
	public void initTest() {		
//		Body ground = null;
//		{
//			BodyDef bd = new BodyDef();
//			ground = getWorld().createBody(bd);
//
//			PolygonShape shape = new PolygonShape();
//			shape.setAsEdge(new Vec2(-40.0f, 0.0f), new Vec2(40.0f, 0.0f));
//			ground.createFixture(shape, 0.0f);
//		}
		
		List<SwimCharacter> characters = scenario.getCharacters();
		
		//Try to assign different debug colors to each character 
		Color3f[] debugColors = new Color3f[]{
				new Color3f(1.0f, 0.0f, 0.0f),
				new Color3f(0.0f, 1.0f, 0.0f),
				new Color3f(0.0f, 0.0f, 1.0f)
		}; 
		for (int i = 0; i < characters.size(); i++) {
			Color3f debugColor = debugColors[i % debugColors.length];
			characters.get(i).setDebugColor(debugColor);
		}
		
		//Apply optimized control strategy
		for (int i = 0; i < characters.size(); i++) {
			SwimCharacter character = characters.get(i);
			String completeCharID = charIDs.get(i);
			String suffix = suffixes.get(i);
			if (suffix.length() > 0) completeCharID += "_" + suffix;
			character.setControlParams(SwimmerOptimization.readFromCSV("./controlData", completeCharID));
		}
	}
	
	@Override
	public void step(SwimSettings settings) {
		super.step(settings);

		//Add debug info
		if (mainChar != null) {
			float rootDisp = mainChar.getRootBody().getPosition().x;
			float swimSpeed = mainChar.getRootBody().getLinearVelocity().x;
			Formatter f = new Formatter();
			float torque = 0.0f; //TODO: hook up to a motor value?
			addTextLine(f.format("Horizontal Displacement: %f, Swim Speed: %f, Motor Force = %f, ", rootDisp, swimSpeed, torque).toString());
		}
	}

	@Override
	public void keyPressed(char argKeyChar, int argKeyCode) {
		//nothing for now
	}

	@Override
	public String getTestName() {
		return "Basic Swim Test: " + ID;
	}
	
	@Override
	public String getTestID() {
		return ID;
	}

}
