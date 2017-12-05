# Channels

As described in the [Architecture](doc/architecture.md) documentation, OpenEMS is providing the data of each connected "Thing" (Device, Controller,...) via Channels.

## Channel Address

Each channel in the system is addressed by a distinct channel address in the form `Thing-ID/Channel-ID`.

Thing-IDs are typically:
- `ess0` for the first storage system or battery inverter
- `ess1` for the second storage system or battery inverter
- ...
- `meter0` for the first meter in the system
- ...

Example: the State of charge ("Soc") of the first storage system has the channel address `ess0/Soc`.

## Available Channels

To find out which Channels are available, there are two ways:
- General purpose channels are defined in the DeviceNature
- All available channels are defined in the Device

For example a FENECON Pro energy storage system implements all generic channels from EssNature and many channels which are specific to a FENECON Pro system.

### ESS (Energy Storage System)

#### DeviceNature implementation

- [EssNature](../edge/src/io/openems/api/device/nature/ess/EssNature.java)
	- Soc
	- SystemState
	- AllowedCharge
	- AllowedDischarge
	- Capacity
	- ...
- [AsymmetricEssNature](../edge/src/io/openems/api/device/nature/ess/AsymmetricEssNature.java) extends EssNature
  - ActivePowerL1, ActivePowerL2, ActivePowerL3
  - ReactivePowerL1, ReactivePowerL2, ReactivePowerL3
  - ...
- [SymmetricEssNature](../edge/src/io/openems/api/device/nature/ess/SymmetricEssNature.java) extends EssNature
  - ActivePower
  - ReactivePower
  - ...

#### Device implementation

- [FENECON Mini (readonly)](../edge/src/io/openems/impl/device/minireadonly/FeneconMiniEss.java)
- [FENECON Pro](../edge/src/io/openems/impl/device/pro/FeneconProEss.java)
- [FENECON Commercial](../edge/src/io/openems/impl/device/commercial/FeneconCommercialEss.java)

### Meter

#### DeviceNature implementation

- [MeterNature](../edge/src/io/openems/api/device/nature/meter/MeterNature.java)
	- Type
- [AsymmetricMeterNature](../edge/src/io/openems/api/device/nature/meter/AsymmetricMeterNature.java) extends MeterNature
  - ActivePowerL1, ActivePowerL2, ActivePowerL3
  - ReactivePowerL1, ReactivePowerL2, ReactivePowerL3
  - ...
- [AsymmetricMeterNature](../edge/src/io/openems/api/device/nature/meter/SymmetricMeterNature.java) extends MeterNature
  - ActivePower
  - ...
  - ReactivePower

#### Device implementation

- [Socomec](../edge/src/io/openems/impl/device/socomec/SocomecMeter.java)
- [FENECON Pro production meter](../edge/src/io/openems/impl/device/pro/FeneconProPvMeter.java)

For more natures see [Implementation in Source Code](../edge/src/io/openems/api/device/nature).

## Default configurations for FENECON ESS

If you receive your OpenEMS together with a FENECON energy storage system, you will have the following Thing-IDs:

- FENECON Pro
  - `ess0`: [FENECON Pro](../edge/src/io/openems/impl/device/pro/FeneconProEss.java)
  - `meter0`: [Socomec grid meter](../edge/src/io/openems/impl/device/socomec/SocomecMeter.java)
  - `meter1`: [FENECON Pro production meter](../edge/src/io/openems/impl/device/pro/FeneconProPvMeter.java)
- FENECON Mini
  - `ess0`: [FENECON Mini](../edge/src/io/openems/impl/device/minireadonly/FeneconMiniEss.java)
  - `meter0`: [FENECON Mini grid meter](../edge/src/io/openems/impl/device/minireadonly/FeneconMiniGridMeter.java)
  - `meter1`: [FENECON Mini production meter](../edge/src/io/openems/impl/device/minireadonly/FeneconMiniProductionMeter.java)
