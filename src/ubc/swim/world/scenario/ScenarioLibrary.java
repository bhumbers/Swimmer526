package ubc.swim.world.scenario;

import java.util.List;

import ubc.swim.world.characters.TadpoleCharacter;
import ubc.swim.world.characters.HumanChar;
import ubc.swim.world.characters.PaddleChar;
import ubc.swim.world.characters.SwimCharacter;
import ubc.swim.world.characters.HumanChar.Stroke;

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
			SwimCharacter character = getCharacterByID(charID);
			character.initialize(scenario.getWorld());
			character.moveTo(0, 10);
			
			scenario.addCharacter(character);
		}
		
		return scenario;
	}
	
	/**
	 * Returns an uninitialized swimmer character referenced by given ID
	 * @param charID
	 * @return
	 */
	public static SwimCharacter getCharacterByID(String charID) {
		SwimCharacter character = null;
		
		switch (charID) {
			case "humanCrawl":
				character = new HumanChar(Stroke.CRAWL);
				break;
			case "humanFly":
				character = new HumanChar(Stroke.FLY);
				break;
			case "paddle":
				character = new PaddleChar();
				break;
			case "paddleComplex":
				character = new TadpoleCharacter(3);
				break;
			default:
				break;
		}
		
		return character;
	}
}
