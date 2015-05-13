package ec.gnp;
import ec.*;
import ec.util.*;

import java.io.*;

/**
 *  GNPGraph is a GNPNodeParent which holds the root GNPNode of a graph
 * of GNPNodes.  GNPGraphs typically fill out an array held in a GNPIndividual
 * (their "owner") and their roots are evaluated to evaluate a Genetic
 * programming graph.
 *
 * GNPGraphs also have <i>constraints</i>, which are shared, and define items
 * shared among several GNPGraphs.
 * 
 * @author Batu
 * version 1.0
 *
 */

public class GNPGraph implements GNPNodeParent, Prototype 
{
	public static final String P_GRAPH = "graph";
	public static final String P_GRAPHCONSTRAINTS = "tc";
	public static final String P_USEGRAPHVIZ = "graphviz";
    public static final String P_USELATEX = "latex";
    public static final String P_USEC = "c";
    public static final String P_USEOPS = "c-operators";
    public static final String P_USEVARS = "c-variables";
    public static final int NO_GRAPHNUM = -1;
    
    public static final String P_PRINT_STYLE = "print-style";
    public static final String V_LISP = "lisp";
    public static final String V_DOT = "dot";
    public static final String V_LATEX = "latex";
    public static final String V_C = "c";
    public static final int PRINT_STYLE_LISP = 0;
    public static final int PRINT_STYLE_DOT = 1;
    public static final int PRINT_STYLE_LATEX = 2;
    public static final int PRINT_STYLE_C = 3;
    
    /** the root GNPNode in the GNPGraph */
    public GNPNode child;
    
    /** the owner of the GNPGraph */
    public GNPIndividual owner;
    
    /** constraints on the GNPGraph  -- don't access the constraints through
    this variable -- use the constraints() method instead, which will give
    the actual constraints object. */
    public byte constraints;
    
    /** The print style of the GNPGraph. */
    public int printStyle;
    
    /** When using c to print for humans, do we print terminals as variables? 
    (as opposed to zero-argument functions)? */
    public boolean printTerminalsAsVariablesInC;

    /** When using c to print for humans, do we print two-argument nonterminals in operator form "a op b"? 
    (as opposed to functions "op(a, b)")? */
    public boolean printTwoArgumentNonterminalsAsOperatorsInC;
    
    public final GNPGraphConstraints constraints(final GNPInitializer initializer)
    {
    	return initializer.graphConstraits[constraints];
    }
    
    public Parameter defaultBase()
    {
    	return GNPDefaults.base().push(P_GRAPH);
    }
    
    /** Returns true if I am "genetically" the same as graph,
    though we may have different owners. */
    public boolean graphEquals(GNPGraph graph)
    {
    	return child.rootedGraphEquals(graph.child);
    }
    
    /** Returns a hash code for comparing different GNPGraphs.  In
    general, two graphs which are graphEquals(...) should have the
    same hash code. */
    public int graphHashCode()
    {
    	return child.rootedGraphHashCode();
    }
    
    public GNPGraph lightClone()
    {
    	try
    	{
    		return (GNPGraph)(super.clone());
    	}
    	catch(CloneNotSupportedException e)
    	{
    		throw new InternalError();
    	}
    }
    
    public Object clone()
    {
    	GNPGraph newgraph = lightClone();
    	newgraph.child = (GNPNode)(child.clone());
    	int len = child.parent.length;
    	for(int i = 0; i < len; ++i)
    	{
    		if(child.parent[i].equals(this))
    		{
    			newgraph.child.parent[i] = newgraph;
    			break;
    		}
    	}
    	return newgraph;
    }
    
    /** An expensive function which determines my graph number -- only
    use for errors, etc. Returns ec.gnp.GNPgraph.NO_graphNUM if the 
    graph number could not be
    determined (might happen if it's not been assigned yet). */
    public int graphNumber()
    {
    	if(owner == null)
    		return NO_GRAPHNUM;
    	if(owner.graphs == null)
    		return NO_GRAPHNUM;
    	for(int x = 0; x < owner.graphs.length; ++x)
    	{
    		if(owner.graphs[x] == this)
    			return x;
    	}
    	return NO_GRAPHNUM;
    	
    }
    
    public void setup(final EvolutionState state, final Parameter base)
    {
    	Parameter def = defaultBase();
    	
    	//get rid of deprecated values
    	if(state.parameters.exists(base.push(P_USEGRAPHVIZ), def.push(P_USEGRAPHVIZ)))
    		state.output.error("Parameter no longer used.  See GNPGraph.java for details.", base.push(P_USEGRAPHVIZ), def.push(P_USEGRAPHVIZ));
    	if (state.parameters.exists(base.push(P_USELATEX), def.push(P_USELATEX)))
            state.output.error("Parameter no longer used.  See GNPGraph.java for details.", base.push(P_USELATEX), def.push(P_USELATEX));
        if (state.parameters.exists(base.push(P_USEC), def.push(P_USEC)))
            state.output.error("Parameter no longer used.  See GNPGraph.java for details.", base.push(P_USEC), def.push(P_USEC));
        state.output.exitIfErrors();
        
        String style = state.parameters.getString(base.push(P_PRINT_STYLE), def.push(P_PRINT_STYLE));
        if (style == null)  // assume Lisp
            printStyle = PRINT_STYLE_LISP;
        else if (style.equals(V_C))
            printStyle = PRINT_STYLE_C;
        else if (style.equals(V_DOT))
            printStyle = PRINT_STYLE_DOT;
        else if (style.equals(V_LATEX))
            printStyle = PRINT_STYLE_LATEX;
        
        // in C, treat terminals as variables?  By default, yes.
        printTerminalsAsVariablesInC = state.parameters.getBoolean(base.push(P_USEVARS),def.push(P_USEVARS),true);

        // in C, treat two-child functions as operators?  By default, yes.
        printTwoArgumentNonterminalsAsOperatorsInC = state.parameters.getBoolean(base.push(P_USEOPS),def.push(P_USEOPS),true);

        // determine my constraints -- at this point, the constraints should have been loaded.
        String s = state.parameters.getString(base.push(P_GRAPHCONSTRAINTS), def.push(P_GRAPHCONSTRAINTS));
        if(s == null)
        	state.output.fatal("No graph constraints are defined for the GNPGraph " + base + ".");
        else
        	constraints = GNPGraphConstraints.constraintsFor(s, state).constraintNumber;
        
        state.output.exitIfErrors(); 
    }
    
    /** Verification of validity of the graph -- strictly for debugging purposes only */
	public final void verify(EvolutionState state)
	{
		if(!(state.initializer instanceof GNPInitializer))
		{
			state.output.error("Initializer is not a GNPInitializer");
			return;
		}
		
		GNPInitializer initializer = (GNPInitializer)(state.initializer);
		
		if(child == null)
		{
			state.output.error("Null root child of GNPGraph.");
			return;
		}
		if(owner == null)
		{
			state.output.error("Null owner of GNPGraph.");
			return;
		}
		if(graphNumber() == NO_GRAPHNUM)
		{
			state.output.error("No Graph Number! I appear to be an orphan GNPGraph.");
			return;
		}
		if(constraints < 0 || constraints >= initializer.numGraphConstraints)
		{
			state.output.error("Preposterous graph constraints (" + constraints + ")");
			return;
		}
		
		child.verify(state, constraints(initializer).functionset, 0);
		state.output.exitIfErrors();
	}
	
	/** Prints out the graph in single-line fashion suitable for reading
    in later by computer. O(n). 
    The default version of this method simply calls child's 
    printRootedGraph(...) method. 
	*/
	
	public void printGraph(final EvolutionState state, final int log)
	{
		printGraph(state, log, Output.V_VERBOSE);
	}
	
	public void printGraph(final EvolutionState state, final int log, final int verbosity)
	{
		child.printRootedgraph(state, log, 0);
		state.output.println("", log);
	}
	
	public void printGraph(final EvolutionState state, final PrintWriter writer)
	{
		child.printRootedgraph(state, writer, 0);
		writer.println();
	}
	

/** Builds a new randomly-generated rooted tree and attaches it to the GPTree. */

	public void buildGraph(final EvolutionState state, final int thread) 
    {
		GNPInitializer initializer = ((GNPInitializer)state.initializer);
	    /* 生成树的子节点，即真正的根节点 */
	    child = constraints(initializer).init.newRootedGraph(state,
	        constraints(initializer).graphtype,
	        thread,
	        this,
	        constraints(initializer).functionset,
	        0,
	        GNPNodeBuilder.NOSIZEGIVEN);
    }

	public void buildGraph(final EvolutionState state, final int thread, int count, Individual[] subIndividual)
	{
		GNPInitializer initializer = ((GNPInitializer)state.initializer);
		child = constraints(initializer).init.newRootedGraph(state, constraints(initializer).graphtype, thread, this, 
														constraints(initializer).functionset, 0, GNPNodeBuilder.NOSIZEGIVEN, 
														count, subIndividual);
	}
}
	
