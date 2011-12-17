package ubc.swim.world.scenario;

import java.util.HashMap;
import java.util.List;

import ubc.swim.world.HumanChar;
import ubc.swim.world.SwimCharacter;
import ubc.swim.world.HumanChar.Stroke;

/**
 * A library of common scenarios, characters, etc.
 * @author Ben Humberston
 *
 */
public class ScenarioLibrary {
	
	/**
	 * Returns scenario with a simple fluid environment and chars
	 * with given IDs added to the scene
	 * @param c
	 * @return
	 */
	public static Scenario getBasicScenario(List<String> charIDs) {
		Scenario scenario = new Scenario();
		scenario.initialize();
		
		//Add list of characters to the scene
		for (String charID : charIDs) {
			SwimCharacter character = null;
			
			switch (charID) {
				case "humanCrawl":
					character = new HumanChar(Stroke.CRAWL);
					character.initialize(scenario.getWorld());
					character.moveTo(0, 10);
					break;
				default:
					break;
			}
			
			scenario.addCharacter(character);
		}
		
		return scenario;
	}
}
