package ec.gnp;
import ec.*;
import ec.util.*;

/**
 * GNPNodeBuilder is a Prototype which defines the superclass for objects
 * which create ("grow") GNP graphs, whether for population initialization,
 * subgraph mutation, or whatnot.  It defines a single abstract method, 
 * newRootedgraph(...), which must be implemented to grow the graph.
 *
 * <p>GNPNodeBuilder also provides some facilities for user-specification
 * of probabilities of various graph sizes, which the graph builder can use
 * as it sees fit (or totally ignore).  
 * There are two such facilities.  First, the user might
 * specify a minimum and maximum range for graph sizes to be picked from;
 * graphs would likely be picked uniformly from this range.  Second, the
 * user might specify an array, <tt>num-sizes</tt> long, of probabilities of 
 * graph sizes, in order to give a precise probability distribution. 
 * 
 * @author Batu
 *
 */

public abstract class GNPNodeBuilder implements Prototype
{
	public static final int NOSIZEGIVEN = -1;
    public static final int CHECK_BOUNDARY = 8;
    public static final String P_MINSIZE = "min-size";
    public static final String P_MAXSIZE = "max-size";
    public static final String P_NUMSIZES = "num-sizes";
    public static final String P_SIZE = "size";
    
    public int minSize;  /** the minium possible size  -- if unused, it's 0 */
    public int maxSize;  /** the maximum possible size  -- if unused, it's 0 */
    public double[] sizeDistribution;  /* sizeDistribution[x] represents the likelihood of size x appearing -- if unused, it's null */
    
    /** Returns true if some size distribution (either minSize and maxSize,
    or sizeDistribution) is set up by the user in order to pick sizes randomly. */
    public boolean canPick()
    {
    	return (minSize != 0 || sizeDistribution != null);
    }
    
    /** Assuming that either minSize and maxSize, or sizeDistribution, is defined,
    picks a random size from minSize...maxSize inclusive, or randomly
    from sizeDistribution. */ 
    public int pickSize(final EvolutionState state, final int thread)
    {
    	if(minSize > 0)
    	{
    		//pick from nimSize...maxSize
    		return state.random[thread].nextInt(maxSize - minSize + 1) + minSize;
    	}
    	else if(sizeDistribution != null)
    	{
    		return RandomChoice.pickFromDistribution(sizeDistribution, state.random[thread].nextDouble()) + 1;
    	}
    	else
    		throw new InternalError("Neither minSize nor sizeDistribution is defined in GNPNodeBuilder");
    	
    }
    
    public Object clone()
    {
    	try
    	{
    		GNPNodeBuilder c = (GNPNodeBuilder)(super.clone());
    		
    		if(sizeDistribution != null)
    			c.sizeDistribution = (double[]) (sizeDistribution.clone());
    		return c;
    	}
    	catch (CloneNotSupportedException e)
    	{
    		throw new InternalError();
    	}
    }
    
    public void setup(final EvolutionState state, final Parameter base)
    {
    	Parameter def = defaultBase();
    	
    	// min and max size
    	
    	if(state.parameters.exists(base.push(P_MINSIZE),def.push(P_MINSIZE)))
    	{
    		if (!(state.parameters.exists(base.push(P_MAXSIZE), def.push(P_MAXSIZE))))
    			state.output.fatal("This GNPNodeBuilder has a " + P_MINSIZE + " but not a " + P_MAXSIZE + ".");
    		
    		minSize = state.parameters.getInt(base.push(P_MINSIZE), def.push(P_MINSIZE), 1);
    		if(minSize == 0)
    			state.output.fatal("The GNPNodeBuilder must have a min size >= 1.", base.push(P_MINSIZE), def.push(P_MINSIZE));
    		maxSize = state.parameters.getInt(base.push(P_MAXSIZE), def.push(P_MAXSIZE),1);
    		if (maxSize==0) 
                state.output.fatal("The GNPNodeBuilder must have a max size >= 1.", base.push(P_MAXSIZE), def.push(P_MAXSIZE));
    		if (minSize > maxSize)
                state.output.fatal("The GNPNodeBuilder must have min size <= max size.", base.push(P_MINSIZE), def.push(P_MINSIZE));
    		 
    	}
    	else if(state.parameters.exists(base.push(P_MAXSIZE), def.push(P_MAXSIZE)))
    		state.output.fatal("This GNPNodeBuilder has a " + P_MAXSIZE + " but not a " + P_MINSIZE + ".",base.push(P_MAXSIZE), def.push(P_MAXSIZE));
    	
    	// load sizeDistribution
    	
    	else if(state.parameters.exists(base.push(P_NUMSIZES), def.push(P_NUMSIZES)))
    	{
    		int size = state.parameters.getInt(base.push(P_NUMSIZES), def.push(P_NUMSIZES), 1);
    		if(size == 0)
    			state.output.fatal("The number of sizes in the GNPNodeBuilder's distribution must be >= 1. ");
    		sizeDistribution = new double[size];
    		if(state.parameters.exists(base.push(P_SIZE).push("0"), def.push(P_SIZE).push("0")))
    			state.output.warning("GNPNodeBuilder does not use size #0 in the distribution", base.push(P_SIZE).push("0"), def.push(P_SIZE).push("0"));
    		
    		double sum = 0.0;
    		for(int x = 0; x < size; ++x)
    		{
    			sizeDistribution[x] = state.parameters.getDouble(base.push(P_SIZE).push("" + (x+1)), def.push(P_SIZE).push("" + (x+1)), 0.0);
    			if(sizeDistribution[x] < 0.0)
    			{
    				state.output.warning( "Distribution value #" + x + " negative or not defined, assumed to be 0.0", base.push(P_SIZE).push(""+(x+1)), def.push(P_SIZE).push(""+(x+1)));
    				sizeDistribution[x] = 0.0;
    			}
    			sum += sizeDistribution[x];
    		}
    		if(sum > 1.0)
    			state.output.warning("Distribution sums to greater than 1.0", base.push(P_SIZE), def.push(P_SIZE));
    		if(sum == 0.0)
    			state.output.fatal("Distribution is all 0's", base.push(P_SIZE), def.push(P_SIZE));
    		
    		// normalize and prepare
    		RandomChoice.organizeDistribution(sizeDistribution);
    		
    	}
    }
    
    public abstract GNPNode newRootedGraph(final EvolutionState state, final GNPType type, final int thread, final GNPNodeParent parent, 
    									   final GNPFunctionSet set, final int arGNPosition, final int requestedSize);
    
    public abstract GNPNode newRootedGraph(final EvolutionState state, final GNPType type, final int thread, final GNPNodeParent parent, 
			   final GNPFunctionSet set, final int arGNPosition, final int requestedSize, int count, Individual[] subIndividual);
    
    /** Issues a warning that no terminal was found with a return type of the given type, and that an algorithm
    had requested one.  If fail is true, then a fatal is issued rather than a warning.  The warning takes
    the form of a one-time big explanatory message, followed by a one-time-per-type message. */
    protected void warnAboutNoTerminalWithType(GNPType type, boolean fail, EvolutionState state)
    {
    // big explanation -- appears only once
    state.output.warnOnce("A GNPNodeBuilder has been requested at least once to generate a one-node tree with " +
        "a return value type-compatable with a certain type; but there is no TERMINAL which is type-compatable " +
        "in this way.  As a result, the algorithm was forced to use a NON-TERMINAL, making the tree larger than " +
        "requested, and exposing more child slots to fill, which if not carefully considered, could " +
        "recursively repeat this problem and eventually fill all memory.");
            
    // shorter explanation -- appears for each node builder and type combo
    if (fail)
        state.output.fatal("" + this.getClass() + " can't find a terminal type-compatable with " + type + 
            " and cannot replace it with a nonterminal.  You may need to try a different node-builder algorithm.");
    else
        state.output.warnOnce("" + this.getClass() + " can't find a terminal type-compatable with " + type);
    }
    
    /** If the given test is true, issues a warning that no terminal was found with a return type of the given type, and that an algorithm
    had requested one.  If fail is true, then a fatal is issued rather than a warning.  The warning takes
    the form of a one-time big explanatory message, followed by a one-time-per-type message. Returns the value of the test.
    This form makes it easy to insert warnings into if-statements.  */
    protected boolean warnAboutNonterminal(boolean test, GNPType type, boolean fail, EvolutionState state)
    {
    	if (test) warnAboutNonTerminalWithType(type, fail, state);
    		return test;
    }
     
/** Issues a warning that no nonterminal was found with a return type of the given type, and that an algorithm
    had requested one.  If fail is true, then a fatal is issued rather than a warning.  The warning takes
    the form of a one-time big explanatory message, followed by a one-time-per-type message. */
    protected void warnAboutNonTerminalWithType(GNPType type, boolean fail, EvolutionState state)
    {
    // big explanation -- appears only once
    	state.output.warnOnce("A GNPNodeBuilder has been requested at least once to generate a tree with " +
        "a return value type-compatable with a certain type; but there is no NON-TERMINAL which is type-compatable " +
        "in this way.  As a result, the algorithm was forced to use a TERMINAL, making the tree smaller than " +
        "requested.");
            
    // shorter explanation -- appears for each node builder and type combo
    if (fail)
        state.output.fatal("" + this.getClass() + " can't find a non-terminal type-compatable with " + type + 
            " and cannot replace it with a terminal.  You may need to try a different node-builder algorithm.");
    else
        state.output.warnOnce("" + this.getClass() + " can't find a non-terminal type-compatable with " + type);
    }

    /** Issues a fatal error that no node (nonterminal or terminal) was found with a return type of the given type, and that an algorithm
    had requested one.  */
    protected void errorAboutNoNodeWithType(GNPType type, EvolutionState state)
    {
    	state.output.fatal("" + this.getClass() + " could find no terminal or nonterminal type-compatable with " + type);
    }
    
    
}
