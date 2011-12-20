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
 * Created at 5:34:33 PM Jul 17, 2010
 */
package ubc.swim.gui;

import java.util.Arrays;

import ubc.swim.tests.BasicSwimTest;
import ubc.swim.tests.PaddleTest;
import ubc.swim.tests.SwimTest;


/**
 * Hard-coded list of tests that appear in swimmer app dropdown.
 * Adapted from org.jbox2d.testbed.framework.TestList by Daniel Murphy
 * 
 * @author Ben Humberston
 */
public class SwimTestList {
  
  public static void populateModel(SwimModel argModel){
      
	  argModel.addCategory("Basic");
	  argModel.addTest(new BasicSwimTest("paddle", Arrays.asList("paddle"), Arrays.asList("")));
	  argModel.addTest(new BasicSwimTest("tadpole", Arrays.asList("tadpole"), Arrays.asList("")));
	  argModel.addTest(new BasicSwimTest("tadpole_lowIters", Arrays.asList("tadpole"), Arrays.asList("lowIters")));
	  argModel.addTest(new BasicSwimTest("tadpole_medIters", Arrays.asList("tadpole"), Arrays.asList("medIters")));
	  argModel.addTest(new BasicSwimTest("tadpole_highIters", Arrays.asList("tadpole"), Arrays.asList("highIters")));
      argModel.addTest(new BasicSwimTest("humanCrawlRefTraj", Arrays.asList("humanCrawlRefTraj"), Arrays.asList("")));
      argModel.addTest(new BasicSwimTest("humanFlyRefTraj", Arrays.asList("humanFlyRefTraj"), Arrays.asList("")));
      argModel.addTest(new BasicSwimTest("humanCrawl", Arrays.asList("humanCrawl"), Arrays.asList("")));
      argModel.addTest(new BasicSwimTest("humanFly", Arrays.asList("humanFly"), Arrays.asList("")));
      
      argModel.addCategory("Multicharacter");
      argModel.addTest(new BasicSwimTest("paddle_and_human", Arrays.asList("paddle", "humanFlyRefTraj"), Arrays.asList("", "")));
      
      SwimTest speedPaddleTest = new BasicSwimTest("paddle_speed", Arrays.asList("paddle", "paddle", "paddle"), Arrays.asList("speed1", "speed2", "speed3"));
      speedPaddleTest.setDefaultCameraScale(40);
      speedPaddleTest.setDefaultCameraPos(5, 10);
      argModel.addTest(speedPaddleTest);
      
      SwimTest energyPaddleTest = new BasicSwimTest("paddle_energy", Arrays.asList("paddle", "paddle", "paddle"), Arrays.asList("energy1", "energy2", "energy3"));
      energyPaddleTest.setDefaultCameraScale(40);
      energyPaddleTest.setDefaultCameraPos(5, 10);
      argModel.addTest(energyPaddleTest);
      
      SwimTest orientationPaddleTest = new BasicSwimTest("paddle_orientation", Arrays.asList("paddle", "paddle", "paddle"), Arrays.asList("orientation1", "orientation2", "orientation3"));
      orientationPaddleTest.setDefaultCameraScale(40);
      orientationPaddleTest.setDefaultCameraPos(5, 10);
      argModel.addTest(orientationPaddleTest);
      
      argModel.addCategory("Debugging");
      argModel.addTest(new PaddleTest());
  }
}
