package ubc.swim.optimization;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;

import ubc.swim.dynamics.controllers.BuoyancyControllerDef;
import ubc.swim.dynamics.controllers.DynamicsController;
import ubc.swim.world.HumanChar;
import ubc.swim.world.HumanChar.Stroke;
import ubc.swim.world.scenario.Scenario;

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
		
	}
	
	/**
	 * Returns numeric fitness score for given control strategy when used
	 * by swimmer character.
	 */
	@Override
	public double valueOf(double[] x) {
		scenario.reset();
		
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Returns true if given control strategy is allowed
	 */
	@Override
	public boolean isFeasible(double[] x) {
		return true;
	}
	
	/**
	 * Creates physical sim world and character in initial test configuration
	 */
	protected void initializeScenario() {
		//TODO: select correct
		Scenario scenario = new Scenario();
	}

}
