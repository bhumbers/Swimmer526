package ubc.swim.optimization;

import java.util.ArrayList;
import java.util.List;

import org.jbox2d.dynamics.Body;

import ubc.swim.gui.SwimSettings;
import ubc.swim.world.SwimCharacter;
import ubc.swim.world.scenario.Scenario;
import ubc.swim.world.scenario.ScenarioLibrary;

/**
 * Basic function for scoring fitness of virtual swimmer control strategies
 * 
 * @author Ben Humberston
 *
 */
public class SwimFitnessFunctionA extends SwimFitnessFunction {
	
	protected String charName;
	protected Scenario scenario;
	
	/**
	 * Creates a new fitness function that will use a character
	 * with specified identifier
	 * @param charName
	 */
	public SwimFitnessFunctionA(String charName) {
		this.charName = charName;
	}
	
	/**
	 * Returns numeric fitness score for given control strategy when used
	 * by swimmer character.
	 */
	@Override
	public double valueOf(double[] x) {
		//For now, just recreate complete scenario (don't try to reset)
		List<String> charIDs = new ArrayList<String>();
		charIDs.add(charName);
		scenario = ScenarioLibrary.getBasicScenario(charIDs);
		
		SwimCharacter character = scenario.getCharacters().get(0); 
		character.setControlParams(x);
		Body rootBody = character.getRootBody();
		
		float evaluation = 0.0f;
		float maxRuntime = 5; //5 seconds
		float time = 0.0f;
		
		SwimSettings settings = new SwimSettings();
		float hz = (float)settings.getSetting(SwimSettings.Hz).value;
		float dt = hz > 0f ? 1f / hz : 0;
		
		while (time < maxRuntime) {
			float prevRootAng = rootBody.getAngle();
			
			scenario.step(settings, dt);
			
			//TODO: REMOVE. testing optimization
			evaluation += 0.1f * (float)Math.abs(rootBody.getAngle() - prevRootAng);
			
			//TODO: update score based on:
			// Minimize energy to velocity ratio
			// minimize distance from target velocity?
			// Minimize rotation of root body?
			// minimize deviation from crawl/fly phase lock?
			
			time += dt;
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
