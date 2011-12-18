package ubc.swim.world.trajectory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * A reference trajectory that uses polynomial basis function
 * @author Ben Humberston
 *
 */
public class PolynomialTrajectory extends RefTrajectory {
	protected static final float MIN_PERIOD = 0.00001f;
	
	/** Struct to hold coefficient and polynomial exponent term*/
	protected class Term {
		public float coefficient;
		public int exponent;
	}
	
	/** Sorts terms by their exponent */
	public class TermComparator implements Comparator<Term> {
	    @Override
	    public int compare(Term o1, Term o2) {
	        return o1.exponent - o2.exponent;
	    }
	}
	
	/** Coefficients, in order from  */
	protected ArrayList<Term> terms;
	
	/**
	 * Initializes with given number of basis funcs
	 * @param numFuncs
	 */
	public PolynomialTrajectory() {
		terms = new ArrayList<Term>();
	}
	
	/**
	 * Sets coefficient for the term with given exponent. If the term already 
	 * has a non-zero coefficient set, replaces that coeffient. Otherwise, adds new term
	 * to this trajectory's polynomial function.
	 */
	public void setTermCoefficient(int exponent, float coefficient)
	{
		//Search for existing term with same power
		for (Term term : terms) {
			if (term.exponent == exponent){
				term.coefficient = coefficient;
				return;
			}
		}
		
		//Otherwise, add new term
		Term term = new Term();
		term.exponent = exponent;
		term.coefficient = coefficient;
		terms.add(term);
		
		Collections.sort(terms, new TermComparator());
	}
	
	@Override
	public float getValue(float time) {
		float val = 0.0f;
		//Add contribution from each term
		for (int i = 0; i < terms.size(); i++) {
			Term term = terms.get(i);
			
			val += term.coefficient * (float)Math.pow(time, term.exponent);
		}
		
		return val;
	}
}
