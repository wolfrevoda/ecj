package ec.gnp;
import ec.*;

import ec.util.*;

import java.io.*;

/**
 * An ADF is a GNPNode which implements an "Automatically Defined Function",
 * as described in Koza II.  
 *
 * <p>In this system, the ADF facility consists of several classes: ADF,
 * ADM, ADFStack, ADFContext, and ADFArgument. ADFs, and their cousins
 * ADMs ("Automatically Defined Macros [Lee Spector]"), appear as
 * typical function nodes in a GNP graph.  However, they have a special
 * <i>associated graph</i> in the individual's graph forest which 
 * they evaluate as a kind of a "subfunction".
 *
 * <p>When an ADF is evaluated, it first evaluates all of its children
 * and stores away their results.  It then evaluates its associated graph.
 * In the associated graph may exist one or more <i>ADF Argument Terminals</i>,
 * defined by the ADFArgument class.  These terminal nodes are associated
 * with a single number which represents the "argument" in the original ADF
 * which evaluated their graph.  When an Argument Terminal is evaluated,
 * it returns the stored result for that child number in the parent ADF.
 * Ultimately, when the associated graph completes its evaluation, the ADF
 * returns that value.
 *
 * <p>ADMs work slightly differently.  When an ADM is evaluated, it
 * immediately evaluates its associated graph without first evaluating
 * any children.  When an Argument Terminal is evaluated, it evaluates
 * the subgraph of the appropriate child number in the parent ADM and returns
 * that result.  These subgraphs can be evaluated many times.  When the
 * associated graph completes its evaluation, the ADM returns that value.
 * 
 * <p>Obviously, if you have Argument Terminals in a graph, that graph must
 * be only callable by ADFs and ADMs, otherwise the Argument Terminals
 * won't have anything to return.  Furthermore, you must make sure that
 * you don't have an Argument Terminal in a graph whose number is higher
 * than the smallest arity (number of arguments) of a calling ADF or ADM.
 *
 * <p>The mechanism behind ADFs and ADMs is complex, requiring two specially-
 * stored stacks (contained in the ADFStack object) of ADFContexts.  For
 * information on how this mechanism works, see ADFStack.
 *
 *

 <p><b>Parameters</b><br>
 <table>
 <tr><td valign=top><i>base</i>.<tt>graph</tt><br>
 <font size=-1>int &gt;= 0</font></td>
 <td valign=top>(The "associated graph" of the ADF)</td></tr>
 <tr><td valign=top><i>base</i>.<tt>name</tt><br>
 <font size=-1>String, can be undefined</font></td>
 <td valign=top>(A simple "name" of the ADF to distinguish it from other ADF functions in your function set.  Use only letters, numbers, hyphens, and underscores.  Lowercase is best.)</td></tr>
 </table>

 <p><b>Default Base</b><br>
 gp.adf
 * 
 * @author Batu
 * version 1.0
 *
 */

public class ADF extends GNPNode{
	
	public static final String P_ADF = "adf";
	public static final String P_ASSOCIATEDGRAPH = "graph";
	public static final String P_FUNCTIONNAME = "name";
	
	/** The ADF's associated graph */
	public int associatedGraph;
	
    /** The "function name" of the ADF, to distinguish it from other GNP
    functions you might provide.  */
	public String name;
	
	public String name()
	{
		return name;
	}
	
	public Parameter defaultBase()
	{
		return GNPDefaults.base().push(P_ADF);
	}
	
	public void writeNode(final EvolutionState state, final DataOutput dataOutput) throws IOException
	{
		dataOutput.writeInt(associatedGraph);
		dataOutput.writeUTF(name);
	}
	
	public void readNode(final EvolutionState state, final DataInput dataInput) throws IOException
	{
		associatedGraph = dataInput.readInt();
		name = dataInput.readUTF();
	}
	
	/** Returns name.hashCode() + class.hashCode() + associatedGraph.  Hope
    that's reasonably random. */
	public int nodeHashCode()
	{
		return (this.getClass().hashCode() + name.hashCode() + associatedGraph);
	}
	/** Determines node equality by comparing the class, associated graph, and
    function name of the nodes. */
	public boolean nodeEquals(final GNPNode node)
	{
		if(!this.getClass().equals(node.getClass()) || children.length != node.children.length)
			return false;
		ADF adf = (ADF)node;
		return (associatedGraph == adf.associatedGraph && name.equals(adf.name));
	}
	
	 /** Checks type-compatibility constraints between the ADF, its argument terminals, and the graph type of its associated graph, and also 
	  * checks to make sure the graph exists, there aren't invalid argument terminals in it, and there are sufficient argument terminals 
	  * (a warning).  Whew! */
	public void checkConstraints(final EvolutionState state, final int graph, final GNPIndividual typicalIndividual, 
			final Parameter individualBase)
	{
		super.checkConstraints(state, graph, typicalIndividual, individualBase);
		
		//does the associated graph exist?
		
		if(associatedGraph < 0 || associatedGraph >= typicalIndividual.graphs.length)
			state.output.error("The node " + toStringForError() + " of individual " + 
	                individualBase + " must have an associated graph that is >= 0 and < " + typicalIndividual.graphs.length + 
	                ".  Value provided was: " + associatedGraph);
		else
		{
			// is the associated graph of the correct type?
			GNPInitializer initializer = (GNPInitializer)state.initializer;
			if(!constraints(initializer).returntype.compatibleWith(initializer, 
					typicalIndividual.graphs[associatedGraph].constraints(initializer).graphtype))
				state.output.error("The return type of the node " + toStringForError() + " of individual " + individualBase + 
						"is not type-compatible with the graph type of its associated graph");
			
			GNPNode[][] funcs = typicalIndividual.graphs[associatedGraph].constraints(initializer).functionset.nodes;
			
			ADFArgument validArgument[] = new ADFArgument[children.length];
			
			for(int w = 0; w < funcs.length; ++w)
			{
		        // does the tree's function set have argument terminals 
                // that are beyond what I can provide?  (issue an error)
				GNPNode[] gnpfi = funcs[w];
				for(int x = 0; x < gnpfi.length; ++x)
				{
					if(gnpfi[x] instanceof ADFArgument)
					{
						ADFArgument argument = (ADFArgument)(gnpfi[x]);
						int arg = argument.argument;
						if(arg >= children.length)
							state.output.error("The node" + toStringForError() + " in individual "  + individualBase +
							" would call its associated graph, which has an argument terminal with an argument number (" + 
							arg + ") >= the ADF/ADM's arity (" + children.length +").  The argument terminal in question is " 
                            + gnpfi[x].toStringForError()); 
						else
						{
							if(validArgument[arg] != null && validArgument[arg] != argument)
								state.output.warning("There exists more than one Argument terminal for argument #" + 
													arg + " for the node " + toStringForError() + " in individual " + individualBase);
							else
								validArgument[arg] = argument;
							
							// is the argument terminal of the correct return type? 
							if(!gnpfi[x].constraints(initializer).returntype.compatibleWith(initializer, constraints(initializer).childtypes[arg]))
								state.output.error("The node " + toStringForError() + " in individual " + individualBase + 
										" would call its associated tree, which has an argument terminal which is not type-compatible with the related argument position of the ADF/ADM.  The argument terminal in question is "
										+ gnpfi[x].toStringForError());
						}
					}
				}
				// does the graph's function set have fewer argument terminals
	            // than I can provide? (issue a warning)
	            
	            for (int x=0;x<children.length;x++)
	                if (validArgument[x] == null) 
	                    state.output.warning("There is no argument terminal for argument #" 
	                        + x + " for the node " 
	                        + toStringForError() + " in individual " + 
	                        individualBase);
			}
				
		}
	}
	
	public void setup(final EvolutionState state, final Parameter base)
	{
		Parameter def = defaultBase();
		
		associatedGraph = state.parameters.getInt(base.push(P_ASSOCIATEDGRAPH), def.push(P_ASSOCIATEDGRAPH), 0);
		if(associatedGraph < 0)
			state.output.fatal("ADF/ADM node must have a positive-numbered associated graph", base.push(P_ASSOCIATEDGRAPH), def.push(P_FUNCTIONNAME));
		name = state.parameters.getString(base.push(P_FUNCTIONNAME), def.push(P_FUNCTIONNAME));
		 if (name == null || name.equals(""))
         {
         name = "ADF" + (associatedGraph - 1);
         state.output.warning("ADF/ADM node for Tree " + associatedGraph + " has no function name.  Using the name " + name(),
             base.push(P_FUNCTIONNAME),def.push(P_FUNCTIONNAME));
         }
                     
     if (name.length() == 1)
         {
         state.output.warning("Using old-style ADF/ADM name.  You should change it to something longer and more descriptive, such as ADF" + name,
             base.push(P_FUNCTIONNAME),def.push(P_FUNCTIONNAME));
         }

     // now we let our parent set up.  
     super.setup(state,base);
	}
	
	public String toString() { return name(); }
    
    public void eval(final EvolutionState state, final int thread, final GNPData input, final ADFStack stack, final GNPIndividual individual,
        final Problem problem)
    {
        // get a context and prepare it
        ADFContext c = stack.get();
        c.prepareADF(this, (GNPProblem) problem);

        // evaluate my arguments and load 'em in 
        for(int x=0;x<children.length;x++)
            {
            input.copyTo(c.arguments[x]);
            children[x].eval(state,thread,c.arguments[x],
                stack,individual,problem);
            }

        // Now push the context onto the stack.
        stack.push(c);

        // evaluate the top of the associatedTree
        individual.graphs[associatedGraph].child.eval(
            state,thread,input,stack,individual,problem);

        // pop the context off, and we're done!
        if (stack.pop(1) != 1)
            state.output.fatal("Stack prematurely empty for " + toStringForError());
        }
}
