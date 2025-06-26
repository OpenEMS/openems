package io.openems.common.types;

/**
 * Defines the type of an ElectricityMeter.
 * 
 * <p>
 * See "io.openems.edge.meter.api" for details.
 */
public enum MeterType {
	/**
	 * Defines a Grid-Meter, i.e. a meter that is measuring at the grid connection
	 * point (German: "Netzanschlusspunkt")
	 */
	GRID,
	/**
	 * Defines a Production-Meter, i.e. a meter that is measuring an electric
	 * producer like a photovoltaics installation
	 */
	PRODUCTION,
	/**
	 * Defines a Production+Consumption-Meter, i.e. a meter that is measuring
	 * something that is an electric producer and consumer at the same time, like a
	 * non-controlled, external energy storage system.
	 */
	PRODUCTION_AND_CONSUMPTION,
	/**
	 * Defines a Consumption-Meter that metered, i.e. a meter that is measuring an
	 * electric consumer like a heating-element or electric car.
	 *
	 * <p>
	 * Select this {@link MeterType} if the device is not actively managed by
	 * OpenEMS - see {@link #MANAGED_CONSUMPTION_METERED} otherwise.
	 *
	 * <p>
	 * Note: Consumption is generally calculated using the data from Grid-Meter,
	 * Production-Meter and charge/discharge of battery. The value of
	 * CONSUMPTION_METERED is _not added_ to this calculated consumption as it is
	 * expected to be already measured by the Grid-Meter.
	 */
	CONSUMPTION_METERED,
	/**
	 * Defines a Consumption-Meter that is actively managed by OpenEMS and metered
	 * (See {@link #CONSUMPTION_METERED}).
	 */
	MANAGED_CONSUMPTION_METERED,
	/**
	 * Defines a Consumption-Meter that is NOT metered, i.e. a meter that is
	 * measuring an electric consumer like a heating-element or electric car.
	 *
	 * <p>
	 * Note: Consumption is generally calculated using the data from Grid-Meter,
	 * Production-Meter and charge/discharge of battery. The value of
	 * CONSUMPTION_NOT_METERED is _added_ to this calculated consumption as it is
	 * expected to be NOT already measured by the Grid-Meter.
	 */
	CONSUMPTION_NOT_METERED;
}
