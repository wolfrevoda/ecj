package ec.gnp;
import ec.*;
import ec.gp.GPAtomicType;
import ec.util.*;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * 
 * @author Batu
 * version 1.0
 *
 *GNPSetType is a GNPType which contains the AutomicTypes in a set
 */

public final class GNPSetType extends GNPType{

    public static final String P_MEMBER = "member";
    public static final String P_SIZE = "size";	
    
    /** a packet, sorted array of atomic types in the set */
    public int[] types_packed;
    
    /** A sparse array of atomic types in the set */
    public boolean[] types_sparse;

    /** The hashtable of types in the set */
    public Hashtable types_h;
    
    public GNPSetType() {}
    
    /** Sets up the packed and sparse arrays based on the hashtable */
    public void postProcessSetType(int totalAutomicTypes)
    {
    	//load the hashtable into the arrays
    	
    	int x = 0;
    	types_packed = new int[types_h.size()];
    	types_sparse = new boolean[totalAutomicTypes];
    
    	//get all of the values from hashtable
    	
    	Enumeration e = types_h.elements();
    	while(e.hasMoreElements())
    	{
    		GNPAtomicType t = (GNPAtomicType)(e.nextElement());
    		types_packed[x++] = t.type;
    		types_sparse[x++] = true;
    	}
    	
    	java.util.Arrays.sort(types_packed);
    	
    }
    
    public void setup(final EvolutionState state, Parameter base)
    {
    	super.setup(state, base);
    	
    	//the hashtable of types in the set
    	types_h = new Hashtable();
    	
    	//check how many atomic do we have
    	int len = state.parameters.getInt(base.push(P_SIZE), null, 1);
    	if (len<=0) 
    		state.output.fatal("The number of atomic types in the GPSetType " + name + " must be >= 1.",base.push(P_SIZE));
    	
    	//load the GPAtomicTypes
    	for(int x = 0; x < len; ++x)
    	{
    		String s = state.parameters.getString(base.push(P_MEMBER).push(""+x),null);
            if (s==null)
                state.output.fatal("Atomic type member #" + x + " is not defined for the GNPSetType " + name + ".",base.push(P_MEMBER).push(""+x));
            GNPType t = GNPType.typeFor(s, state);
            if (!(t instanceof GNPAtomicType)) // uh oh
                state.output.fatal("Atomic type member #" + x + " of GNPSetType " + name + " is not a GNPAtomicType.",base.push(P_MEMBER).push(""+x));
            if (types_h.get(t)!=null)
                state.output.warning("Atomic type member #" + x + " is included more than once in GPSetType " + name + ".", base.push(P_MEMBER).push(""+x));
            types_h.put(t, t);
    	}
    }
    
    public final boolean compatibleWith(final GNPInitializer initializer, final GNPType t)
    {
    	if(t.type == type)
    		return true;
    	else if(t.type < initializer.numAtomicTypes)
    		return types_sparse[t.type];
    	else
    	{
    		GNPSetType s = (GNPSetType)t;
    		int x = 0; int y = 0;
    		for( ; x < types_packed.length && y < s.types_packed.length; )
    		{
    			if(types_packed[x] == s.types_packed[y])
    				return true;
    			else if(types_packed[x] < s.types_packed[y])
    				++x;
    			else
    				++y;
    		}
    		return false;
    	}
    	
    }
    
}
