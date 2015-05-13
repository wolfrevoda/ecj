package ec.gnp;
import ec.util.*;
import ec.*;
import ec.simple.*;

public abstract class GNPProblem extends Problem implements SimpleProblemForm
{
	public final static String P_GNPPROBLEM = "problem";
	public final static String P_STACK = "stack";
	public final static String P_DATA = "data";
	
	public ADFStack stack;
	
	public GNPData input;
	
	public Parameter defaultBase()
	{
		return GNPDefaults.base().push(P_GNPPROBLEM);
	}
	
	public void setup(final EvolutionState state, final Parameter base)
	{
		Parameter p = base.push(P_STACK);
		Parameter def = defaultBase();
		
		stack = (ADFStack)(state.parameters.getInstanceForParameter(p, def.push(P_STACK), ADFStack.class));
		stack.setup(state, p);
		
		p = base.push(P_DATA);
		input = (GNPData)(state.parameters.getInstanceForParameter(p, def.push(P_DATA), GNPData.class));
		input.setup(state, p);
	}
	
	public Object clone()
	{
		GNPProblem prob = (GNPProblem)(super.clone());
		prob.stack = (ADFStack)(stack.clone());
		prob.input = (GNPData)(input.clone());
		return prob;
	}
}
