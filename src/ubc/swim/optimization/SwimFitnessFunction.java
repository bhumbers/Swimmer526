package ubc.swim.optimization;

import ubc.swim.world.characters.SwimCharacter;

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
	 * Returns swimmer character used on last valueOf simulation
	 */
	public ubc.swim.world.characters.SwimCharacter getCharacter() { return character;}
	
	/**
	 * Returns dimensionality of control used by character in this fitness function
	 * @return
	 */
	public abstract int getNumControlDimensions();

}
