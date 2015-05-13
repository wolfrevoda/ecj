package ec.gnp;
import ec.*;
import ec.util.*;

import java.io.*;

/**
 * GNPIndividual is an Individual used for GNP evolution runs.
 * GNPIndividuals contain, at the very least, a nonempty array of GNPGraphs.
 * You can use GNPIndividual directly, or subclass it to extend it as
 * you see fit.
 * 
 * @author Batu
 * version 1.0
 *
 */

public class GNPIndividual extends Individual
{
	private static final long serialVersionUID = 1;
	
	public static final String P_NUMGRAPHS = "numgraphs";
	public static final String P_GRAPH = "graph";
	
	public GNPGraph[] graphs;
	
	public Parameter defalutBase()
	{
		return GNPDefaults.base().push(P_INDIVIDUAL);
	}
	
	public boolean equals(Object ind)
	{
		if(ind == null)
			return false;
		if(!(this.getClass().equals(ind.getClass())))
			return false;
		GNPIndividual i = (GNPIndividual)ind;
		if(graphs.length != i.graphs.length)
			return false;
		for(int x = 0; x < graphs.length;++x)
		{
			if(!graphs[x].graphEquals(i.graphs[x]))
				return false;
		}
		return true;
	}
	
	public int hashCode()
	{
		int hash = this.getClass().hashCode();
		
		for(int x = 0; x < graphs.length; ++x)
			hash = (hash << 1 | hash >>> 31) ^ graphs[x].graphHashCode();
		
		return hash;
	}
	
    /** Sets up a prototypical GPIndividual with those features which it
    shares with other GPIndividuals in its species, and nothing more. */
	
	public void setup(final EvolutionState state, final Parameter base)
	{
		super.setup(state, base);
		
		Parameter def = defalutBase();
		
		evaluated = false;
		
		//how many graphs?
		int t = state.parameters.getInt(base.push(P_NUMGRAPHS), def.push(P_NUMGRAPHS), 1);
		if(t <= 0)
			state.output.fatal("A GPIndividual must have at least one tree.", base.push(P_NUMGRAPHS), def.push(P_NUMGRAPHS));
		
		//load the graphs
		graphs = new GNPGraph[t];
		
		for(int x = 0; x < t; ++x)
		{
			Parameter p = base.push(P_GRAPH).push("" + x);
			graphs[x] = (GNPGraph)(state.parameters.getInstanceForParameterEq(p, def.push(P_GRAPH).push("" + x), GNPGraph.class));
			graphs[x].owner = this;
			graphs[x].setup(state, p);
		}
		
		// now that our function sets are all associated with trees,
        // give the nodes a chance to determine whether or not this is
        // going to work for them (especially the ADFs).
		GNPInitializer initializer = (GNPInitializer)state.initializer;
		for(int x = 0; x < t; ++x)
		{
			for(int w = 0; w < graphs[x].constraints(initializer).functionset.nodes.length; ++w)
			{
				GNPNode[] gnpfi = graphs[x].constraints(initializer).functionset.nodes[w];
				for(int y = 0; y < gnpfi.length; ++y)
					gnpfi[y].checkConstraints(state, x, this, base);
			}
		}
		
		state.output.exitIfErrors();
	}
	
	/** Verification of validity of the GPIndividual -- strictly for debugging purposes only */
	public void verify(EvolutionState state)
	{
		if(!(state.initializer instanceof GNPInitializer))
		{
			state.output.error("Initializer is not a GNPInitializer");
			return;
		}
		
		if(graphs == null)
		{
			state.output.error("Null graphs in GNPIndividual.");
			return;
		}
		for(int x = 0; x < graphs.length; ++x)
		{
			if(graphs[x] == null)
			{
				state.output.error("Null graph (#"+x+") in GNPIndividual.");
				return;
			}
		}
		for(int x = 0; x < graphs.length; ++x)
			graphs[x].verify(state);
		state.output.exitIfErrors();
	}
	
	
	public void printIndividual(final EvolutionState state, final int log)
	{
		state.output.println(EVALUATED_PREAMBLE + Code.encode(evaluated), log);
		fitness.printFitness(state, log);
		for(int x = 0; x < graphs.length; ++x)
		{
			state.output.println("Graph " + x + ":", log);
			graphs[x].printGraph(state, log);
		}
	}
	
	public void printIndividual(final EvolutionState state, final PrintWriter writer)
	{
		writer.println(EVALUATED_PREAMBLE + Code.encode(evaluated));
		fitness.printFitness(state, writer);
		for(int x = 0; x < graphs.length; ++x)
		{
			writer.println("Graph " + x + ":");
			graphs[x].printGraph(state, writer);
		}
	}
	
	public Object clone()
	{
        // a deep clone
        
        GNPIndividual myobj = (GNPIndividual)(super.clone());

        // copy the tree array
        myobj.graphs = new GNPGraph[graphs.length];
        for(int x=0;x<graphs.length;x++)
        {
            myobj.graphs[x] = (GNPGraph)(graphs[x].clone());  // force a deep clone
            myobj.graphs[x].owner = myobj;  // reset owner away from me
        }
        return myobj;
	}
	
	public GNPIndividual lightClone()
	{
		// a light clone
        GNPIndividual myobj = (GNPIndividual)(super.clone());
        
        // copy the graph array
        myobj.graphs = new GNPGraph[graphs.length];
        for(int x=0;x<graphs.length;x++)
        {
            myobj.graphs[x] = (GNPGraph)(graphs[x].lightClone());  // note light-cloned!
            myobj.graphs[x].owner = myobj;  // reset owner away from me
        }
        return myobj;
	}
	
    /** Returns the "size" of the individual, namely, the number of nodes
    in all of its subtrees.  */
	public long size()
	{
		long size = 0; 
		for(int x = 0; x < graphs.length; ++x)
			size += graphs[x].child.numNodes(GNPNode.NODESEARCH_ALL);
		return size;
	}

	public Parameter defaultBase() 
	{
		return GNPDefaults.base().push(P_INDIVIDUAL);
	}
}
