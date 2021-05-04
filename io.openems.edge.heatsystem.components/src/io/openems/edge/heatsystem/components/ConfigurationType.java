package io.openems.edge.heatsystem.components;

/**
 * ConfigurationType. This will be used by Heatsystem components to check if it either has to use Devices or Channels.
 * E.g. a valve uses the "Relay" nature when Devices are configures. Doesn't matter when Channels are configured.
 */
public enum ConfigurationType {
    CHANNEL, DEVICE
}
