package ec.gnp;
import ec.*;
import ec.util.*;


/**
 * @author Batu
 * version 1.0
 */


public abstract class GNPType implements Clique{
	public final static String P_NAME = "name";

    /** The name of the type */
    public String name;

    /** The preassigned integer value for the type */
    public int type;

    /** Am I compatible with ("fit" with) <i>t</i>?  For two atomic
        types, this is done by direct pointer equality.  For
        two set types, this is done by determining if the intersection
        is nonempty.  A set type is compatible with an atomic type
        if it contains the atomic type in its set. */
    public abstract boolean compatibleWith(final GNPInitializer initializer, final GNPType t);

    /** Returns the type's name */
    public String toString() { return name; }
    
    public void setup(final EvolutionState state, final Parameter base)
    {
    // What's my name?
    name = state.parameters.getString(base.push(P_NAME),null);
    if (name==null)
        state.output.fatal("No name was given for this GP type.", base.push(P_NAME));

    // Register me
    GNPType old_type = (GNPType)(((GNPInitializer)state.initializer).typeRepository.put(name,this));
    if (old_type != null)
        state.output.fatal("The GP type \"" + name + "\" has been defined multiple times.", base.push(P_NAME));     
    }

/** Returns a type for a given name.
    You must guarantee that after calling typeFor(...) one or
    several times, you call state.output.exitIfErrors() once. */

public static GNPType typeFor(final String typeName,
    final EvolutionState state)
    {
    GNPType myType = (GNPType)(((GNPInitializer)state.initializer).typeRepository.get(typeName));
    if (myType==null)
        state.output.error("The GP type \"" + typeName + "\" could not be found.");
    return myType;
    }
}
