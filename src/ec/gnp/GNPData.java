package ec.gnp;
import ec.*;
import ec.util.*;

/**
 * 
 * GNPData is the parent class of data transferred between GNPNodes.
 * If performed correctly, there need be only one GNPData instance 
 * ever created in the evaluation of many individuals. 
 *
 * <p>You can use GNPData as-is if you have absolutely no data to
 * transfer between individuals.  Otherwise, you need to subclass
 * GNPData, add your own instance variables, and then override
 * the copyTo(...) method and, depending on whether the data has
 * pointers in it (like arrays), the clone() method as well.
 * 
 * @author Batu
 * version 1.0
 *
 */

public class GNPData {
	
	public static final String P_GNPDATA = "data";
	
	public void copyTo(final GNPData gpd)
	{
		
	}
	
	public Parameter defaultBase()
	{
		return GNPDefaults.base().push(P_GNPDATA);
	}
	
	public void setup(final EvolutionState state, final Parameter base)
	{
		
	}
	
	public Object clone()
	{
		try
		{
			return super.clone();
		}
		catch(CloneNotSupportedException e)
		{
			throw new InternalError();
		}
	}

}
