package ec.gnp;
import ec.*;

/**
 * An ADM is an ADF which doesn't evaluate its arguments beforehand, but
 * instead only evaluates them (and possibly repeatedly) when necessary
 * at runtime. 
 * 
 * @author Batu
 *
 */

public class ADM extends ADF
{
	public void eval(final EvolutionState state, final int thread, final GNPData input, final ADFStack stack, 
					final GNPIndividual individual, final Problem problem)
	{
		//prepare a context
		ADFContext c = stack.push(stack.get());
		c.prepareADM(this);
		
		//evaluate the top of the associatedGraph
		individual.graphs[associatedGraph].child.eval(state, thread, input, stack, individual, problem);
		
		//pop the context off
		if(stack.pop(1) != 1)
			state.output.fatal("Stack prematurely empty for " + toStringForError());
	}

}
