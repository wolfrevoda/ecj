package ec.gnp;
import java.io.Serializable;

/**
 * GNPNodeGatherer is a small container object for the GNPNode.nodeInPosition(...)
 * method and GNPNode.numNodes(...) method. 
 * It may be safely reused without being reinitialized.
 * 
 * @author Batu
 * version 1.0
 *
 */

public abstract class GNPNodeGatherer implements Serializable
{
	GNPNode node;
	
	
    /** Returns true if thisNode is the kind of node to be considered in the
    gather count for nodeInPosition(...) and GPNode.numNodes(GPNodeGatherer).
    The default form simply returns true.  */
	public boolean test(final GNPNode thisNode)
	{
		return true;
	}
}
