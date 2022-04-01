package io.openems.edge.ess.api;

/**
 * A MetaEss is a wrapper for physical energy storage systems. It is not a
 * physical Ess itself. This is used to distinguish e.g. an EssCluster from an
 * actual Ess.
 */
public interface MetaEss extends SymmetricEss {

	/**
	 * Get the Component-IDs of energy storage systems that are handled by this
	 * MetaEss.
	 *
	 * @return an array of Component-IDs
	 */
	public String[] getEssIds();

}
