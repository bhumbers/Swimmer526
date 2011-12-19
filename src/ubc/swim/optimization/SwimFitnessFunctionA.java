package ubc.swim.optimization;

import java.util.ArrayList;
import java.util.List;

import org.jbox2d.dynamics.Body;

import ubc.swim.gui.SwimSettings;
import ubc.swim.world.characters.SwimCharacter;
import ubc.swim.world.scenario.Scenario;
import ubc.swim.world.scenario.ScenarioLibrary;

/**
 * Basic function for scoring fitness of virtual swimmer control strategies
 * 
 * @author Ben Humberston
 *
 */
public class SwimFitnessFunctionA extends SwimFitnessFunction {
	
	protected static final float TWO_PI = (float)(2 * Math.PI);
	
	protected String charID;
	protected Scenario scenario;
	
	protected float goalSpeed = 0.2f;
	protected float goalDisplacement = 0.0f;
	protected float maxRuntime = 5.0f; //5 seconds
	
	//Weight terms (assigned default vals)
	protected float speedTermWeight = 10.0f;
	protected float displacementTermWeight = 0.0f; //off by default; some tests use this rather than speed
	protected float energyTermWeight = 1.0f;
	protected float rootAngleTermWeight = 1.0f;
	
	/**
	 * Creates a new fitness function that will use a character
	 * with specified identifier
	 * @param charName
	 */
	public SwimFitnessFunctionA(String charID) {
		this.charID = charID;
	}
	
	/** Set target horizontal speed that is used as swimmer's goal */
	public void setGoalSpeed(float val) {this.goalSpeed = val;}
	/** Set target horizontal displacement that is used as swimmer's goal */
	public void setGoalDisplacement(float val) {this.goalDisplacement = val;}
	/** Set how long simulation is run in order to gather cost data */
	public void setMaxRuntime(float val) {this.maxRuntime = val;}
	
	/** Sets the weight assigned to the horizontal speed cost term */
	public void setSpeedTermWeight(float val) {this.speedTermWeight = val;}
	/** Sets the weight assigned to the horizontal displacement error cost term */
	public void setDisplacementErrorTermWeight(float val) {this.displacementTermWeight = val;}
	/** Sets the weight assigned to the total energy usage cost term */
	public void setEnergyTermWeight(float val) {this.energyTermWeight = val;}
	/** Sets the weight assigned to the cost term for deviation of the character root body from its original angle*/
	public void setRootAngleTermWeight(float val) {this.rootAngleTermWeight = val;}
	
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
		
		character = scenario.getCharacters().get(0); 
		character.setControlParams(x);
		Body rootBody = character.getRootBody();
		float rootAngleOrig = rootBody.getAngle();
		
		float evaluation = 0.0f;
		float time = 0.0f;
		
		//TODO: run for 5 seconds, assign bad score if no motion; 
		//otherwise, run 5 more seconds and score based on that? May help long term stroke stability
		
		SwimSettings settings = new SwimSettings();
		float hz = (float)settings.getSetting(SwimSettings.Hz).value;
		float dt = hz > 0f ? 1f / hz : 0;
		
		while (time < maxRuntime) {
			//Do a single simulation step
			scenario.step(settings, dt);
			
			//Then, update cost evaluation so far
			
			//Minimize distance from target speed
			if (speedTermWeight != 0) {
				float speedError = Math.abs(rootBody.getLinearVelocity().x - goalSpeed);
				evaluation += speedTermWeight * speedError;
			}
			
			//Minimize total applied torques
			if (energyTermWeight != 0) {
				float torquesApplied = Math.abs(character.getPrevTorque());
				evaluation += energyTermWeight * torquesApplied;
			}
			
			//Minimize root angle rotation outside some threshold value
			if (rootAngleTermWeight != 0) {
				float rootAngleDeviation = (float)Math.abs((rootBody.getAngle() % TWO_PI) - rootAngleOrig);
				if (rootAngleDeviation > SwimCharacter.ROOT_BODY_ANGLE_DEVIATION_THRESHOLD)
					evaluation += rootAngleTermWeight * rootAngleDeviation;
			}
			
			time += dt;
		}
		
		//Alternative locomotion measure... 
		//Find how far off final goal displacement the character ended up
		if (displacementTermWeight != 0) {
			float goalDispError = goalDisplacement - rootBody.getPosition().x;
			evaluation += displacementTermWeight * Math.abs(goalDispError);
		}
		
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
