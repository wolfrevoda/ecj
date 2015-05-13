package ec.gnp.batu;
import ec.gnp.GNPDefaults;
import ec.util.Parameter;
import ec.*;

public final class GNPBatuDefaults implements DefaultsForm
{
	public static final String P_BATU = "batu";
	
	public static final Parameter base()
	{
		return GNPDefaults.base().push(P_BATU);
	}

}
