package ubc.swim.world.scenario;

import java.util.List;

import ubc.swim.world.characters.RefTrajHumanChar;
import ubc.swim.world.characters.Stroke;
import ubc.swim.world.characters.TadpoleCharacter;
import ubc.swim.world.characters.HumanChar;
import ubc.swim.world.characters.PaddleChar;
import ubc.swim.world.characters.SwimCharacter;

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
		
		final float REF_TRAJ_CRAWL_STROKE_PERIOD = 1.0f; //seconds
		final float REF_TRAJ_FLY_STROKE_PERIOD   = 1.0f; //seconds
		
		if (charID.equals("paddle")) 
			character = new PaddleChar();
		if (charID.equals("tadpole")) 
			character = new TadpoleCharacter(4);
		if (charID.equals("humanCrawl")) 
			character = new HumanChar(Stroke.CRAWL);
		if (charID.equals("humanFly")) 
			character = new HumanChar(Stroke.FLY);
		if (charID.equals("humanCrawlRefTraj")) 
			character = new RefTrajHumanChar(Stroke.CRAWL, REF_TRAJ_CRAWL_STROKE_PERIOD);
		if (charID.equals("humanFlyRefTraj")) 
			character = new RefTrajHumanChar(Stroke.FLY, REF_TRAJ_FLY_STROKE_PERIOD);
		
		return character;
	}
}
