package ec.gnp.batu;
import ec.util.*;
import ec.*;

import java.io.*;

public class BatuFitness extends Fitness
{
	public static final String P_BATUFITNESS = "fitness";

    /** This ranges from 0 (best) to infinity (worst).    I
        define it here as equivalent to the standardized fitness. */
    protected double standardizedFitness;

    /** This auxillary measure is used in some problems for additional
        information.  It's a traditional feature of Koza-style GP, and so
        although I think it's not very useful, I'll leave it in anyway. */
    public int hits;
    
    public Parameter defaultBase()
    {
    	return GNPBatuDefaults.base().push(P_BATUFITNESS);
    }
    
    /** Set the standardized fitness in the half-open interval [0.0,infinity)
    which is defined (NOTE: DIFFERENT FROM fitness()!!!) as 0.0 
    being the IDEAL and infinity being worse than the worst possible.
    This is the GP tradition.  The fitness() function instead will output
    the equivalent of Adjusted Fitness.
*/
    public void setStandardizedFitness(final EvolutionState state, final double _f)
    {
	    if (_f < 0.0 || _f >= Double.POSITIVE_INFINITY || Double.isNaN(_f))
	        {
	        state.output.warning("Bad fitness (may not be < 0, NaN, or infinity): " + _f  + ", setting to 0.");
	        standardizedFitness = 0;
	        }
	    else standardizedFitness = _f;
    }
    
    /** Returns the adjusted fitness metric, which recasts the
    fitness to the half-open interval (0,1], where 1 is ideal and
    0 is worst.  Same as adjustedFitness().  */

    public double fitness()
    {
    	return 1.0/(1.0 + standardizedFitness);     
    }
    
    /** Returns the standardized fitness metric. */

    public double standardizedFitness()
    {
        return standardizedFitness;
    }

    /** Returns the adjusted fitness metric, which recasts the fitness
        to the half-open interval (0,1], where 1 is ideal and 0 is worst.
        This metric is used when printing the fitness out. */

    public double adjustedFitness()
    {
        return fitness();
    }

    public void setup(final EvolutionState state, final Parameter base) { }
    
    public boolean isIdealFitness()
    {
        return standardizedFitness <= 0.0;  // should always be == 0.0, <0.0 is illegal, but just in case...
    }
    
    public boolean equivalentTo(final Fitness _fitness)
    {
    	return ((BatuFitness)_fitness).standardizedFitness() == standardizedFitness;
    }
    
    public boolean betterThan(final Fitness _fitness)
    {
    	return ((BatuFitness)_fitness).standardizedFitness() > standardizedFitness;
    }
    
    public void setToMeanOf(EvolutionState state, Fitness[] fitnesses)
    {
    	double f = 0;
    	long h = 0;
    	for(int i = 0; i < fitnesses.length; ++i)
    	{
    		BatuFitness fit = (BatuFitness)(fitnesses[i]);
    		f += fit.standardizedFitness;
    		h += fit.hits;
    	}
    	f /= fitnesses.length;
    	h /= fitnesses.length;
    	standardizedFitness = (double)f;
    	hits = (int)h;
    }

    
    

}
