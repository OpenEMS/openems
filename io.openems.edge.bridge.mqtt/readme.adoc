# io.openems.edge.bridge.mqtt Provider

This Bundle Provides the MQTT Bridge. It Communicates with a Broker, Handles the Subscribe and Publish task.
Handles inflight messages and If Broker changes. --> QoS > 0 Handling.
Since only One Bridge is needed (Only one Broker) this Bridge is implemented with SingletonPattern.

Persistent or Clean Session will be configured automatically.

AutoReconnect is also handled automatically.

Two Examples are found in the dummy package.

