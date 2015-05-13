package ec.gnp.batu;
import ec.*;
import ec.gnp.*;
import ec.util.*;

public abstract class BatuBuilder extends GNPNodeBuilder{
	 
	public static final String P_MAXDEPTH = "max-depth";
	public static final String P_MINDEPTH = "min-depth";

	/** The largest maximum tree depth RAMPED HALF-AND-HALF can specify. */
	public int maxDepth;

	/** The smallest maximum tree depth RAMPED HALF-AND-HALF can specify. */
	public int minDepth;
	
	public void setup(final EvolutionState state, final Parameter base)
	{
		super.setup(state, base);
		
		Parameter def = defaultBase();

        // load maxdepth and mindepth, check that maxdepth>0, mindepth>0, maxdepth>=mindepth
        maxDepth = state.parameters.getInt(base.push(P_MAXDEPTH),def.push(P_MAXDEPTH),1);
        if (maxDepth<=0)
            state.output.fatal("The Max Depth for a KozaBuilder must be at least 1.",
                base.push(P_MAXDEPTH),def.push(P_MAXDEPTH));
                        
        minDepth = state.parameters.getInt(base.push(P_MINDEPTH),def.push(P_MINDEPTH),1);
        if (minDepth<=0)
            state.output.fatal("The Min Depth for a KozaBuilder must be at least 1.",
                base.push(P_MINDEPTH),def.push(P_MINDEPTH));

        if (maxDepth<minDepth)
            state.output.fatal("Max Depth must be >= Min Depth for a KozaBuilder",
                base.push(P_MAXDEPTH),def.push(P_MAXDEPTH));
	}
	
	/**
	 * Use this method to generate some subgraph which level is 1 and 2
	 * @param state
	 * @param current
	 * @param max
	 * @param type
	 * @param thread
	 * @param parent
	 * @param argposition
	 * @param set
	 * @return
	 */
	protected GNPNode growSubNode(final EvolutionState state, final int current, final int max, final GNPType type, final int thread,
								final GNPNodeParent parent, final int argposition, final GNPFunctionSet set)
	{
		boolean triedTerminals = false;
		
		int t = type.type;
		GNPNode[] terminals = set.terminals[t];
		GNPNode[] nodes = set.nodes[t];
		
		if (nodes.length == 0)
            errorAboutNoNodeWithType(type, state);
		
		if((current + 1 >= max) && terminals.length != 0)
		{
			GNPNode n = (GNPNode)(terminals[state.random[thread].nextInt(terminals.length)].lightClone());
			n.resetNode(state, thread);
			n.argposition[0] = (byte)argposition;
			n.parent[0] = parent;
			return n;
		}
		else
		{
			if(triedTerminals)
				warnAboutNoTerminalWithType(type, false, state);
			GNPNode n = (GNPNode)(nodes[state.random[thread].nextInt(nodes.length)].lightClone());
			n.resetNode(state, thread);
			n.argposition[0] = (byte)argposition;
			n.parent[0] = parent;
			
			GNPType[] childtypes = n.constraints(((GNPInitializer)state.initializer)).childtypes;
			for(int i = 0; i < childtypes.length; ++i)
				n.children[i] = growSubNode(state, current + 1, max, childtypes[i], thread, n, i, set);
			
			return n;
		}
	}
	
	protected GNPNode growSubNode(final EvolutionState state, final int current, final int max, final GNPType type, final int thread,
								final GNPNodeParent parent, final int argposition, final GNPFunctionSet set, final GNPNode[] subNodes)
	{
		boolean triedNonTerminals = false;
		boolean hasMaxDepth = false;
		
		int t = type.type;
		GNPNode[] nonterminals = set.nonterminals[t];
		
		if(triedNonTerminals)
			warnAboutNoTerminalWithType(type, false, state);
		GNPNode n = (GNPNode)(nonterminals[state.random[thread].nextInt(nonterminals.length)].lightClone());
		n.resetNode(state, thread);
		n.argposition[0] = (byte)argposition;
		n.parent[0] = parent;
		
		GNPType[] childtypes = n.constraints(((GNPInitializer)state.initializer)).childtypes;
		if(childtypes.length == 1)
		{
			n.children[0] = (GNPNode)(subNodes[state.random[thread].nextInt(subNodes.length)].lightClone());
			while(n.children[0].depth() < max - 1)
				n.children[0] = (GNPNode)(subNodes[state.random[thread].nextInt(subNodes.length)].lightClone());
		}
		else
		{
			for(int i = 0; i < childtypes.length; ++i)
			{
				n.children[i] = (GNPNode)(subNodes[state.random[thread].nextInt(subNodes.length)].lightClone());
				if(n.children[i].depth() == max - 1)
					hasMaxDepth = true;
				if(i == childtypes.length - 1)
				{
					n.children[i] = (GNPNode)(subNodes[state.random[thread].nextInt(subNodes.length)].lightClone());
					for(int j = 0; j < i; ++j)
					{
						
					}
					if(!hasMaxDepth)
					{
						while(n.children[i].depth() < max - 1)
							n.children[i] = (GNPNode)(subNodes[state.random[thread].nextInt(subNodes.length)].lightClone());
					}
				}
			}
		}
		
		return n;
	}

}
