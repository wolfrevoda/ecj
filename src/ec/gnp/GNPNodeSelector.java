package ec.gnp;
import ec.*;

/**
 * GNPNodeSelector is a Prototype which describes algorithms which
 * select random nodes out of graphs, typically marking them for
 * mutation, crossover, or whatnot.  GNPNodeSelectors can cache information
 * about a graph, as they may receive the pickNode(...) method more than
 * once on a graph.  But this should really only be done if it can be
 * done relatively efficiently; it's not all that common.  A GNPNodeSelector
 * will be called reset() just before it is pressed into service in
 * selecting nodes from a new graph, which gives it the chance to
 * reset caches, etc.
 * 
 * @author Batu
 *
 */

public interface GNPNodeSelector extends Prototype
{
	/** Picks a node at random from graph and returns it.   The graph
    is located in ind, which is located in s.population[subpopulation].
    This method will be preceded with a call to reset();
    afterwards, pickNode(...) may be called several times for the
    same graph.
	 */
	
	public abstract GNPNode pickNode(final EvolutionState s, final int subpopulation, final int thread, final GNPIndividual ind,
									final GNPGraph graph);
	
	 /** Resets the Node Selector before a new series of pickNode()
    if need be. */
	
	public abstract void reset();

}
