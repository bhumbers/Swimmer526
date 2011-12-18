package ubc.swim.optimization;

import java.util.ArrayList;
import java.util.List;

import org.jbox2d.dynamics.Body;

import ubc.swim.gui.SwimSettings;
import ubc.swim.world.characters.SwimCharacter;
import ubc.swim.world.motors.TorqueMotor;
import ubc.swim.world.scenario.Scenario;
import ubc.swim.world.scenario.ScenarioLibrary;

/**
 * Basic function for scoring fitness of virtual swimmer control strategies
 * 
 * @author Ben Humberston
 *
 */
public class SwimFitnessFunctionA extends SwimFitnessFunction {
	
	protected String charID;
	protected Scenario scenario;
	
	/**
	 * Creates a new fitness function that will use a character
	 * with specified identifier
	 * @param charName
	 */
	public SwimFitnessFunctionA(String charID) {
		this.charID = charID;
	}
	
	@Override
	public int getNumControlDimensions() {
		//Not the most efficient solution, but oh well...
		SwimCharacter character = ScenarioLibrary.getCharacterByID(charID);
		return character.getNumControlDimensions();
	}
	
	/**
	 * Returns numeric fitness score for given control strategy when used
	 * by swimmer character.
	 */
	@Override
	public double valueOf(double[] x) {
		//For now, just recreate complete scenario (don't try to reset)
		List<String> charIDs = new ArrayList<String>();
		charIDs.add(charID);
		scenario = ScenarioLibrary.getBasicScenario(charIDs);
		
		SwimCharacter character = scenario.getCharacters().get(0); 
		character.setControlParams(x);
		Body rootBody = character.getRootBody();
		float rootAngleOrig = rootBody.getAngle();
		
		float evaluation = 0.0f;
		float maxRuntime = 5; //5 seconds
		float time = 0.0f;
		
		SwimSettings settings = new SwimSettings();
		float hz = (float)settings.getSetting(SwimSettings.Hz).value;
		float dt = hz > 0f ? 1f / hz : 0;
		
		float targetSpeed = 1.0f; // meters/second
		
		float accDistanceError;
		
		while (time < maxRuntime) {
			
			scenario.step(settings, dt);
			
			final float GOAL_SPEED = 2.0f;
			
			final float SPEED_TERM_WEIGHT = 1000.0f;
			final float ENERGY_TERM_WEIGHT = 0.0f;
			final float ROOT_ANGLE_TERM_WEIGHT = 0.0f;
			
			//Minimize distance from target speed
//			float targetDistanceAtTime = targetSpeed * time;
//			float targetDistanceError = Math.abs(targetDistanceAtTime - rootBody.getPosition().x);
//			evaluation += SPEED_TERM_WEIGHT * targetDistanceError;
			evaluation += SPEED_TERM_WEIGHT * Math.abs(GOAL_SPEED - rootBody.getLinearVelocity().x);
			
			//Minimize total applied torques
			float torquesApplied = 0.0f;
//			for (TorqueMotor motor : character.getMotors())
//				torquesApplied += (float)Math.abs(motor.getPrevTorque());
			torquesApplied = character.getPrevTorque();
			evaluation += ENERGY_TERM_WEIGHT * torquesApplied;
			
			//Minimize root angle rotation outside some threshold value
			float rootAngleDeviation = (float)Math.abs(rootBody.getAngle() - rootAngleOrig);
			float rootAngleDevThreshold = (float)Math.PI * 0.2f;
			if (rootAngleDeviation > rootAngleDevThreshold)
				evaluation += ROOT_ANGLE_TERM_WEIGHT * rootAngleDeviation;
			
			//TODO: update score based on:
			// Minimize energy to velocity ratio
			// minimize distance from target velocity?
			
			time += dt;
		}
		
//		evaluation += Math.abs((maxRuntime * 0.3f) - rootBody.getPosition().x);
		
		return evaluation;
	}

	/**
	 * Returns true if given control strategy is allowed
	 */
	@Override
	public boolean isFeasible(double[] x) {
		return true;
	}

}
