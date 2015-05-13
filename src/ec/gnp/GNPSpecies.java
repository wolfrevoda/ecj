package ec.gnp;
import ec.*;
import ec.util.*;

import java.io.*;

public class GNPSpecies extends Species
{
	public static final String P_GNPSPECIES = "species";
	
	public Parameter defaultBase()
	{
		return GNPDefaults.base().push(P_GNPSPECIES);
	}
	
	public void setup(final EvolutionState state, final Parameter base)
	{
		super.setup(state, base);
		
		if(!(i_prototype instanceof GNPIndividual))
			state.output.fatal("The Individual class for the Species " + getClass().getName() + " is must be a subclass of ec.gp.GNPIndividual.", base );
	}
	
	public Individual newIndividual(EvolutionState state, int thread)
	{
		GNPIndividual newind = ((GNPIndividual)(i_prototype)).lightClone();
		
		// Initialize the graphs
		for(int i = 0; i < newind.graphs.length; ++i)
			newind.graphs[i].buildGraph(state, thread);
		
		// Set the fitness
		newind.fitness = (Fitness)(f_prototype.clone());
		newind.evaluated = false;
		newind.species = this;
		
		return newind;
	}
	
	
    public Individual newIndividual(EvolutionState state, int thread, Individual[] subIndividual, int count)
    {
    	GNPIndividual newind = ((GNPIndividual)(i_prototype)).lightClone();
        
        // Initialize the trees
        for (int x=0;x<newind.graphs.length;x++)
            newind.graphs[x].buildGraph(state, thread, count, subIndividual);

        // Set the fitness
        newind.fitness = (Fitness)(f_prototype.clone());
        newind.evaluated = false;

        // Set the species to me
        newind.species = this;
                
        // ...and we're ready!
        return newind;
    }
	
	public Individual newIndividual(final EvolutionState state, final LineNumberReader reader) throws IOException
	{
		GNPIndividual newind = ((GNPIndividual)i_prototype).lightClone();
		
		newind.fitness = (Fitness)(f_prototype.clone());
		newind.evaluated = false;
		newind.species = this;
		
		newind.readIndividual(state, reader);
		
		return newind;
		
	}
	
	public Individual newIndividual(final EvolutionState state, final DataInput dataInput) throws IOException
	{
	GNPIndividual newind = ((GNPIndividual)i_prototype).lightClone();
		
		newind.fitness = (Fitness)(f_prototype.clone());
		newind.evaluated = false;
		newind.species = this;
		
		newind.readIndividual(state, dataInput);
		
		return newind;
	}
}
