package ubc.swim.world.scenario;

import java.util.ArrayList;
import java.util.List;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;

import ubc.swim.dynamics.controllers.FluidControllerDef;
import ubc.swim.dynamics.controllers.DynamicsController;
import ubc.swim.gui.SwimSettings;
import ubc.swim.world.characters.SwimCharacter;

/** A world scenario for the swimmer app
 * 
 * @author Ben Humberston
 *
 */
public class Scenario {
	/**
	 * List of characters in scenario
	 */
	protected ArrayList<SwimCharacter> characters;
	
	//Controllers which affect dynamics in this test
	protected ArrayList<DynamicsController> dynControllers;
	
	protected World world;
	protected float runtime = 0.0f;
	
	/** 
	 * Constructor
	 */
	public Scenario() {
		characters = new ArrayList<SwimCharacter>();
		dynControllers = new ArrayList<DynamicsController>();
	}
	
	/**
	 * Creates the world used in this scenario
	 */
	public void initialize() {
		dynControllers.clear();
		
		createWorld();
		
		runtime = 0.0f;
//		reset();
	}
	
	//TODO: implement reset to allow object reuse?
//	/**
//	 * Resets this scenario to its original configuration
//	 */
//	public void reset() {
//		runtime = 0.0f;
//		
//		//Reset controllers
//		for (DynamicsController controller : dynControllers)
//			controller.clear();
//		
//		//TODO: reset characters
//		//requires removing bodies & joints from world, recreating?
//	}
	
	public World getWorld() {return world;}
	public List<DynamicsController> getDynamicsControllers() {return dynControllers;}
	public List<SwimCharacter> getCharacters() {return characters;}
	
	protected void createWorld() {
		Vec2 gravity = new Vec2(0, -10f);
		world = new World(gravity, true);
		
		//Create fluid environment
		float fluidHeight = 10.0f;
		FluidControllerDef fluidDef = new FluidControllerDef();
		fluidDef.density = 5.0f;
		fluidDef.offset = fluidHeight;
		fluidDef.linearDrag = 100.0f;
		fluidDef.useDensity = true;
		DynamicsController fluid = fluidDef.create();
		fluid.world = world;
			
		dynControllers.add(fluid);
	}
	
	/**
	 * Adds given character to this scenario's controllers
	 * @param character
	 */
	public void addCharacter(SwimCharacter character) {
		characters.add(character);
		
		//Put character segments under fluid control
		for (Body charBody : character.getBodies())
			addBody(charBody);
	}
	
	/**
	 * Adds given body to this scenario's controllers
	 * @param body
	 */
	public void addBody(Body body) {
		for (DynamicsController controller : dynControllers)
			controller.addBody(body);
	}
	
	public void step(SwimSettings settings, float dt) {		
		//Apply dynamic controllers
		for (DynamicsController controller : dynControllers)
			controller.step(settings);
		
		//Update characters
		for (SwimCharacter character : characters) {
			character.step(settings, dt);
			character.updateStats(dt);
		}

		world.step(dt,
				settings.getSetting(SwimSettings.VelocityIterations).getIntValue(),
				settings.getSetting(SwimSettings.PositionIterations).getIntValue());
		
		runtime += dt;
	}
}
