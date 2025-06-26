package io.openems.edge.evcs.api;

/**
 * A {@link MetaEvcs} is a wrapper for physical electric vehicle charging
 * stations. It is not a physical {@link Evcs} itself. This is used to
 * distinguish e.g. an EvcsCluster from an actual Evcs.
 */
public interface MetaEvcs extends Evcs {

}
