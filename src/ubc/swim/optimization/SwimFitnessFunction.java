package ubc.swim.optimization;

import org.jbox2d.dynamics.World;

import ubc.swim.world.SwimCharacter;

import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;

/**
 * Abstract swimmer fitness function; provides interface common to all 
 * swimmer fitness functions.
 * @author Ben Humberston
 *
 */
public abstract class SwimFitnessFunction implements IObjectiveFunction {
	protected SwimCharacter character;
	
	@Override
	public abstract double valueOf(double[] x);

	@Override
	public abstract boolean isFeasible(double[] x);
	
	/**
	 * Returns swimmer character used in this function
	 */
	public ubc.swim.world.SwimCharacter getCharacter() { return character;}

}
