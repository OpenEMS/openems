# io.openems.edge.bridge.mqtt Provider

This Bundle Provides the MQTT Bridge. It Communicates with a Broker, Handles the subscribe- and publishtasks.

Persistent or Clean Session will be configured automatically.

AutoReconnect is also handled automatically.

This Bridge only works atm with MQTT version 3_1_1.

The Bridge gets all the Tasks and asks the MqttComponent if their MQTT config changed.

