package ec.gnp;
import ec.*;
import ec.util.*;

/**
 * ADFContext is the object pushed onto an ADF stack which represents
 * the current context of an ADM or ADF function call, that is, how to
 * get the argument values that argument_terminals need to return.
 *
 * <p><i>adf</i> contains the relevant ADF/ADM node. 
 * If it's an ADF
 * function call, then <i>arguments[]</i> contains the evaluated arguments
 * to the ADF.  If it's an ADM function call,
 * then <i>arguments[]</i> is set to false.
 *
 * <p>You set up the ADFContext object for a given ADF or ADM node with
 * the prepareADF(...) and prepareADM(...) functions.
 *
 * <p>To evaluate an argument number from an ADFContext, call evaluate(...),
 * and the results are evaluated and copied into input.
 * 
 * @author Batu
 *
 */

public class ADFContext implements Prototype 
{
    public final static String P_ADFCONTEXT = "adf-context";  // deprecated

    /** The ADF/ADM node proper */
    public ADF adf;

    /** An array of GPData nodes (none of the null, when it's used) 
        holding an ADF's arguments' return results */
    public GNPData[] arguments = new GNPData[0];
    
    public Parameter defaultBase()
    {
    	return GNPDefaults.base().push(P_ADFCONTEXT);
    }
    
    public Object clone()
    {
    	try
    	{
    		ADFContext myobj = (ADFContext)(super.clone());
    		
    		myobj.arguments = new GNPData[arguments.length];
    		for(int i = 0; i < myobj.arguments.length; ++i)
    			myobj.arguments[i] = (GNPData)(arguments[i].clone());
    		
    		return myobj;
    	}
    	catch (CloneNotSupportedException e)
    	{
    		throw new InternalError();
    	}
    }
    
    public void setup(final EvolutionState state, final Parameter base)
    {
    }
    
    /** Evaluates the argument number in the current context */
    public void evaluate(final EvolutionState state, final int thread, final GNPData input, final ADFStack stack, final GNPIndividual individual,
    					final Problem problem, final int argument)
    {
    	if(argument >= adf.children.length || argument < 0)
    	{
    		individual.printIndividual(state, 0);
    		state.output.fatal("Invalid argument number for " + adf.errorInfo());
    	}
    	
    	if(!(adf instanceof ADM))
    		arguments[argument].copyTo(input);
    	else
    	{
    		if (stack.moveOntoSubstack(1)!=1)
                state.output.fatal("Substack prematurely empty for "  + adf.errorInfo());
    		
    		//call the GNPNode
    		adf.children[argument].eval(state, thread, input, stack, individual, problem);
    		
    		//restore my context
    		if (stack.moveFromSubstack(1)!=1)
                state.output.fatal("Stack prematurely empty for " + adf.errorInfo());
    	}
    	
    }
    
    /** Increases arguments to accommodate space if necessary.
    Sets adf to a.
    You need to then fill out the arguments yourself. */
    public final void prepareADF(ADF a, GNPProblem problem)
    {
    	// set to the length requested or longer
    	if(a.children.length > arguments.length)
    	{
    		GNPData[] newarguments = new GNPData[a.children.length];
    		System.arraycopy(arguments,0,newarguments,0,arguments.length);
    		for(int i = arguments.length; i < newarguments.length; ++i)
    			newarguments[i] = (GNPData)(problem.input.clone());
    		arguments = newarguments;
    	}
    	adf = a;
    }
    
    public final void prepareADM(ADM a)
    {
    	adf = a;
    }
}
