package ec.gnp.batu;
import ec.*;
import ec.gnp.*;
import ec.util.*;

public class HalfBuilder extends BatuBuilder
{
	public static final String P_HALFBUILDER = "half";
    public static final String P_PICKGROWPROBABILITY = "growp";

    /** The likelihood of using GROW over FULL. */
    public double pickGrowProbability;
    
    public Parameter defaultBase()
    {
        return GNPBatuDefaults.base().push(P_HALFBUILDER); 
    }
    
    public void setup(final EvolutionState state, final Parameter base)
    {
    	super.setup(state, base);
    	
    	Parameter def = defaultBase();
    	
    	pickGrowProbability = state.parameters.getDoubleWithMax(
                base.push(P_PICKGROWPROBABILITY),
                def.push(P_PICKGROWPROBABILITY),0.0,1.0);
            if (pickGrowProbability < 0.0)
                state.output.fatal("The Pick-Grow Probability for HalfBuilder must be a double floating-point value between 0.0 and 1.0 inclusive.", base.push(P_MAXDEPTH),def.push(P_MAXDEPTH));
           
    }
    
    public GNPNode newRootedGraph(final EvolutionState state, final GNPType type, final int thread, final GNPNodeParent parent,
    							final GNPFunctionSet set, final int argposition, final int requestedSize, int count, Individual[] subIndividual)
    {
    	GNPNode[] subNodes  = new GNPNode[subIndividual.length];
    	
    	for(int i = 0; i < subIndividual.length; ++i)
    	{
    		GNPIndividual temp = (GNPIndividual)subIndividual[i];
    		subNodes[i] = (GNPNode)(temp.graphs[0].child.clone());
    	}
    	return count < 3 ? growSubNode(state, 0, state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set) : 
    					growSubNode(state, 0, state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set, subNodes);
    }
    
    public GNPNode newRootedGraph(final EvolutionState state,
            final GNPType type,
            final int thread,
            final GNPNodeParent parent,
            final GNPFunctionSet set,
            final int argposition,
            final int requestedSize)
            {
                return growSubNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set);
            }

}
