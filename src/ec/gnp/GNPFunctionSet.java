package ec.gnp;
import java.io.*;

import ec.*;
import ec.gnp.GNPNode;
import ec.util.*;

import java.util.*;

/**
 * 
 * @author Batu
 * version 1.0
 * GNPFunctionSet is a Clique which represents a set of GNPNode prototypes
 * forming a standard function set for forming certain trees in individuals.
 * GNPFunctionSets store their GNPNode Prototypes in three hashtables,
 * one for all nodes, one for nonterminals, and one for terminals.  Each
 * hashed item is an array of GNPNode objects,
 * hashed by the return type of the GNPNodes in the array.
 *
 */

public class GNPFunctionSet {

	 private static final long serialVersionUID = 1;

	 public final static String P_NAME = "name";
	 public final static String P_FUNC = "func";
	 public final static String P_SIZE = "size";

	 /** Name of the GNPFunctionSet */
	 public String name;

	 /** The nodes that our GNPGraph can use: arrays of nodes hashed by type. */
	 public Hashtable nodes_h;
	 
	 /** The nodes that our GNPGraph can use: nodes[type][thenodes]. */
	 public GNPNode[][] nodes;
	 /** The nonterminals our GNPGraph can use: arrays of nonterminals hashed by type. */
	 public Hashtable nonterminals_h;
	 /** The nonterminals our GNPGraph can use: nonterminals[type][thenodes]. */
	 public GNPNode[][] nonterminals;
	 /** The terminals our GNPGraph can use: arrays of terminals hashed by type. */
	 public Hashtable terminals_h;
	 /** The terminals our GNPGraph can use: terminals[type][thenodes]. */
	 public GNPNode[][] terminals;
	 
	 /** The nodes that our GNPGraph can use, hashed by name(). */
	 public Hashtable nodesByName;
	 
	 /** Nodes == a given arity, that is: nodesByArity[type][arity][thenodes] */
	 public GNPNode[][][]nodesByArity;

	 /** Nonterminals <= a given arity, that is: nonterminalsUnderArity[type][arity][thenodes] --
	 this will be O(n^2).  Obviously, the number of nonterminals at arity slot 0 is 0.*/
	 public GNPNode[][][]nonterminalsUnderArity;

	 /** Nonterminals >= a given arity, that is: nonterminalsOverArity[type][arity][thenodes] --
	 this will be O(n^2).  Obviously, the number of nonterminals at arity slot 0 is all the 
  	 nonterminals of that type. */
	 public GNPNode[][][]nonterminalsOverArity;
	 
	 /** Returns the name. */
	 public String toString() { return name; }
	 
	 /** Sets up all the GNPFunctionSet, loading them from the parameter
     file.  This must be called before anything is called which refers
     to a type by name. */

	 /** Sets up the arrays based on the hashtables */
	 
	 public void postProcessFunctionSet()
	 {
		 nodes = new GNPNode[nodes_h.size()][];
		 terminals = new GNPNode[terminals_h.size()][];
		 nonterminals = new GNPNode[nonterminals_h.size()][];
		 
		 
		 Enumeration e = nodes_h.keys();
		 while(e.hasMoreElements())
		 {
			 GNPType gnpt = (GNPType)(e.nextElement());
			 GNPNode[] gnpfi = (GNPNode[])(nodes_h.get(gnpt));
			 nodes[gnpt.type] = gnpfi;
		 }
		 e = nonterminals_h.keys();
		 while(e.hasMoreElements())
		 {
			 GNPType gnpt = (GNPType)(e.nextElement());
			 GNPNode[] gnpfi = (GNPNode[])(nonterminals_h.get(gnpt));
			 nonterminals[gnpt.type] = gnpfi;
		 }
		 e = terminals_h.keys();
		 while(e.hasMoreElements())
		 {
			 GNPType gnpt = (GNPType)(e.nextElement());
			 GNPNode[] gnpfi = (GNPNode[])(terminals_h.get(gnpt));
			 terminals[gnpt.type] = gnpfi;
		 }
		 
		// set up arity-based arrays
	    // first, determine the maximum arity
	    int max_arity=0;
	    for(int x=0;x<nodes.length;x++)
	    	for(int y=0;y<nodes[x].length;y++)
	    		if (max_arity < nodes[x][y].children.length)
	    			max_arity = nodes[x][y].children.length;
	    // next set up the == array
	    nodesByArity = new GNPNode[nodes.length][max_arity + 1][];
	    for(int x = 0; x < nodes.length; ++x)
	    	for(int a = 0; a <= max_arity; ++a)
	    	{
	    		//how many nodes do we have?
	    		int num_of_a = 0;
	    		for(int y = 0; y < nodes[x].length; ++y)
	    			if(nodes[x][y].children.length == a)
	    				num_of_a++;
	    		// allocate and fill
	    		nodesByArity[x][a] = new GNPNode[num_of_a];
	    		int cur_a = 0;
	    		for(int y = 0; y < nodes[x].length; ++y)
	    			if(nodes[x][y].children.length == a)
	    				nodesByArity[x][a][cur_a++] = nodes[x][y];
	    	}
	    // set up the <= nonterminals array
	    nonterminalsUnderArity = new GNPNode[nonterminals.length][max_arity + 1][];
        for(int x=0;x<nonterminals.length;x++)
        for (int a = 0;a <= max_arity; a++)
        {
        	// how many nonterminals do we have?
        	int num_of_a = 0;
            for(int y=0;y<nonterminals[x].length;y++)
            	if (nonterminals[x][y].children.length <= a) num_of_a++;
            // allocate and fill
            nonterminalsUnderArity[x][a] = new GNPNode[num_of_a];
            int cur_a = 0;
            for(int y=0;y<nonterminals[x].length;y++)
            	if (nonterminals[x][y].children.length <= a )
            		nonterminalsUnderArity[x][a][cur_a++] = nonterminals[x][y];
        }
        
        // now set up the >= nonterminals array
        nonterminalsOverArity = new GNPNode[nonterminals.length][max_arity + 1][];
        for(int x=0;x<nonterminals.length;x++)
        for (int a = 0;a <= max_arity; a++)
        {
        	// how many nonterminals do we have?
            int num_of_a = 0;
            for(int y=0;y<nonterminals[x].length;y++)
            	if (nonterminals[x][y].children.length >= a) num_of_a++;
            // allocate and fill
            nonterminalsOverArity[x][a] = new GNPNode[num_of_a];
            int cur_a = 0;
            for(int y=0;y<nonterminals[x].length;y++)
            	if (nonterminals[x][y].children.length >= a )
            		nonterminalsOverArity[x][a][cur_a++] = nonterminals[x][y];
        }

	 }
	 
	 /** Must be done <i>after</i> GPType and GPNodeConstraints have been set up */
	 
	 public void setup(final EvolutionState state, final Parameter base)
	 {
		 //what's my name?
		 name = state.parameters.getString(base.push(P_NAME), null);
		 if(name == null)
			 state.output.fatal("No name was given for this function set.", base.push(P_NAME));
		 
		 //register me
		 GNPFunctionSet old_functionset = (GNPFunctionSet)(((GNPInitializer)state.initializer).functionSetRepository.put(name,this));
		 if (old_functionset != null)
			 state.output.fatal("The GNPFunctionSet \"" + name + "\" has been defined multiple times.", base.push(P_NAME));		 
		 
		 //how many functions do i have?
		 int numFuncs = state.parameters.getInt(base.push(P_SIZE), null, 1);
		 if(numFuncs < 1)
			 state.output.error("The GNPFunctionSet \"" + name + "\" has no functions.", base.push(P_SIZE));
		 
		 nodesByName = new Hashtable();
		 
		 Parameter p = base.push(P_FUNC);
	     Vector tmp = new Vector();
	     
	     for(int x = 0; x < numFuncs; ++x)
	     {
	    	 //load
	    	 Parameter pp = p.push("" + x);
	    	 GNPNode gnpfi = (GNPNode)(state.parameters.getInstanceForParameter(pp, null, GNPNode.class));
	    	 gnpfi.setup(state, pp);
	    	 
	    	 //add to my collection
	    	 tmp.addElement(gnpfi);
	    	 
	    	 //load into the nodesByName hashtable
	    	 GNPNode[] nodes = (GNPNode[])(nodesByName.get(gnpfi.name()));
	    	 if(nodes == null)
	    		 nodesByName.put(gnpfi.name(), new GNPNode[] {gnpfi});
	    	 else
	    	 {
	    		 GNPNode[] nodes2 = new GNPNode[nodes.length + 1];
	    		 System.arraycopy(nodes, 0, nodes2, 0, nodes.length);
	    		 nodes2[nodes2.length - 1] = gnpfi;
	    		 nodesByName.put(gnpfi.name(), nodes2);
	    	 }
	     }
	     
	     //make my hash tables
	     nodes_h = new Hashtable();
	     terminals_h = new Hashtable();
	     nonterminals_h = new Hashtable();
	     
	     // Now set 'em up according to the types in GPType
		 Enumeration e = ((GNPInitializer)state.initializer).typeRepository.elements();
		 GNPInitializer initializer = ((GNPInitializer)state.initializer);
		 while(e.hasMoreElements())
		 {
			 GNPType typ = (GNPType)(e.nextElement());
			 
			 //make vectors for the type
			 Vector nodes_v = new Vector();
			 Vector terminals_v = new Vector();
			 Vector nonterminals_v = new Vector();
			 
			 //add GPNodes as appropriate to each vector
			 Enumeration v = tmp.elements();
			 while(v.hasMoreElements())
			 {
				 GNPNode i = (GNPNode)(v.nextElement());
				 if(typ.compatibleWith(initializer, i.constraints(initializer).returntype))
				 {
					 nodes_v.addElement(i);
					 if(i.children.length == 0)
						 terminals_v.addElement(i);
					 else
						 nonterminals_v.addElement(i);
				 }
			 }
			 
			 //turn nodes_h' vectors into arrays
			 GNPNode[] ii = new GNPNode[nodes_v.size()];
			 nodes_v.copyInto(ii);
			 nodes_h.put(typ, ii);
			 
			 //turn terminals_h' vectors into arrays
			 ii = new GNPNode[terminals_v.size()];
	         terminals_v.copyInto(ii);
	         terminals_h.put(typ,ii);
	         
	         //turn nonterminals_h' vectors into arrays
	         ii = new GNPNode[nonterminals_v.size()];
	         nonterminals_v.copyInto(ii);
	         nonterminals_h.put(typ,ii);
		 }
	     // I don't check to see if the generation mechanism will be valid here
	     // -- I check that in GPTreeConstraints, where I can do the weaker check
	     // of going top-down through functions rather than making sure that every
	     // single function has a compatible argument function (an unneccessary check)

	     state.output.exitIfErrors();  // because I promised when I called n.setup(...)

	     // postprocess the function set
	     postProcessFunctionSet();
	     
	 }
	 
	 /** Returns the function set for a given name.
     You must guarantee that after calling functionSetFor(...) one or
     several times, you call state.output.exitIfErrors() once. */
	 
	 public static GNPFunctionSet functionSetFor(final String functionSetName, final EvolutionState state)
	 {
		 GNPFunctionSet set = (GNPFunctionSet)(((GNPInitializer)state.initializer).functionSetRepository.get(functionSetName));
		 if(set == null)
			 state.output.error("The GNP function set \"" + functionSetName + "\" could not be found.");
		 return set;
	 }
	 
	 private void writeObject(ObjectOutputStream out) throws IOException
     {
        // this wastes an hashtable pointer, but what the heck.

        out.defaultWriteObject();
     }

	 private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
     {
        in.defaultReadObject();
     }
	 
}
