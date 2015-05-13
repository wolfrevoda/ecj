package ec.gnp;
import java.util.Enumeration;
import java.util.Hashtable;

import ec.gp.GPAtomicType;
import ec.gp.GPSetType;
import ec.gp.GPType;
import ec.simple.SimpleInitializer;
import ec.util.Parameter;
import ec.EvolutionState;

/**
 * 
 * @author Batu
 * version 1.0
 *
 */
public class GNPInitializer extends SimpleInitializer {
	   private static final long serialVersionUID = 1;

	    public static final int SIZE_OF_BYTE = 256;
	    public final static String P_TYPE = "type";
	    public final static String P_NODECONSTRAINTS = "nc";
	    public final static String P_GRAPHCONSTRAINTS = "gc";
	    public final static String P_FUNCTIONSETS = "fs";
	    public final static String P_SIZE = "size";
	    public final static String P_ATOMIC = "a";
	    public final static String P_SET = "s";
	    
	    public Hashtable typeRepository;
	    public GNPType[] types;
	    public int numAtomicTypes;
	    public int numSetTypes;
	    
	    public Hashtable nodeConstraintRepository;
	    public GNPNodeConstraints[] nodeConstraints;
	    public byte numNodeConstraints;
	    
	    public Hashtable functionSetRepository;
	    
	    public Hashtable graphConstraintRepository;
	    public GNPGraphConstraints[] graphConstraits;
	    public int numGraphConstraints;
	    
	    public void setup(final EvolutionState state, final Parameter base)
	    {
	    	super.setup(null, base);
	    	
	    	setupTypes(state, GNPDefaults.base().push(P_TYPE));
	    	
	    }
	    
	    /**
	     * set all the types, load them from parameter file.
	     * @param state
	     * @param base
	     */
	    public void setupTypes(final EvolutionState state, final Parameter base)
	    {
	    	state.output.message("Processing GNP types");
	    	
	    	typeRepository = new Hashtable();
	    	numAtomicTypes = numSetTypes = 0;
	    	
	    	//check how many atomic types do we have
	    	
	    	int x = state.parameters.getInt(base.push(P_ATOMIC), null, 1);
	    	if (x<=0) 
	            state.output.fatal("The number of GP atomic types must be at least 1.",base.push(P_ATOMIC).push(P_SIZE));
	    	
	    	//load our atomic type
	    	
	    	for(int y = 0; y < x; ++y)
	    		new GNPSetType().setup(state, base.push(P_SET).push("" + y));
	    	
	    	postProcessTypes();
	    }
	    
	    public void postProcessTypes()
	    {
	    	// assign positive integers and 0 to atomic types
	    	int x = 0;
	    	Enumeration e = typeRepository.elements();
	    	while(e.hasMoreElements())
	    	{
	    		GNPType t = (GNPType)(e.nextElement());
	    		if (t instanceof GNPAtomicType)
	    		{
	    			t.type = x;
	    			x++;
	    		}
	    	}
	    	
	    	numAtomicTypes = x;
	    	
	    	e = typeRepository.elements();
	    	while(e.hasMoreElements())
	    	{
	    		GNPType t = (GNPType)(e.nextElement());
	    		if(t instanceof GNPSetType)
	    		{
	    			((GNPSetType)t).postProcessSetType(numAtomicTypes);
	                t.type = x; 
	                x++;
	    		}
	    	}
	    	
	    	numSetTypes = x - numAtomicTypes;
	    	
	    	 types = new GNPType[numSetTypes + numAtomicTypes];
	         e = typeRepository.elements();
	         while(e.hasMoreElements())
	         {
	        	 GNPType t = (GNPType)(e.nextElement());
	             types[t.type] = t;
	         }
	    }
	    
	    public void setupNodeConstraints(final EvolutionState state, final Parameter base)
	    {
	    	state.output.message("Processing GP Node Constraints");
	    	
	    	nodeConstraintRepository = new Hashtable();
	    	nodeConstraints = new GNPNodeConstraints[SIZE_OF_BYTE];
	    	numNodeConstraints = 0;
	    	
	    	//Check how many GPNodeConstraints do we have?
	    	int x = state.parameters.getInt(base.push(P_SIZE), null, 1);
	    	if(x <= 0)
	    		state.output.fatal("The number of GP node constraints must be at least 1.", base.push(P_SIZE));
	    	
	    	//load our constraints
	    	for(int y = 0; y < x; ++y)
	    	{
	    		GNPNodeConstraints c;
	    		if(state.parameters.exists(base.push("" + y), null))
	    			c = (GNPNodeConstraints)(state.parameters.getInstanceForParameterEq(base.push("" + y), null, GNPNodeConstraints.class));
	    		else
	    		{
	    			state.output.message("No GP Node Constraints specified, assuming the default class: ec.gp.GPNodeConstraints for " +  base.push(""+y));
	    			c = new GNPNodeConstraints();
	    		}
	    		c.setup(state, base.push("" + y));
	    	}
	    	
	    	//set our constraints array up
	    	Enumeration e = nodeConstraintRepository.elements();
	    	while(e.hasMoreElements())
	    	{
	    		GNPNodeConstraints c = (GNPNodeConstraints)(e.nextElement());
	    		c.constraintNumber = numNodeConstraints;
	    		nodeConstraints[numNodeConstraints] = c;
	    		numNodeConstraints++;
	    		
	    	}
	    }
	    
	    public void setupFunctionSets(final EvolutionState state, final Parameter base)
	    {
	    	state.output.message("Processing GP Function Sets");
	        
	        functionSetRepository = new Hashtable();
	        // How many GPFunctionSets do we have?
	        int x = state.parameters.getInt(base.push(P_SIZE),null,1);
	        if (x<=0) 
	            state.output.fatal("The number of GPFunctionSets must be at least 1.",base.push(P_SIZE));
	        
	        // Load our FunctionSet
	        for(int y = 0; y < x; ++y)
	        {
	        	GNPFunctionSet c;
	        }
	    }
	    
}
