package io.openems.edge.heat.askoma;

/**
 * Operating mode as configured via OSGi/Config. For the channel representation
 * (which additionally carries an {@code UNDEFINED} state) see
 * {@link ChannelMode}.
 */
public enum Mode {
	OFF, FAST_HEAT, SURPLUS;
}