package ec.gnp;
import ec.*;

public abstract class GNPBreedingPipeline extends BreedingPipeline
{
	/** Standard parameter for node-selectors associated with a GPBreedingPipeline */
    public static final String P_NODESELECTOR = "ns";
    
    /** Standard parameter for graph fixing */
    public static final String P_GRAPH = "graph";
    
    /** Standard value for an unfixed tree */
    public static final int GRAPH_UNFIXED = -1;
    
    /** Returns true if <i>s</i> is a GNPSpecies. */
    public boolean produces(final EvolutionState state, final Population newpop, final int subpopulation, final int thread)
    {
    	if(!super.produces(state, newpop, subpopulation, thread))
    		return false;
    	
    	// we produce individuals which are owned by subclasses of GNPSpecies
    	if(newpop.subpops[subpopulation].species instanceof GNPSpecies)
    		return true;
    	return false;
    }

}
