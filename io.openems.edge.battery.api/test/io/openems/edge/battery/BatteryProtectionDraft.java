package io.openems.edge.battery;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.linecharacteristic.PolyLine;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.common.type.TypeUtils;

/*
 * All Voltage in [mV]. All Percentage in range [0,1]. All Temperature in
 * [degC].
 */

public class BatteryProtectionDraft {

	private static class MaxChargeCurrentHandler {
		public static class Builder {
			private final ClockProvider clockProvider;
			private final int bmsMaxEverAllowedChargeCurrent;
			private PolyLine voltageToPercent = PolyLine.empty();
			private PolyLine temperatureToPercent = PolyLine.empty();
			private double maxIncreasePerSecond = Double.MAX_VALUE;

			private Builder(ClockProvider clockProvider, int bmsMaxEverAllowedChargeCurrent) {
				this.clockProvider = clockProvider;
				this.bmsMaxEverAllowedChargeCurrent = bmsMaxEverAllowedChargeCurrent;
			}

			public Builder setVoltageToPercent(PolyLine voltageToPercent) {
				this.voltageToPercent = voltageToPercent;
				return this;
			}

			public Builder setTemperatureToPercent(PolyLine temperatureToPercent) {
				this.temperatureToPercent = temperatureToPercent;
				return this;
			}

			public Builder setMaxIncreasePerSecond(double maxIncreasePerSecond) {
				this.maxIncreasePerSecond = maxIncreasePerSecond;
				return this;
			}

			public MaxChargeCurrentHandler build() {
				return new MaxChargeCurrentHandler(this.clockProvider, this.bmsMaxEverAllowedChargeCurrent,
						this.voltageToPercent, this.temperatureToPercent, this.maxIncreasePerSecond);
			}
		}

		/**
		 * Create a MaxChargeCurrentHandler builder.
		 * 
		 * @param clockProvider a {@link ClockProvider}
		 * @return a {@link Builder}
		 */
		public static Builder create(ClockProvider clockProvider, int bmsMaxEverAllowedChargeCurrent) {
			return new Builder(clockProvider, bmsMaxEverAllowedChargeCurrent);
		}

		private final ClockProvider clockProvider;
		private final int bmsMaxEverAllowedChargeCurrent;
		private final PolyLine voltageToPercent;
		private final PolyLine temperatureToPercent;
		private final double maxIncreasePerSecond;

		private Instant lastResultTimestamp = null;
		private Double lastResultLimit = null;

		public MaxChargeCurrentHandler(ClockProvider clockProvider, int bmsMaxEverAllowedChargeCurrent,
				PolyLine voltageToPercent, PolyLine temperatureToPercent, double maxIncreasePerSecond) {
			this.clockProvider = clockProvider;
			this.bmsMaxEverAllowedChargeCurrent = bmsMaxEverAllowedChargeCurrent;
			this.voltageToPercent = voltageToPercent;
			this.temperatureToPercent = temperatureToPercent;
			this.maxIncreasePerSecond = maxIncreasePerSecond;
		}

		private synchronized int calculateCurrentLimit(int minCellVoltage, int maxCellVoltage, int minCellTemperature,
				int maxCellTemperature, int bmsAllowedChargeCurrent) {
			// Get the minimum limit of all limits in Ampere
			double limit = TypeUtils.min(//

					// Original 'AllowedChargeCurrent' by the BMS
					Double.valueOf(bmsAllowedChargeCurrent),

					// Calculate Ampere limit for Min-Cell-Voltage
					this.percentToAmpere(this.voltageToPercent.getValue(minCellVoltage)),

					// Calculate Ampere limit for Max-Cell-Voltage
					this.percentToAmpere(this.voltageToPercent.getValue(maxCellVoltage)),

					// Calculate Ampere limit for Min-Cell-Temperature
					this.percentToAmpere(this.temperatureToPercent.getValue(minCellTemperature)),

					// Calculate Ampere limit for Min-Cell-Temperature
					this.percentToAmpere(this.temperatureToPercent.getValue(maxCellTemperature)), //

					// Calculate Max Increase Ampere Limit
					this.getMaxIncreaseAmpereLimit() //
			);

			this.lastResultLimit = limit;

			return (int) Math.round(limit);
		}

		/**
		 * Calculates the maximum increase limit in Ampere from the
		 * 'maxIncreasePerSecond' parameter.
		 * 
		 * <p>
		 * If maxIncreasePerSecond is 0.5, last limit was 10 A and 1 second passed, this
		 * method returns 10.5.
		 * 
		 * @return the limit or null
		 */
		private synchronized Double getMaxIncreaseAmpereLimit() {
			Instant now = Instant.now(this.clockProvider.getClock());
			final Double result;
			if (this.lastResultTimestamp != null && this.lastResultLimit != null) {
				result = this.lastResultLimit
						+ (Duration.between(this.lastResultTimestamp, now).toMillis() * maxIncreasePerSecond) //
								/ 1000.; // convert [mA] to [A]
			} else {
				result = null;
			}
			this.lastResultTimestamp = now;
			return result;
		}

		/**
		 * Convert a Percent value to a concrete Ampere value in [A] by multiplying it
		 * with 'bmsMaxEverAllowedChargeCurrent'.
		 * 
		 * <ul>
		 * <li>null % -> null
		 * <li>0 % -> 0
		 * <li>anything else -> calculate percent; at least '1 A'.
		 * 
		 * @param percent the percent value in [0,1]
		 * @return the ampere value in [A]
		 */
		private Double percentToAmpere(Double percent) {
			if (percent == null) {
				return null;
			} else if (percent == 0D) {
				return 0D;
			} else {
				return Math.max(1D, this.bmsMaxEverAllowedChargeCurrent * percent);
			}
		}

	}

	@Test
	public void test() {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		MaxChargeCurrentHandler maxChargeCurrentHandler = MaxChargeCurrentHandler.create(cm, 40) //
				.setVoltageToPercent(PolyLine.create() //
						.addPoint(Math.nextDown(3000), 0.1) //
						.addPoint(3000, 1) //
						.addPoint(3450, 1) //
						.addPoint(Math.nextDown(3650), 0.01) //
						.addPoint(3650, 0) //
						.build()) //
				.setTemperatureToPercent(PolyLine.create() //
						.addPoint(Math.nextDown(-10), 0) //
						.addPoint(-10, 0.215) //
						.addPoint(0, 0.215) //
						.addPoint(1, 0.325) //
						.addPoint(5, 0.325) //
						.addPoint(6, 0.65) //
						.addPoint(15, 0.65) //
						.addPoint(16, 1) //
						.addPoint(44, 1) //
						.addPoint(45, 0.65) //
						.addPoint(49, 0.65) //
						.addPoint(50, 0.325) //
						.addPoint(54, 0.325) //
						.addPoint(55, 0) //
						.build()) //
				.setMaxIncreasePerSecond(0.5) //
				.build();

		// Min-Cell-Voltage
		assertEquals(0.1 /* 10 % */, (double) maxChargeCurrentHandler.voltageToPercent.getValue(2950), 0.1);

		// Max-Cell-Voltage
		assertEquals(1 /* 100 % */, (double) maxChargeCurrentHandler.voltageToPercent.getValue(3300), 0.1);

		// Min-Cell-Temperature
		assertEquals(1 /* 100 % */, (double) maxChargeCurrentHandler.temperatureToPercent.getValue(16), 0.1);

		// Max-Cell-Temperature
		assertEquals(1 /* 100 % */, (double) maxChargeCurrentHandler.temperatureToPercent.getValue(16), 0.1);

		// 10 % -> 4 A; 100 % -> 40 A
		assertEquals(4, (double) maxChargeCurrentHandler.percentToAmpere(0.1), 0.1);
		assertEquals(40, (double) maxChargeCurrentHandler.percentToAmpere(1.), 0.1);

		// Integration test
		assertEquals(4, maxChargeCurrentHandler.calculateCurrentLimit(2950, 3300, 16, 17, 40));
		clock.leap(900, ChronoUnit.MILLIS);
		assertEquals(4, maxChargeCurrentHandler.calculateCurrentLimit(3000, 3300, 16, 17, 40));
		clock.leap(900, ChronoUnit.MILLIS);
		assertEquals(5, maxChargeCurrentHandler.calculateCurrentLimit(3050, 3300, 16, 17, 40));

		PolyLine.printAsCsv(maxChargeCurrentHandler.voltageToPercent);
		PolyLine.printAsCsv(maxChargeCurrentHandler.temperatureToPercent);
	}

	@Test
	public void testVoltageToPercent() {
		PolyLine p = PolyLine.create() //
				.addPoint(Math.nextDown(3000), 0.1) //
				.addPoint(3000, 1) //
				.addPoint(3450, 1) //
				.addPoint(Math.nextDown(3650), 0.01) //
				.addPoint(3650, 0) //
				.build();

		assertEquals(0.1, p.getValue(2500), 0.001);
		assertEquals(0.1, p.getValue(Math.nextDown(3000)), 0.001);
		assertEquals(1, p.getValue(3000), 0.001);
		assertEquals(1, p.getValue(3450), 0.001);
		assertEquals(0.752, p.getValue(3500), 0.001);
		assertEquals(0.257, p.getValue(3600), 0.001);
		assertEquals(0.01, p.getValue(Math.nextDown(3650)), 0.001);
		assertEquals(0, p.getValue(3650), 0.001);
		assertEquals(0, p.getValue(4000), 0.001);
	}

	@Test
	public void testTemperatureToPercent() {
		PolyLine p = PolyLine.create() //
				.addPoint(Math.nextDown(-10), 0) //
				.addPoint(-10, 0.215) //
				.addPoint(0, 0.215) //
				.addPoint(1, 0.325) //
				.addPoint(5, 0.325) //
				.addPoint(6, 0.65) //
				.addPoint(15, 0.65) //
				.addPoint(16, 1) //
				.addPoint(44, 1) //
				.addPoint(45, 0.65) //
				.addPoint(49, 0.65) //
				.addPoint(50, 0.325) //
				.addPoint(54, 0.325) //
				.addPoint(55, 0) //
				.build();

		assertEquals(0, p.getValue(-20), 0.001);
		assertEquals(0, p.getValue(Math.nextDown(-10)), 0.001);
		assertEquals(0.215, p.getValue(-10), 0.001);
		assertEquals(0.215, p.getValue(0), 0.001);
		assertEquals(0.27, p.getValue(0.5), 0.001);
		assertEquals(0.325, p.getValue(1), 0.001);
		assertEquals(0.325, p.getValue(5), 0.001);
		assertEquals(0.4875, p.getValue(5.5), 0.001);
		assertEquals(0.65, p.getValue(6), 0.001);
		assertEquals(0.65, p.getValue(15), 0.001);
		assertEquals(0.825, p.getValue(15.5), 0.001);
		assertEquals(1, p.getValue(16), 0.001);
		assertEquals(1, p.getValue(44), 0.001);
		assertEquals(0.825, p.getValue(44.5), 0.001);
		assertEquals(0.65, p.getValue(45), 0.001);
		assertEquals(0.65, p.getValue(49), 0.001);
		assertEquals(0.4875, p.getValue(49.5), 0.001);
		assertEquals(0.325, p.getValue(50), 0.001);
		assertEquals(0.325, p.getValue(54), 0.001);
		assertEquals(0.1625, p.getValue(54.5), 0.001);
		assertEquals(0, p.getValue(55), 0.001);
		assertEquals(0, p.getValue(100), 0.001);
	}

	@Test
	public void testMaxIncreasePerSecond() {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		MaxChargeCurrentHandler sut = MaxChargeCurrentHandler.create(cm, 40) //
				.setMaxIncreasePerSecond(0.5) //
				.build();
		sut.lastResultLimit = 0D;
		sut.lastResultTimestamp = Instant.now(clock);

		clock.leap(1, ChronoUnit.SECONDS);
		assertEquals(0.5, (double) sut.getMaxIncreaseAmpereLimit(), 0.001);
		sut.lastResultLimit = 0.5;

		clock.leap(1, ChronoUnit.SECONDS);
		assertEquals(1, (double) sut.getMaxIncreaseAmpereLimit(), 0.001);
		sut.lastResultLimit = 1.;

		clock.leap(800, ChronoUnit.MILLIS);
		assertEquals(1.4, (double) sut.getMaxIncreaseAmpereLimit(), 0.001);
	}

}
