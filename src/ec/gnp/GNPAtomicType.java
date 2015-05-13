package ec.gnp;

public final class GNPAtomicType extends GNPType{
	
	public GNPAtomicType(final String n)
	{
		name = n;
	}
	
	public GNPAtomicType(){}
	
	public final boolean compatibleWith(final GNPInitializer initializer, final GNPType t)
	{
		if(t.type == type)
			return true;
		else if(t.type < initializer.numAtomicTypes)
			return false;
		else
			return ((GNPSetType)t).types_sparse[type];
	}
}
