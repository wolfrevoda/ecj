package ec.gnp;
import ec.*;
import ec.util.*;

import java.util.*;

/**
 * A GNPGraphConstraints is a Clique which defines constraint information
 * common to many different GNPGraph graphs, namely the graph type,
 * builder, and function set.  GNPGraphConstraints have unique names
 * by which they are identified. 
 * In adding new things to GNPGraphConstraints, you should ask yourself
 * the following questions: first, is this something that takes up too
 * much memory to store in GNPGraphs themseves?  second, is this something
 * that needs to be accessed very rapidly, so cannot be implemented as
 * a method call in a GNPGraph?  third, can this be shared among different
 * GNPGraph?
 * 
 * @author Batu
 *
 */

public class GNPGraphConstraints implements Clique
{
	
	private static final long serialVersionUID = 1;

    public static final int SIZE_OF_BYTE = 256;
    public final static String P_NAME = "name";
    public final static String P_SIZE = "size";
    public final static String P_INIT = "init";
    public static final String P_RETURNS = "returns";
    public static final String P_FUNCTIONSET = "fset";

    public String name;
    
    /** The byte value of the constraints -- we can only have 256 of them */
    public byte constraintNumber;
    
    /** The builder for the graph */
    public GNPNodeBuilder init;
    
    /** The type of the root of the graph */
    public GNPType graphtype;
    
    /** The function set for nodes in the tree */
    public GNPFunctionSet functionset;
    
    public String toString()
    {
    	return name;
    }
    
    /** This must be called <i>after</i> the GPTypes and GPFunctionSets 
    have been set up. */
    
    public void setup(final EvolutionState state, final Parameter base)
    {
    	//what's my name?
    	name = state.parameters.getString(base.push(P_NAME), null);
    	if(name == null)
    		state.output.fatal("No name was given for this function set.", base.push(P_NAME));
    	
    	//register me
    	GNPGraphConstraints old_constraints = (GNPGraphConstraints)(((GNPInitializer)state.initializer).graphConstraintRepository.put(name,this));
    	if(old_constraints != null)
    		state.output.fatal("The GNP tree constraint \"" + name + "\" has been defined multiple times.", base.push(P_NAME));
    	
    	//Load my initializing builder
    	init = (GNPNodeBuilder)(state.parameters.getInstanceForParameter(base.push(P_INIT), null, GNPNodeBuilder.class));
    	init.setup(state, base.push(P_INIT));
    	
    	//load my return type
    	String s = state.parameters.getString(base.push(P_RETURNS), null);
    	if(s == null)
    		state.output.fatal("No return type given for the GNPGraphConstraints " + name, base.push(P_RETURNS));
    	graphtype = GNPType.typeFor(s, state);
    	
    	//load my function set
    	s = state.parameters.getString(base.push(P_FUNCTIONSET), null);
    	if(s == null)
    		state.output.fatal("No function set given for the GNPGraphConstraints " + name, base.push(P_RETURNS));
    	functionset = GNPFunctionSet.functionSetFor(s, state);
    	state.output.exitIfErrors();
    	
    }
    
    /** When completed, done will hold all the types which are needed
     in the function set -- you can then check to make sure that
     they contain at least one terminal and (hopefully) at least
     one nonterminal.*/
    private void checkFunctionSetValidity(final EvolutionState state, final Hashtable done, final GNPType type)
    {
    	done.put(type, type);
    	
    	GNPNode[] i = functionset.nodes[type.type];
    	
    	GNPInitializer initializer = ((GNPInitializer)state.initializer);
    	for(int x = 0; x < i.length; ++x)
    	{
    		for(int y = 0; y < i[x].constraints(initializer).childtypes.length; ++y)
    		{
    			if(done.get(i[x].constraints(initializer).childtypes[y]) == null)
    			{
    				checkFunctionSetValidity(state, done, i[x].constraints(initializer).childtypes[y]);
    			}
    		}
    	}
    }
    
    public static GNPGraphConstraints constraintsFor(final String constraintsName, final EvolutionState state)
    {
    	GNPGraphConstraints myConstraints = (GNPGraphConstraints)(((GNPInitializer)state.initializer).graphConstraintRepository.get(constraintsName));
    	if(myConstraints == null)
    		state.output.error("The GNP graph constraints \"" + constraintsName + "\" could not be found.");
    	return myConstraints;
    }
    
    

}
