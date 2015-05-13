package ec.gnp;
import ec.*;
import ec.util.*;

/**
 * 
 * @author Batu
 * version 1.0
 *
 *A GPNodeConstraints is a Clique which defines constraint information
 * common to many different GPNode functions, namely return types,
 * child types, and number of children. 
 * GPNodeConstraints have unique names by which
 * they are identified.
 */

public class GNPNodeConstraints {
	
	public static final int SIZE_OF_BYTE = 256;
	public final static String P_NAME = "name";
	public final static String P_RETURNS = "returns";
	public final static String P_CHILD = "child";
	public final static String P_SIZE = "size";
	public final static String P_PROBABILITY = "prob";
	public final static double DEFAULT_PROBABILITY = 1.0;
	
    /** Probability of selection -- an auxillary measure mostly used by PTC1/PTC2
    right now */
	public double probabilityOfSelection;

	/** The byte value of the constraints -- we can only have 256 of them */
	public byte constraintNumber;

	/** The return type for a GNPNode */
	public GNPType returntype;
	
	/** The children types for a GNPNode */
	public GNPType[] childtypes;
	
	/** The parent types for a GNPNode */
	public GNPType[] parenttypes;
	
	/** The name of the GNPNodeConstraints object -- this is NOT the
    name of the GNPNode */
	public String name;
	
	public String toString()
	{
		return name;
	}
	
    /** A little memory optimization: if GNPNodes have no children, they are welcome to
    use share this zero-sized array as their children array. */
	public GNPNode zeroChildren[] = new GNPNode[0];
	
	  /** This must be called <i>after</i> the GPTypes have been set up. */
	public void setup(final EvolutionState state, final Parameter base)
	{
		// What's my name?
		name = state.parameters.getString(base.push(P_NAME), null);
		if (name==null)
			state.output.fatal("No name was given for this node constraints.", base.push(P_NAME));
		
		// Register me
		GNPNodeConstraints old_constraints = (GNPNodeConstraints)(((GNPInitializer)state.initializer).nodeConstraintRepository.put(name, this));
        if (old_constraints != null)
            state.output.fatal("The GP node constraint \"" + name + "\" has been defined multiple times.", base.push(P_NAME));
        
        // What's my return type?
        String s = state.parameters.getString(base.push(P_RETURNS), null);
        if(s == null)
        	state.output.fatal("No return type given for the GPNodeConstraints " + name, base.push(P_RETURNS));
        returntype = GNPType.typeFor(s, state);
        
        // Load probability of selection
        if(state.parameters.exists(base.push(P_PROBABILITY), null))
        {
        	double f = state.parameters.getDouble(base.push(P_PROBABILITY), null, 0);
        	if(f < 0)
        		state.output.fatal("The probability of selection is < 0, which is not valid.",base.push(P_PROBABILITY),null);
        	probabilityOfSelection = f;
        }
        else
        	probabilityOfSelection = DEFAULT_PROBABILITY;
        
        //How many child types do we have
        
        int x = state.parameters.getInt(base.push(P_SIZE), null, 0);
        if(x < 0)
        	state.output.fatal("The number of children types for the GPNodeConstraints " + name + " must be >= 0.", base.push(P_SIZE));
        
        childtypes = new GNPType[x];
        
        Parameter p = base.push(P_CHILD);
        
        //Load my children
        for(x = 0; x < childtypes.length; ++x)
        {
        	s = state.parameters.getString(p.push("" + x), null);
        	if(s == null)
        		state.output.fatal("Type #" + x + " is not defined for the GNPNodeConstraints " + name +  ".", base.push(""+x));
        	childtypes[x] = GNPType.typeFor(s, state);
        }
        
        state.output.exitIfErrors();
	}
	
    /** You must guarantee that after calling constraintsFor(...) one or
    several times, you call state.output.exitIfErrors() once. */
	
	public static GNPNodeConstraints constraintsFor(final String constraintsName, final EvolutionState state)
	{
		GNPNodeConstraints myConstraints = (GNPNodeConstraints)(((GNPInitializer)state.initializer).nodeConstraintRepository.get(constraintsName));
		if(myConstraints == null)
			state.output.error("The GP node constraint \"" + constraintsName + "\" could not be found.");
		return myConstraints;
	}

}
