# io.openems.edge.wago Provider

${Bundle-Description}

This bundle creates automatically Read-/Write-Channels for all WAGO modules that are attached to a WAGO Fieldbus Coupler. It therefore reads and parses the "http://[IP-ADDRESS]/etc/ea-config.xml" file to get required information.

Example:
Component-ID of the WAGO-Component is "io0". The examples below refer to the first WAGO module. For second module the Channel is "DigitalOutputM2C1", for third its "DigitalOutputM3C1" and so on.


WAGO I/0 750-523

RelayM1 - the actual state of the Relay output; read and writable
RelayM1Hand - the 'manual' mode switch; read-only


WAGO I/O 750-501

DigitalOutputM1C1 - the actual state of the first digital output; read and writable
DigitalOutputM1C2 - the actual state of the second digital output; read and writable


WAGO I/O 750-400

DigitalInputM1C1 - the actual state of the first digital input; read-only
DigitalInputM1C2 - the actual state of the second digital input; read-only