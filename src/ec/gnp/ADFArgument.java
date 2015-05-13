package ec.gnp;
import ec.*;
import ec.util.*;

import java.io.*;

/**
 *  An ADFArgument is a GNPNode which represents an ADF's 
 * <i>argument terminal</i>, its counterpart which returns argument
 * values in its associated function graph.  In lil-gp this is called an
 * ARG node.
 *
 * <p>Obviously, if you have Argument Terminals in a graph, that graph must
 * be only callable by ADFs and ADMs, otherwise the Argument Terminals
 * won't have anything to return.  Furthermore, you must make sure that
 * you don't have an Argument Terminal in a graph whose number is higher
 * than the smallest arity (number of arguments) of a calling ADF or ADM.
 *
 <p><b>Parameters</b><br>
 <table>
 <tr><td valign=top><i>base</i>.<tt>arg</tt><br>
 <font size=-1>int &gt;= 0</font></td>
 <td valign=top>(The related argument position for the ADF Argument Node in the associated ADF)</td></tr>
 </table>

 <p><b>Default Base</b><br>
 gp.adf-argument
 * 
 * @author Batu
 *
 */

public class ADFArgument extends GNPNode
{
	public static final String P_ADFARGUMENT = "adf-argument";
	public final static String P_ARGUMENT = "arg";
	public static final String P_FUNCTIONNAME = "name";
	public int argument;
	
	/** The "function name" of the ADFArgument, to distinguish it from other GNP functions you might provide.  */
	public String name;
	public String name()
	{
		return name;
	}
	
	public Parameter defaultBase()
	{
		return GNPDefaults.base().push(P_ADFARGUMENT);
	}
	
	public String toString()
	{
		return name();
	}
	
	public void setup(final EvolutionState state, final Parameter base)
	{
		super.setup(state, base);
		
		Parameter def = defaultBase();
		
		argument = state.parameters.getInt(base.push(P_ARGUMENT), def.push(P_ARGUMENT), 0);
		if(argument < 0)
			state.output.fatal("Argument terminal must have a positive argument number.", base.push(P_ARGUMENT),def.push(P_ARGUMENT));
		
		name = state.parameters.getString(base.push(P_FUNCTIONNAME), def.push(P_FUNCTIONNAME));
        if (name == null || name.equals(""))
        {
        name = "ARG" + argument;
        state.output.warning("ADFArgument node for argument " + argument + " has no function name.  Using the name " + name(),
            base.push(P_FUNCTIONNAME),def.push(P_FUNCTIONNAME));
        }
	}
	
	public void writeNode(final EvolutionState state, final DataOutput dataOutput) throws IOException
    {
		dataOutput.writeInt(argument);
    }
     

	public void readNode(final EvolutionState state, final DataInput dataInput) throws IOException
    {
    	argument = dataInput.readInt();
    }
	
	public void eval(final EvolutionState state, final int thread, final GNPData input, final ADFStack stack, 
					final GNPIndividual individual, final Problem problem)
	{
		//get current context
		ADFContext c = stack.top(0);
		if(c == null)
			state.output.fatal("No context with which to evaluate ADFArgument terminal " +  toStringForError() +  ".  This often happens if you evaluate a tree by hand  which is supposed to only be an ADF's associated tree."); 
		c.evaluate(state, thread, input, stack, individual, problem, argument);
	}
}
