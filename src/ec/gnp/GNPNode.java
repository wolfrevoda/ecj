package ec.gnp;
import ec.*;
import ec.util.*;

import java.io.*;
import java.util.*;

/**
 * 
 * @author Batu
 * version 1.0
 *
 */

public abstract class GNPNode implements GNPNodeParent, Prototype{

	public static final String P_NODE = "node";
	public static final String P_NODECONSTRAINTS = "nc";
	public static final String GNPNODEPRINTTAB = "    ";
	public static final int MAXPRINTBYTES = 40;
	
	public static final int NODESEARCH_ALL = 0;
	public static final int NODESEARCH_TERMINALS = 1;
	public static final int NODESEARCH_NONTERMINALS = 2;
	static final int NODESEARCH_CUSTOM = 3;  // should not be public
	
	public static final int CHILDREN_UNKNOWN = -1;
	
	/** The GNPNode's parent.  4 bytes. for graph, maybe have at least one parent, so we use array to store it's parent */
	public GNPNodeParent parent[];
	public GNPNode children[];
	
	 /** The argument position of the child in its parent. 
    This is a byte to save space (GNPNode is the critical object space-wise) -- 
    besides, how often do you have 256 children? You can change this to a short
    or int easily if you absolutely need to.  It's possible to eliminate even
    this and have the child find itself in its parent, but that's an O(children[])
    operation, and probably not inlinable, so I figure a byte is okay. 
    for different parent, the node will have different position, so we define a array called 
    argposition to store the position for this node*/
	public byte argposition[];
	
	/** The GNPNode's constraints.  This is a byte to save space -- how often do
    you have 256 different GNPNodeConstraints?  Well, I guess it's not infeasible.
    You can increase this to an int without much trouble.  You typically 
    shouldn't access the constraints through this variable -- use the constraints(state)
    method instead. */
	public byte constraints;
	
	/** The GNPNpde's visit is a bool variable which to judge whether this node has been visited,
	 * if true, we can not add this node any more when we calculate the number of graph's nodes */
	public boolean visit = false;
	
	/** Path record the minimum distance from this node to root */
	public int path;
	
	/** Depth record a node's depth from different parents */
	public int depth[];
	
	/** Record the minimum depth for this node  */
	public int minDepth;
	
	public final GNPNodeConstraints constraints(final GNPInitializer initializer)
	{
		return initializer.nodeConstraints[constraints];
	}
	
	/** The default base for GPNodes -- defined even though
    GPNode is abstract so you don't have to in subclasses. */
	public Parameter defaultBase()
	{
		return GNPDefaults.base().push(P_NODE);
	}
	
    /** You ought to override this method to check to make sure that the
    constraints are valid as best you can tell.  Things you might
    check for:

    <ul>
    <li> children.length is correct
    <li> certain arguments in constraints.childtypes are 
    swap-compatible with each other
    <li> constraints.returntype is swap-compatible with appropriate 
    arguments in constraints.childtypes
    </ul>
    
    You can't check for everything, of course, but you might try some
    obvious checks for blunders.  The default version of this method
    simply calls numChildren() if it's defined (it returns something >= 0).
    If the value doesn't match the current number of children, an error is raised.
    This is a simple constraints check.

    The ultimate caller of this method must guarantee that he will eventually
    call state.output.exitIfErrors(), so you can freely use state.output.error
    instead of state.output.fatal(), which will help a lot.
    
    Warning: this method may get called more than once.
     */
	
	public void checkConstraints(final EvolutionState state, final int graph, final GNPIndividual typicalIndividual, final Parameter individualBase)
	{
		int numChildren = expectedChildren();
		if(numChildren >= 0 && children.length != numChildren)
			state.output.error("Incorrect number of children for node " + toStringForError() + " at " + individualBase + ", was expecting " + numChildren + " but got " + children.length);
		
	}
	
	/** 
    Returns the number of children this node expects to have.  This method is
    only called by the default implementation of checkConstraints(...), and by default
    it returns CHILDREN_UNKNOWN.  You can override this method to return a value >= 0,
    which will be checked for in the default checkConstraints(...), or you can leave
    this method alone and override checkConstraints(...) to check for more complex constraints
    as you see fit.
	*/
	public int expectedChildren()
	{
		return CHILDREN_UNKNOWN;
	}
	
    /** 
    <strong>Sets up a <i>prototypical</i> GNPNode with those features all nodes of that
    prototype share, and nothing more.  So no filled-in children, 
    no argposition, no parent.  Yet.</strong>

    This must be called <i>after</i> the GPTypes and GNPNodeConstraints 
    have been set up.  Presently they're set up in GNPInitializer,
    which gets called before this does, so we're safe. 
    
    You should override this if you need to load some special features on
    a per-function basis.  Note that base hangs off of a function set, so
    this method may get called for different instances in the same GNPNode
    class if they're being set up as prototypes for different GPFunctionSets.

    If you absolutely need some global base, then you should use something
    hanging off of GPDefaults.base().

    The ultimate caller of this method must guarantee that he will eventually
    call state.output.exitIfErrors(), so you can freely use state.output.error
    instead of state.output.fatal(), which will help a lot.
    */
	public void setup(final EvolutionState state, final Parameter base)
	{
		Parameter def = defaultBase();
		
		//define my constraints
		String s = state.parameters.getString(base.push(P_NODECONSTRAINTS), def.push(P_NODECONSTRAINTS));
		if(s == null)
			state.output.fatal("No node constraints are defined for the GPNode " + toStringForError(),base.push(P_NODECONSTRAINTS), def.push(P_NODECONSTRAINTS));
		else
			constraints = GNPNodeConstraints.constraintsFor(s, state).constraintNumber;
		
        // The number of children is determined by the constraints.  Though
        // for some special versions of GNPNode, we may have to enforce certain
        // rules, checked in children versions of setup(...)
		
		GNPNodeConstraints constraintsObj = constraints(((GNPInitializer)state.initializer));
		int Clen = constraintsObj.childtypes.length;
		if(Clen == 0)
			children = constraintsObj.zeroChildren;
		else
			children = new GNPNode[Clen];
		
		int Plen = constraintsObj.parenttypes.length;
		parent = new GNPNodeParent[Plen];
		argposition = new byte[Plen];
		depth = new int[Plen];
		
	}
	
    /** Returns the argument type of the slot that I fit into in my parent.  
    If I'm the root, returns the graphtype of the GNPGraph. */
	public final GNPType parentType(final GNPInitializer initializer, int id)
	{
		if(parent[id] instanceof GNPNode)
			return ((GNPNode)parent[id]).constraints(initializer).childtypes[argposition[id]];
		else
			return ((GNPGraph)parent[id]).constraints(initializer).graphtype;
	}
	
    /** Verification of validity of the node in the graph --, strictly for debugging purposes only */
	final int verify(EvolutionState state, GNPFunctionSet set, int index)
	{
		if(!(state.initializer instanceof GNPInitializer))
		{
			state.output.error("" + index + ": Initializer is not a GNPInitializer");
			return index + 1;
		}
		
		GNPInitializer initializer = (GNPInitializer)(state.initializer);
		
		// is the parent and argposition right?
		if (parent == null)
        { 
			state.output.error("" + index + ": null parent"); 
			return index+1; 
		}
		for(int i = 0; i < parent.length; ++i)
		{
			if (argposition[i] < 0)
	        {
				state.output.error("" + index + " The " + i + "th position " + ": negative argposition");
				return index+1; 
			}
			if(parent[i] instanceof GNPGraph && ((GNPGraph)parent[i]).child != this)
			{
				state.output.error("" + index + " The " + i + "th position " + ": I think I am a root node, but my GNPGraph does not think I am a root node");
				return index + 1;
			}
			if(parent[i] instanceof GNPGraph && argposition[i] !=0)
			{
				state.output.error("" + index + " The " + i + "th position " + ": I think I am a root node, but my argposition is not 0");
				return index+1;
			}
			if(parent[i] instanceof GNPGraph && argposition[i] >= ((GNPNode)parent[i]).children.length)
			{
				state.output.error("" + index + " The " + i + "th position " + ": argposition outside range of parent's children array");
				return index+1;
			}
			if(parent[i] instanceof GNPNode && ((GNPNode)parent[i]).children[argposition[i]] != this)
			{
				state.output.error("" + index + ": I am not found in the provided argposition ("+argposition+") of my parent's children array"); 
				return index+1;
			}
		}
		
		// are the parents and argpositions right for my kids?
		if(children == null)
		{
			state.output.error("" + index + ": Null Children Array"); 
			return index+1;
		}
		for(int x = 0; x < children.length; ++x)
		{
			if (children[x] == null)
            { 
				state.output.error("" + index + ": Null Child (#" + x + " )"); 
				return index+1; 
			}
			if (!isParentThis(children[x]))
            { 
				state.output.error("" + index + ": child #"+x+" does not have me as a parent"); 
				return index+1; 
			}
			if (hasPositionNegative(children[x].argposition))
            { 
				state.output.error("" + index + ": child #"+x+" argposition is negative");
				return index+1;
			}
			if (!isRightPosition(children[x].argposition, x))
            { 
				state.output.error("" + index + ": child #"+x+" argposition does not match position in the children array"); 
				return index+1;
			}
		}
		
		//do we have valid constraints
		if(constraints < 0 || constraints >= initializer.numNodeConstraints)
		{
			state.output.error("" + index + ": Preposterous node constraints (" + constraints + ")");
			return index + 1;
		}
		
		//am i swap-compatable with my parent?
		for(int i = 0; i < parent.length; ++i)
		{
			if(parent[i] instanceof GNPNode && !constraints(initializer).returntype.compatibleWith(initializer, ((GNPNode)(parent[i])).constraints(initializer).childtypes[argposition[i]]))
			{
				state.output.error("" + index + ": Incompatable GP type between me and my parent"); 
				return index+1;
			}
			if(parent[i] instanceof GNPGraph && !constraints(initializer).returntype.compatibleWith(initializer, ((GNPGraph)(parent[i])).constraints(initializer).graphtype));
			{
				state.output.error("" + index + ": I am root, but incompatable GP type between me and my tree return type");
				return index + 1;
			}
		}
		
		//is my class in the GNPGunctionSet?
		GNPNode[] nodes = set.nodesByArity[constraints(initializer).returntype.type][children.length];
		boolean there = false;
		for(int x = 0; x < nodes.length; ++x)
		{
			if(nodes[x].getClass() == this.getClass())
			{
				there = true;
				break;
			}
		}
		if(!there)
		{
			state.output.error("" + index + ": I'm not in the function set.");
			return index + 1;
		}
		
		//otherwise we've passed, go to next node
		index++;
		for(int x = 0; x < children.length; ++x)
			index = children[x].verify(state, set, index);
		state.output.exitIfErrors();
		return index;
	}
	
	/** Return true if one of children's parent is this node */
	public boolean isParentThis(GNPNode children)
	{
		for(int i = 0; i < children.parent.length; ++i)
		{
			if(children.parent[i] == this)
				return true;
		}
		return false;
	}
	
	/** Return true if has position is negative */
	public boolean hasPositionNegative(byte argposition[])
	{
		for(int i = 0; i < argposition.length; ++i)
		{
			if(argposition[i] < 0)
				return true;
		}
		return false;
	}
	
	/** Return true if position is right */
	public boolean isRightPosition(byte argposition[], int position)
	{
		for(int i = 0; i < argposition.length; ++i)
		{
			if(argposition[i] == position)
				return true;
		}
		return false;
	}
	
	/** Returns true if I can swap into node's position. 
	 * Here id index the parent which we want to swap position*/
	public final boolean swapCompatibleWith(final GNPInitializer initializer, final GNPNode node, int id)
	{
		 // I'm atomically compatible with him; a fast check
		if (constraints(initializer).returntype==node.constraints(initializer).returntype)  // no need to check for compatibility
			return true;
		// I'm set compatible with his parent's swap-position
		GNPType type;
		if (node.parent[id] instanceof GNPNode)  // it's a GPNode
			type = ((GNPNode)(node.parent[id])).constraints(initializer).childtypes[node.argposition[id]];
		else
			type = ((GNPGraph)(node.parent[id])).constraints(initializer).graphtype;
		
		return constraints(initializer).returntype.compatibleWith(initializer, type);
	}
    /** Returns the number of nodes, constrained by g.test(...)
    in the subtree for which this GNPNode is root.  This might
    be sped up by caching the value.  O(n). */
	public int numNodes(final GNPNodeGatherer g)
    {
		int s=0;
		for(int x=0;x<children.length;x++) 
			s += children[x].numNodes(g);
		if(visit == false)
		{
			visit = true;
			return s + (g.test(this) ? 1 : 0);
		}
		return s;
    }
	
    /** Returns the number of nodes, constrained by nodesearch,
    in the subtree for which this GNPNode is root.
    This might be sped up by cacheing the value somehow.  O(n). */
	public int numNodes(final int nodesearch)
	{
		int s = 0; 
		for(int x = 0; x < children.length; ++x)
			s += children[x].numNodes(nodesearch);
		if(visit == false)
		{
			visit = true;
			return s + ((nodesearch==NODESEARCH_ALL || (nodesearch==NODESEARCH_TERMINALS && children.length==0) || 
					(nodesearch==NODESEARCH_NONTERMINALS && children.length>0)) ? 1 : 0);
		}
		return s;
	}
	
	/** Reset the visit of this node and it's children, this method always visit after
	 *  the method of numNodes() and depth(), etc.*/
	public void resetVisit()
	{
		visit = false;
		for(int i = 0; i < children.length; ++i)
			children[i].resetVisit();
	}
	
	/** Returns the depth of the graph */
	public int depth()
	{
		int d = 0;
		
		return depth(d);
	}
	
	public int depth(int d)
	{
		int len = children.length;
		int count = d;
		for(int i = 0; i < len; ++i)
		{
			++count;
			if(i == 0)
				minDepth = children[i].depth(count);
			else
				minDepth = minDepth < children[i].depth(count) ? minDepth : children[i].depth(count);
			count = d;
		}
		if(children.length == 0)
		{
			if(visit == false)
			{
				minDepth = count;
				visit = true;
			}
			else
				minDepth = minDepth > count ? count : minDepth;
		}
		return minDepth;
	}
	
	/** Return the path length of the graph, which is the sum of all paths from all nodes to the root */
	public int pathLength(int nodesearch)
	{
		return pathLength(NODESEARCH_ALL, 0);
	}
	
	public int pathLength(int nodesearch, int currentDepth)
	{
		int sum = currentDepth;
		if(nodesearch == NODESEARCH_NONTERMINALS && children.length == 0 ||
			nodesearch == NODESEARCH_TERMINALS && children.length > 0)
			sum = 0;
		for(int x = 0; x < children.length; ++x)
		{
			if(visit == false)
			{
				path = pathLength(nodesearch, currentDepth + 1);
				sum += path;
				visit = true;
			}
			else
			{
				sum += path < pathLength(nodesearch, currentDepth + 1) ? 0 : (pathLength(nodesearch, currentDepth + 1) - path);
				path = path < pathLength(nodesearch, currentDepth + 1) ? path : pathLength(nodesearch, currentDepth + 1);
			}
		}
		return sum;
	}
	
	/** Returns the mean depth of the graph */
	int meanDepth(int nodesearch)
	{
		return pathLength(nodesearch) / numNodes(nodesearch);
	}
	
	/** Returns the depth at which I appear in the graph */
	public int atDepth()
	{
		int count = 0;
		
		return atDepth(count);
	}
	
	public int atDepth(int count)
	{
		int len = count;
		int minLen = count;
		for(int i = 0; i < parent.length; ++i)
		{
			if(parent[i] instanceof GNPNode)
			{
				++len;
				if(i == 0)
					minLen = ((GNPNode)parent[i]).atDepth(len);
				else
					minLen = minLen < ((GNPNode)parent[i]).atDepth(len) ? minLen : ((GNPNode)parent[i]).atDepth(len); 
				len = count;
			}
		}
		return minLen;
	}
	
	
	/** Returns the p'th node, constrained by nodesearch,
    in the subgraph for which this GNPNode is root.
    Use numNodes(nodesearch) to determine the total number.  
    g.test(...) is used as the constraining predicate.
    p ranges from 0 to this number minus 1. O(n). The
    resultant node is returned in <i>g</i>.*/
	public GNPNode nodeInPosition(int p, GNPNodeGatherer g)
	{
		nodeInPosition(p, g, NODESEARCH_CUSTOM);
		return g.node;
	}
	
    /** Returns the p'th node, constrained by nodesearch,
    in the subgraph for which this GNPNode is root.
    Use numNodes(nodesearch) to determine the total number.  
    g.test(...) is used as the constraining predicate.
    p ranges from 0 to this number minus 1. O(n). The
    resultant node is returned in <i>g</i>.*/
	public GNPNode nodeInPosition(int p, int nodesearch)
	{
		GNPNodeGatherer g = new GNPNodeGatherer(){
			public boolean test(GNPNode node)
			{
				return true;
			}
		};
		nodeInPosition(p, g, nodesearch);
		return g.node;
	}
	
	/** Returns the p'th node, constrained by nodesearch,
    in the subgraph for which this GNPNode is root.
    Use numNodes(nodesearch) to determine the total number.  Or if
    you used numNodes(g), then when
    nodesearch == NODESEARCH_CUSTOM, g.test(...) is used
    as the constraining predicate.
    p ranges from 0 to this number minus 1. O(n). The
    resultant node is returned in <i>g</i>.*/
	int nodeInPosition(int p, final GNPNodeGatherer g, final int nodesearch)
	{
		// am I of the type I'm looking for?
        if (nodesearch==NODESEARCH_ALL ||
            (nodesearch==NODESEARCH_TERMINALS && children.length==0) ||
            (nodesearch==NODESEARCH_NONTERMINALS && children.length>0) ||
            (nodesearch==NODESEARCH_CUSTOM && g.test(this)))
        {
	            // is the count now at 0?  Is it me?
	            if (p==0)
	                {
	                g.node = this; 
	                return -1; // found it
	                }
	            // if it's not me, drop the count by 1
	            else p--;
        }
        
        // regardless, check my children if I've not returned by now
        for(int x=0;x<children.length;x++)
        {
            p = children[x].nodeInPosition(p,g,nodesearch);
            if (p==-1) return -1; // found it
        }
        return p;
	}
	
	/** Return the root ancestor of this node */
	public GNPNodeParent rootParent()
	{
		GNPNodeParent cparent = this;
		while(cparent != null && cparent instanceof GNPNode)
			cparent = ((GNPNode)cparent).parent[0];
		return cparent;
	}
	
	/** Return true if the subgraph rooted at this node contains subnode */
	public boolean contains(final GNPNode subnode)
	{
		if(subnode == this)
			return true;
		for(int x = 0; x < children.length; ++x)
			if(children[x].contains(subnode))
				return true;
		return false;
	}
	
	 /** Starts a node in a new life immediately after it has been cloned.
    The default version of this function does nothing.  The purpose of
    this function is to give ERCs a chance to set themselves to a new
    random value after they've been cloned from the prototype.
    You should not assume that the node is properly connected to other
    nodes in the graph at the point this method is called. */
	public void resetNode(final EvolutionState state, final int thread) {}
	
	/** A convenience function for identifying a GNPNode in an error message */
	public String errorInfo()
	{
		return "GNPNode" + toString() + "in the function set for graph" + ((GNPGraph)(rootParent())).graphNumber();
	}
	
	public GNPNode lightClone()
	{
		try{
			GNPNode obj = (GNPNode)(super.clone());
			int len = children.length;
            if (len == 0) 
            	obj.children = children;  // we'll share arrays -- probably just using GPNodeConstraints.zeroChildren anyway
            else
            	obj.children = new GNPNode[len];
            return obj;
		}
		catch(CloneNotSupportedException e)
		{
			throw new InternalError();
		}
		
	}
	
	public Object clone()
	{
		GNPNode newnode = (GNPNode)lightClone();
		for(int x = 0; x < children.length; ++x)
		{
			newnode.children[x] = (GNPNode)(children[x].cloneReplacing());
			int PLen = newnode.children[x].parent.length;
			newnode.children[x].parent[PLen] = newnode;
			newnode.children[x].argposition[PLen] = (byte)x;
		}
		return newnode;
	}
	
    /** Deep-clones the graph rooted at this node, and returns the entire
    copied graph.  The result has everything set except for the root
    node's parent and argposition.  This method is identical to
    cloneReplacing for historical reasons, except that it returns
    the object as a GNPNode, not an Object. 
    @deprecated use clone() instead.
*/   
	public final GNPNode cloneReplacing()
	{
		return (GNPNode)clone();
	}
	
	public final GNPNode cloneReplacing(final GNPNode newSubgraph, final GNPNode oldSubgraph)
	{
		if(this == oldSubgraph)
			return newSubgraph.cloneReplacing();
		else
		{
			GNPNode newnode = (GNPNode)(lightClone());
            for(int x=0;x<children.length;x++)
            {
	            newnode.children[x] = (GNPNode)(children[x].cloneReplacing(newSubgraph,oldSubgraph)); 
	            // if you think about it, the following CAN'T be implemented by
	            // the children's clone method.  So it's set here.
	            int PLen = newnode.children[x].parent.length;
	            newnode.children[x].parent[PLen] = newnode;
	            newnode.children[x].argposition[PLen] = (byte)x;
            }
            return newnode;
		}
	}
	
    public final GNPNode cloneReplacingNoSubclone(final GNPNode newSubgraph, final GNPNode oldSubgraph) 
    {
    if (this==oldSubgraph)
        {
    		return newSubgraph;
        }
    else
        {
	        GNPNode newnode = (GNPNode)(lightClone());
	        for(int x=0;x<children.length;x++)
	        {
	            newnode.children[x] = (GNPNode)(children[x].cloneReplacingNoSubclone(newSubgraph,oldSubgraph)); 
	            // if you think about it, the following CAN'T be implemented by
	            // the children's clone method.  So it's set here.
	            int PLen = newnode.children[x].parent.length;
	            newnode.children[x].parent[PLen] = newnode;
	            newnode.children[x].argposition[PLen] = (byte)x;
	        }
	        return newnode;     
        }
    }
    
    public final GNPNode cloneReplacing(final GNPNode[] newSubgraphs, final GNPNode[] oldSubgraphs) 
    {
	    // am I a candidate?
	    int candidate = -1;
	    for(int x=0;x<oldSubgraphs.length;x++)
	        if (this==oldSubgraphs[x]) { candidate=x; break; }
	
	    if (candidate >= 0)
	        return newSubgraphs[candidate].cloneReplacing(newSubgraphs,oldSubgraphs);
	    else
	        {
	        GNPNode newnode = (GNPNode)(lightClone());
	        for(int x=0;x<children.length;x++)
	            {
	            newnode.children[x] = (GNPNode)(children[x].cloneReplacing(newSubgraphs,oldSubgraphs)); 
	            // if you think about it, the following CAN'T be implemented by
	            // the children's clone method.  So it's set here.
	            int PLen = newnode.children[x].parent.length;
	            newnode.children[x].parent[PLen] = newnode;
	            newnode.children[x].argposition[PLen] = (byte)x;
	            }
	        return newnode;     
	        }
    }
    
    /** Clones a new subgraph, but with the single node oldNode 
    (which may or may not be in the subgraph) 
    replaced with a newNode (not a clone of newNode).  
    These nodes should be
    type-compatible both in argument and return types, and should have
    the same number of arguments obviously.  This function will <i>not</i>
    check for this, and if they are not the result is undefined. */


	public final GNPNode cloneReplacingAtomic(final GNPNode newNode, final GNPNode oldNode) 
	{
	    int numArgs;
	    GNPNode curnode;
	    if (this==oldNode)
	    {
	        numArgs = Math.max(newNode.children.length,children.length);
	        curnode = newNode;
	    }
	    else
	    {
	        numArgs = children.length;
	        curnode = (GNPNode)lightClone();
	    }
	
	    // populate
	
	    for(int x=0;x<numArgs;x++)
	    {
	        curnode.children[x] = (GNPNode)(children[x].cloneReplacingAtomic(newNode,oldNode)); 
	        // if you think about it, the following CAN'T be implemented by
	        // the children's clone method.  So it's set here.
	        int PLen = curnode.children[x].parent.length;
	        curnode.children[x].parent[PLen] = curnode;
	        curnode.children[x].argposition[PLen] = (byte)x;
	    }
	    return curnode;
	}
	
	
	
	
	
	/** Clones a new subgraph, but with each node in oldNodes[] respectively
	    (which may or may not be in the subgraph) replaced with
	    the equivalent
	    nodes in newNodes[] (and not clones).  
	    The length of oldNodes[] and newNodes[] should
	    be the same of course.  These nodes should be
	    type-compatible both in argument and return types, and should have
	    the same number of arguments obviously.  This function will <i>not</i>
	    check for this, and if they are not the result is undefined. */
	
	
	public final GNPNode cloneReplacingAtomic(final GNPNode[] newNodes, final GNPNode[] oldNodes) 
	{
	    int numArgs;
	    GNPNode curnode;
	    int found = -1;
	    
	    for(int x=0;x<newNodes.length;x++)
	    {
	        if (this==oldNodes[x]) { found=x; break; }
	    }
	
	    if (found > -1)
	    {
	        numArgs = Math.max(newNodes[found].children.length,
	            children.length);
	        curnode = newNodes[found];
	    }
	    else
	    {
	        numArgs = children.length;
	        curnode = (GNPNode)lightClone();
	    }
	
	    // populate
	
	    for(int x=0;x<numArgs;x++)
	    {
	        curnode.children[x] = (GNPNode)(children[x].cloneReplacingAtomic(newNodes,oldNodes)); 
	        // if you think about it, the following CAN'T be implemented by
	        // the children's clone method.  So it's set here.
	        int PLen = curnode.children[x].parent.length;
	        curnode.children[x].parent[PLen] = curnode;
	        curnode.children[x].argposition[PLen] = (byte)x;
	    }
	    return curnode;
	}
	
	/** Replaces the node with another node in its position in the graph. 
    newNode should already have been cloned and ready to go.
    We presume that the other node is type-compatible and
    of the same arity (these things aren't checked).  */
    
	public final void replaceWith(final GNPNode newNode)
	{
	    // copy the parent and argposition
	    newNode.parent = parent;
	    newNode.argposition = argposition;
	    
	    // replace the parent pointer
	    for(int i = 0; i < newNode.parent.length; ++i) 
	    {
		    if (parent[i] instanceof GNPNode)
		        ((GNPNode)(parent[i])).children[argposition[i]] = newNode; //argposition[i] store the position in ith parent
		    else
		        ((GNPGraph)(parent[i])).child = newNode;
	    }
	        
	    // replace the child pointers
	    for(byte x = 0;x<children.length;x++)
	    {
	        newNode.children[x] = children[x];
	        int len = children[x].parent.length;
	        for(int i = 0; i < len; ++i)
	        {
	        	if(children[x].parent[i].equals(this))
	        	{
			        newNode.children[x].parent[i] = newNode;
			        newNode.children[x].argposition[i] = x;
			        break;
	        	}
	        }
	    }
	}
	
	/** Returns true if I and the provided node are the same kind of
	    node -- that is, we could have both been cloned() and reset() from
	    the same prototype node.  The default form of this function returns
	    true if I and the node have the same class, the same length children
	    array, and the same constraints.  You may wish to override this in
	    certain circumstances.   Here's an example of how nodeEquivalentTo(node)
	    differs from nodeEquals(node): two ERCs, both of
	    the same class, but one holding '1.23' and the other holding '2.45', which
	    came from the same prototype node in the same function set.
	    They should NOT be nodeEquals(...) but *should* be nodeEquivalent(...). */
	public boolean nodeEquivalentTo(GNPNode node)
	{
	    return (this.getClass().equals(node.getClass()) && 
	        children.length == node.children.length &&
	        constraints == node.constraints);
	}
	
	
	/** Returns a hashcode usually associated with all nodes that are 
	    equal to you (using nodeEquals(...)).  The default form
	    of this method returns the hashcode of the node's class.
	    ERCs in particular probably will want to override this method.
	*/
	public int nodeHashCode()
	{
	    return (this.getClass().hashCode());
	}
	
	/** Returns a hashcode associated with all the nodes in the graph.  
    The default version adds the hash of the node plus its child
    graphs, rotated one-off each time, which seems reasonable. */
	public int rootedGraphHashCode()
	{
		int hash = nodeHashCode();
		
		for(int x = 0; x < children.length; ++x)
			hash = (hash << 1 | hash >>> 31) ^ children[x].rootedGraphHashCode();
		return hash;
	}
	
	public boolean nodeEquals(final GNPNode node)
	{
		return nodeEquivalentTo(node);
	}
	
	/** Returns true if the two rooted graphs are "genetically" equal*/
	public boolean rootedGraphEquals(final GNPNode node)
	{
		if(!nodeEquals(node))
			return false;
		for(int x = 0; x < children.length; ++x)
			if(!(children[x].rootedGraphEquals(node.children[x])))
				return false;
		return true;
	}
	
	 /** Prints out a human-readable and Lisp-like atom for the node, 
    and returns the number of bytes in the string that you sent
    to the log (use print(),
    not println()).  The default version gets the atom from
    toStringForHumans(). 
	  */
	public int printNodeForHumans(final EvolutionState state,
	    final int log)
	{
	    return printNodeForHumans(state, log, Output.V_VERBOSE);
	}
	
	/** Prints out a human-readable and Lisp-like atom for the node, 
    and returns the number of bytes in the string that you sent
    to the log (use print(),
    not println()).  The default version gets the atom from
    toStringForHumans(). 
    @deprecated Verbosity no longer has an effect. 
	 */
	public int printNodeForHumans(final EvolutionState state,
	    final int log, 
	    final int verbosity)
	{
	    String n = toStringForHumans();
	    state.output.print(n,log);
	    return n.length();
	}
	
    /** Prints out a COMPUTER-readable and Lisp-like atom for the node, which
    is also suitable for readNode to read, and returns
    the number of bytes in the string that you sent to the log (use print(),
    not println()).  The default version gets the atom from toString().
    O(1). 
     */
	public int printNode(final EvolutionState state, final int log)
	{
	    printNode(state, log, Output.V_VERBOSE);
	    String n = toString();
	    return n.length();
	}
	
	/** Prints out a COMPUTER-readable and Lisp-like atom for the node, which
    is also suitable for readNode to read, and returns
    the number of bytes in the string that you sent to the log (use print(),
    not println()).  The default version gets the atom from toString().
    O(1). 
    @deprecated Verbosity no longer has an effect. 
	 */
	public int printNode(final EvolutionState state, final int log, 
	    final int verbosity)
	{
	    String n = toString();
	    state.output.print(n,log);
	    return n.length();
	}
	
	/** Prints out a COMPUTER-readable and Lisp-like atom for the node, which
    is also suitable for readNode to read, and returns
    the number of bytes in the string that you sent to the log (use print(),
    not println()).  The default version gets the atom from toString().
    O(1). */

	public int printNode(final EvolutionState state,
	    final PrintWriter writer)
	{
	    String n = toString();
	    writer.print(n);
	    return n.length();
	}

	/** Returns a Lisp-like atom for the node and any nodes of the same class.
    This will almost always be identical to the result of toString() (and the default
    does exactly this), but for ERCs it'll be different: toString will include the
    encoded constant data, whereas name() will not include this information and will
    be the same for all ERCs of this type.  If two nodes are nodeEquivalentTo(...)
    each other, then they will have the same name().  If two nodes are nodeEquals(...)
    each other, then they will have the same toString().  */
            
	public String name() 
	{ 
		return toString(); 
	}
	
	/** Returns a Lisp-like atom for the node which can be read in again by computer.
    If you need to encode an integer or a float or whatever for some reason
    (perhaps if it's an ERC), you should use the ec.util.Code library.  */

	public abstract String toString();

	/** Returns a Lisp-like atom for the node which is intended for human
    consumption, and not to be read in again.  The default version
    just calls toString(). */

	public String toStringForHumans() 
	{ 
		return toString(); 
	}
	
	/** Returns a description of the node that can make it easy to identify
    in error messages (by default, at least its name and the graph it's found in).
    It's okay if this is a reasonably expensive procedure -- it won't be called
    a lot.  */
	
	public String toStringForError()
	{
		GNPGraph rootp = (GNPGraph)rootParent();
		if(rootp != null)
		{
			int tnum = ((GNPGraph)(rootParent())).graphNumber();
			return toString() + (tnum == GNPGraph.NO_GRAPHNUM ? "" : "in  graph" + tnum);
		}
		else
			return toString();
	}
	
	 /** Produces the Graphviz code for a Graphviz graph of the subgraph rooted at this node.
    For this to work, the output of toString() must not contain a double-quote. 
    Note that this isn't particularly efficient and should only be used to generate
    occasional graphs for display, not for storing individuals or sending them over networks. */
	
	public String makeGraphvizGraph()
	{
		return "digraph g {\nnode [shape=rectangle];\n" + makeGraphvizSubgraph("n") + "}\n";
	}
	
	/** Produces the inner code for a graphviz subgraph.  Called from makeGraphvizgraph(). 
    Note that this isn't particularly efficient and should only be used to generate
    occasional graphs for display, not for storing individuals or sending them over networks. */
	
	protected String makeGraphvizSubgraph(String prefix)
	{
		String body = prefix + "[label = \"" + toStringForHumans() + "\"];\n";
		for(int x = 0; x < children.length; ++x)
		{
			String newprefix;
			if(x < 10)
				newprefix = prefix + x;
			else
				newprefix = prefix + "n" + x;
			body = body + children[x].makeGraphvizSubgraph(newprefix);
			body = body + prefix + " -> " + newprefix + ";\n";
		}
		return body;
	}
	
	 /** Produces the LaTeX code for a LaTeX graph of the subgraph rooted at this node, using the <tt>epic</tt>
    and <tt>fancybox</tt> packages, as described in sections 10.5.2 (page 307) 
    and 10.1.3 (page 278) of <i>The LaTeX Companion</i>, respectively.  For this to
    work, the output of toStringForHumans() must not contain any weird latex characters, notably { or } or % or \,
    unless you know what you're doing. See the documentation for ec.GNP.GNPGraph for information
    on how to take this code snippet and insert it into your LaTeX file. 
    Note that this isn't particularly efficient and should only be used to generate
    occasional graphs for display, not for storing individuals or sending them over networks. */

public String makeLatexgraph()
    {
    if (children.length==0)
        return "\\GNPbox{"+toStringForHumans()+"}";
        
    String s = "\\begin{bundle}{\\GNPbox{"+toStringForHumans()+"}}";
    for(int x=0;x<children.length;x++)
        s = s + "\\chunk{"+children[x].makeLatexgraph()+"}";
    s = s + "\\end{bundle}";
    return s;
    }
    
/** Producess a String consisting of the graph in pseudo-C form, given that the parent already will wrap the
    expression in parentheses (or not).  In pseudo-C form, functions with one child are printed out as a(b), 
    functions with more than two children are printed out as a(b, c, d, ...), and functions with exactly two
    children are either printed as a(b, c) or in operator form as (b a c) -- for example, (b * c).  Whether
    or not to do this depends on the setting of <tt>useOperatorForm</tt>.  Additionally, terminals will be
    printed out either in variable form -- a -- or in zero-argument function form -- a() -- depending on
    the setting of <tt>printTerminalsAsVariables</tt>.
    Note that this isn't particularly efficient and should only be used to generate
    occasional graphs for display, not for storing individuals or sending them over networks. 
*/
            
public String makeCgraph(boolean parentMadeParens, boolean printTerminalsAsVariables, boolean useOperatorForm)
    {
    if (children.length==0)
        return (printTerminalsAsVariables ? toStringForHumans() : toStringForHumans() + "()");
    else if (children.length==1)
        return toStringForHumans() + "(" + children[0].makeCgraph(true, printTerminalsAsVariables, useOperatorForm) + ")";
    else if (children.length==2 && useOperatorForm)
        return (parentMadeParens ? "" : "(") + 
            children[0].makeCgraph(false, printTerminalsAsVariables, useOperatorForm) + " " + 
            toStringForHumans() + " " + children[1].makeCgraph(false, printTerminalsAsVariables, useOperatorForm) + 
            (parentMadeParens ? "" : ")");
    else
        {
        String s = toStringForHumans() + "(" + children[0].makeCgraph(true, printTerminalsAsVariables, useOperatorForm);
        for(int x = 1; x < children.length;x++)
            s = s + ", " + children[x].makeCgraph(true, printTerminalsAsVariables, useOperatorForm);
        return s + ")";
        }
    }

/**
   Produces a graph for human consumption in Lisp form similar to that generated by printgraphForHumans().
   Note that this isn't particularly efficient and should only be used to generate
   occasional graphs for display, not for storing individuals or sending them over networks.
*/
public StringBuilder makeLispgraph(StringBuilder buf)
    {
    if (children.length==0)
        return buf.append(toStringForHumans());
    else
        {
        buf.append("(");
        buf.append(toStringForHumans());
        for(int x=0;x<children.length;x++)
            {
            buf.append(" ");
            children[x].makeLispgraph(buf);
            }
        buf.append(")");
        return buf;
        //return s + ")";
        }
    }

public String makeLispgraph()
    {
    return makeLispgraph(new StringBuilder()).toString();
    }


/** Prints out the graph on a single line, with no ending \n, in a fashion that can
    be read in later by computer. O(n).  
    You should call this method with printbytes == 0. 
*/

public int printRootedgraph(final EvolutionState state,
    final int log, int printbytes)
    {
    return printRootedgraph(state, log, Output.V_VERBOSE, printbytes);
    }


/** Prints out the graph on a single line, with no ending \n, in a fashion that can
    be read in later by computer. O(n).  
    You should call this method with printbytes == 0. 
    @deprecated Verbosity no longer has an effect.
*/

public int printRootedgraph(final EvolutionState state,
    final int log, final int verbosity,
    int printbytes)
    {
    if (children.length>0) { state.output.print(" (",verbosity,log); printbytes += 2; }
    else { state.output.print(" ",log); printbytes += 1; }

    printbytes += printNode(state,log);

    for (int x=0;x<children.length;x++)
        printbytes = children[x].printRootedgraph(state,log,printbytes);
    if (children.length>0) { state.output.print(")",log); printbytes += 1; }
    return printbytes;
    }


/** Prints out the graph on a single line, with no ending \n, in a fashion that can
    be read in later by computer. O(n).  Returns the number of bytes printed.
    You should call this method with printbytes == 0. */

public int printRootedgraph(final EvolutionState state, final PrintWriter writer,
    int printbytes)
    {
    if (children.length>0) { writer.print(" ("); printbytes += 2; }
    else { writer.print(" "); printbytes += 1; }

    printbytes += printNode(state,writer);

    for (int x=0;x<children.length;x++)
        printbytes = children[x].printRootedgraph(state,writer,printbytes);
    if (children.length>0) { writer.print(")"); printbytes += 1; }
    return printbytes;
    }


/** Prints out the graph in a readable Lisp-like multi-line fashion. O(n).  
    You should call this method with tablevel and printbytes == 0.  
    No ending '\n' is printed.  
*/

public int printRootedgraphForHumans(final EvolutionState state, final int log,
    int tablevel, int printbytes)
    {
    return printRootedgraphForHumans(state, log, Output.V_VERBOSE, tablevel, printbytes);
    }

/** Prints out the graph in a readable Lisp-like multi-line fashion. O(n).  
    You should call this method with tablevel and printbytes == 0.  
    No ending '\n' is printed.  
    @deprecated Verbosity no longer has an effect.
*/

public int printRootedgraphForHumans(final EvolutionState state, final int log,
    final int verbosity,
    int tablevel, int printbytes)
    {
    if (printbytes>MAXPRINTBYTES)
        { 
        state.output.print("\n",log);
        tablevel++;
        printbytes = 0;
        for(int x=0;x<tablevel;x++)
            state.output.print(GNPNODEPRINTTAB,log);
        }

    if (children.length>0) { state.output.print(" (",log); printbytes += 2; }
    else { state.output.print(" ",log); printbytes += 1; }

    printbytes += printNodeForHumans(state,log);

    for (int x=0;x<children.length;x++)
        printbytes = children[x].printRootedgraphForHumans(state,log,tablevel,printbytes);
    if (children.length>0) { state.output.print(")",log); printbytes += 1; }
    return printbytes;
    }


/** Reads the node symbol,
    advancing the DecodeReturn to the first character in the string
    beyond the node symbol, and returns a new, empty GNPNode of the
    appropriate class representing that symbol, else null if the
    node symbol is not of the correct type for your GNPNode class. You may
    assume that initial whitespace has been eliminated.  Generally should
    be case-SENSITIVE, unlike in Lisp.  The default
    version usually works for "simple" function names, that is, not ERCs
    or other stuff where you have to encode the symbol. */
public GNPNode readNode(DecodeReturn dret) 
    {
    int len = dret.data.length();

    // get my name
    String str2 = toString();
    int len2 = str2.length();

    if (dret.pos + len2 > len)  // uh oh, not enough space
        return null;

    // check it out
    for(int x=0; x < len2 ; x++)
        if (dret.data.charAt(dret.pos + x) != str2.charAt(x))
            return null;

    // looks good!  Check to make sure that
    // the symbol's all there is
    if (dret.data.length() > dret.pos+len2)
        {
        char c = dret.data.charAt(dret.pos+len2);
        if (!Character.isWhitespace(c) &&
            c != ')' && c != '(') // uh oh
            return null;
        }

    // we're happy!
    dret.pos += len2;
    return (GNPNode)lightClone();
    }


public void writeRootedgraph(final EvolutionState state,final GNPType expectedType,
    final GNPFunctionSet set, final DataOutput dataOutput) throws IOException
    {
    dataOutput.writeInt(children.length);
    boolean isTerminal = (children.length == 0);

    // identify the node
    GNPNode[] GNPfi = isTerminal ? 
        set.terminals[expectedType.type] : 
        set.nonterminals[expectedType.type];
    
    int index=0;
    for( /*int index=0 */; index <GNPfi.length;index++)
        if ((GNPfi[index].nodeEquivalentTo(this))) break;
    
    if (index==GNPfi.length)  // uh oh
        state.output.fatal("No node in the function set can be found that is equivalent to the node " + this +     
            " when performing writeRootedgraph(EvolutionState, GNPType, GNPFunctionSet, DataOutput).");
    dataOutput.writeInt(index);  // what kind of node it is
    writeNode(state,dataOutput);

    GNPInitializer initializer = ((GNPInitializer)state.initializer);        
    for(int x=0;x<children.length;x++)
        children[x].writeRootedgraph(state,constraints(initializer).childtypes[x],set,dataOutput);
    }


/*public static GNPNode readRootedgraph(final EvolutionState state,
    final DataInput dataInput,
    GNPType expectedType,
    GNPFunctionSet set,
    GNPNodeParent[] parent,
    int[] argposition) throws IOException
    {
    int len = dataInput.readInt();      // num children
    int index = dataInput.readInt();    // index in function set
    
    boolean isTerminal = (len == 0);
    GNPNode[] GNPfi = isTerminal ? 
        set.terminals[expectedType.type] : 
        set.nonterminals[expectedType.type];

    GNPNode node = ((GNPNode)(GNPfi[index].lightClone()));
    
    if (node.children == null || node.children.length != len)
        state.output.fatal("Mismatch in number of children (" + len + 
            ") when performing readRootedgraph(...DataInput...) on " + node);
    
    for(int i = 0; i < parent.length; ++i)
    {
	    node.parent[i] = parent[i];
	    node.argposition[i] = (byte)(argposition[i]);
    }
    node.readNode(state,dataInput);

    // do its children
    GNPInitializer initializer = ((GNPInitializer)state.initializer);        
    for(int x=0;x<node.children.length;x++)
        node.children[x] = readRootedgraph(state,dataInput,node.constraints(initializer).childtypes[x],set, node, x);

    return node;
    }*/

/** Override this to write any additional node-specific information to dataOutput besides: the number of arguments, 
    the specific node class, the children, and the parent.  The default version of this method does nothing. */
public void writeNode(final EvolutionState state, final DataOutput dataOutput) throws IOException
    {
    // do nothing
    }
    
/** Override this to read any additional node-specific information from dataInput besides: the number of arguments,
    the specific node class, the children, and the parent.  The default version of this method does nothing. */
public void readNode(final EvolutionState state, final DataInput dataInput) throws IOException
    {
    // do nothing
    }

/** Reads the node and its children from the form printed out by printRootedgraph. */
/*public static GNPNode readRootedgraph(int linenumber,
    DecodeReturn dret, 
    GNPType expectedType,
    GNPFunctionSet set,
    GNPNodeParent parent,
    int argposition,
    EvolutionState state) 
    {
    final char REPLACEMENT_CHAR = '@';

    // eliminate whitespace if any
    boolean isTerminal = true;
    int len = dret.data.length();
    for(  ;  dret.pos < len && 
              Character.isWhitespace(dret.data.charAt(dret.pos)) ; dret.pos++);
    
    // if I'm out of space, complain
    
    if (dret.pos >= len)
        state.output.fatal("Reading line " + linenumber + ": " + "Premature end of graph structure -- did you forget a close-parenthesis?\nThe graph was" + dret.data);
     
    // if I've found a ')', complain
    if (dret.data.charAt(dret.pos) == ')')
        {
        StringBuilder sb = new StringBuilder(dret.data);
        sb.setCharAt(dret.pos,REPLACEMENT_CHAR);
        dret.data = sb.toString();
        state.output.fatal("Reading line " + linenumber + ": " + "Premature ')' which I have replaced with a '" + REPLACEMENT_CHAR + "', in graph:\n" + dret.data);
        }
    
    // determine if I'm a terminal or not
    if (dret.data.charAt(dret.pos) == '(')
        {
        isTerminal=false;
        dret.pos++;
        // strip following whitespace
        for(  ;  dret.pos < len && 
                  Character.isWhitespace(dret.data.charAt(dret.pos)) ; dret.pos++);
        }
    
    // check again if I'm out of space
    
    if (dret.pos >= len)
        state.output.fatal("Reading line " + linenumber + ": " + "Premature end of graph structure -- did you forget a close-parenthesis?\nThe graph was" + dret.data);
    
    // check again if I found a ')'
    if (dret.data.charAt(dret.pos) == ')')
        {
        StringBuilder sb = new StringBuilder(dret.data);
        sb.setCharAt(dret.pos,REPLACEMENT_CHAR);
        dret.data = sb.toString();
        state.output.fatal("Reading line " + linenumber + ": " + "Premature ')' which I have replaced with a '" + REPLACEMENT_CHAR + "', in graph:\n" + dret.data);
        }
    
    
    // find that node!
    GNPNode[] GNPfi = isTerminal ? 
        set.terminals[expectedType.type] : 
        set.nonterminals[expectedType.type];
    
    GNPNode node = null;
    for(int x=0;x<GNPfi.length;x++)
        if ((node = GNPfi[x].readNode(dret)) != null) break;
    
    // did I find one?
    
    if (node==null)
        {
        if (dret.pos!=0) 
            {
            StringBuilder sb = new StringBuilder(dret.data);
            sb.setCharAt(dret.pos,REPLACEMENT_CHAR);
            dret.data = sb.toString();
            }
        else dret.data = "" + REPLACEMENT_CHAR + dret.data;
        state.output.fatal("Reading line " + linenumber + ": " + "I came across a symbol which I could not match up with a type-valid node.\nI have replaced the position immediately before the node in question with a '" + REPLACEMENT_CHAR + "':\n" + dret.data);
        }
    
    node.parent = parent;
    node.argposition = (byte)argposition;
    GNPInitializer initializer = ((GNPInitializer)state.initializer);
    
    // do its children
    for(int x=0;x<node.children.length;x++)
        node.children[x] = readRootedgraph(linenumber,dret,node.constraints(initializer).childtypes[x],set,node,x,state);
    
    // if I'm not a terminal, look for a ')'
    
    if (!isTerminal)
        {
        // clear whitespace
        for(  ;  dret.pos < len && 
                  Character.isWhitespace(dret.data.charAt(dret.pos)) ; dret.pos++);
        
        if (dret.pos >= len)
            state.output.fatal("Reading line " + linenumber + ": " + "Premature end of graph structure -- did you forget a close-parenthesis?\nThe graph was" + dret.data);
        
        if (dret.data.charAt(dret.pos) != ')')
            {
            if (dret.pos!=0) 
                {
                StringBuilder sb = new StringBuilder(dret.data);
                sb.setCharAt(dret.pos,REPLACEMENT_CHAR);
                dret.data = sb.toString();
                }
            else dret.data = "" + REPLACEMENT_CHAR + dret.data;
            state.output.fatal("Reading line " + linenumber + ": " + "A nonterminal node has too many arguments.  I have put a '" + 
                REPLACEMENT_CHAR + "' just before the offending argument.\n" + dret.data);
            }
        else dret.pos++;  // get rid of the ')'
        }
    
    // return the node
    return node;
    }*/


/** Evaluates the node with the given thread, state, individual, problem, and stack.
    Your random number generator will be state.random[thread].  
    The node should, as appropriate, evaluate child nodes with these same items
    passed to eval(...).

    <p>About <b>input</b>: <tt>input</tt> is special; it is how data is passed between
    parent and child nodes.  If children "receive" data from their parent node when
    it evaluates them, they should receive this data stored in <tt>input</tt>.
    If (more likely) the parent "receives" results from its children, it should
    pass them an <tt>input</tt> object, which they'll fill out, then it should
    check this object for the returned value.

    <p>A graph is typically evaluated by dropping a GNPData into the root.  When the
    root returns, the resultant <tt>input</tt> should hold the return value.

    <p>In general, you should not be creating new GNPDatas.  
    If you think about it, in most conditions (excepting ADFs and ADMs) you 
    can use and reuse <tt>input</tt> for most communications purposes between
    parents and children.  

    <p>So, let's say that your GNPNode function implements the boolean AND function,
    and expects its children to return return boolean values (as it does itself).
    You've implemented your GNPData subclass to be, uh, <b>BooleanData</b>, which
    looks like 
    
    * <tt><pre>public class BooleanData extends GNPData 
    *    {
    *    public boolean result;
    *    public GNPData copyTo(GNPData GNPd)
    *      {
    *      ((BooleanData)GNPd).result = result;
    *      }
    *    }</pre></tt>

    <p>...so, you might implement your eval(...) function as follows:

    * <tt><pre>public void eval(final EvolutionState state,
    *                     final int thread,
    *                     final GNPData input,
    *                     final ADFStack stack,
    *                     final GNPIndividual individual,
    *                     final Problem problem
    *    {
    *    BooleanData dat = (BooleanData)input;
    *    boolean x;
    *
    *    // evaluate the first child
    *    children[0].eval(state,thread,input,stack,individual,problem);
    *  
    *    // store away its result
    *    x = dat.result;
    *
    *    // evaluate the second child
    *    children[1].eval(state,thread,input,stack,individual,problem);
    *
    *    // return (in input) the result of the two ANDed
    *
    *    dat.result = dat.result && x;
    *    return;
    *    }
    </pre></tt>
*/

public abstract void eval(final EvolutionState state,
    final int thread,
    final GNPData input,
    final ADFStack stack,
    final GNPIndividual individual,
    final Problem problem);
}


	
	
	
	
