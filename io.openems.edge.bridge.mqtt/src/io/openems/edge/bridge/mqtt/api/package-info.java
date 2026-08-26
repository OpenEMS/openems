/**
 * OpenEMS Edge Bridge MQTT API.
 *
 * <p>
 * This package contains the public API for the MQTT bridge, including:
 * <ul>
 * <li>{@link io.openems.edge.bridge.mqtt.api.BridgeMqtt} - Main bridge interface
 * <li>{@link io.openems.edge.bridge.mqtt.api.MqttComponent} - Marker interface for MQTT-enabled components
 * <li>{@link io.openems.edge.bridge.mqtt.api.MqttVersion} - Supported MQTT protocol versions
 * <li>{@link io.openems.edge.bridge.mqtt.api.QoS} - Quality of Service levels
 * <li>{@link io.openems.edge.bridge.mqtt.api.MqttMessage} - Message record
 * </ul>
 */
@org.osgi.annotation.versioning.Version("1.0.0")
@org.osgi.annotation.bundle.Export
package io.openems.edge.bridge.mqtt.api;
