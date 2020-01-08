package eu.chargetime.ocpp.model.core;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/*
 * ChargeTime.eu - Java-OCA-OCPP
 *
 * MIT License
 *
 * Copyright (C) 2016-2018 Thomas Volden <tv@chargetime.eu>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import eu.chargetime.ocpp.model.Validatable;
import eu.chargetime.ocpp.utilities.MoreObjects;

/**
 * Class type used with {@link ChargingProfile}. 
 */
@XmlRootElement
@XmlType(propOrder = { "duration", "startSchedule", "chargingRateUnit", "chargingSchedulePeriod", "minChargingRate" })
public class ChargingSchedule implements Validatable {
	private Integer duration;
	private Calendar startSchedule;
	private ChargingRateUnitType chargingRateUnit;
	private ChargingSchedulePeriod[] chargingSchedulePeriod;
	private Double minChargingRate;

	public ChargingSchedule() {
	}

	public ChargingSchedule(ChargingRateUnitType chargingRateUnit, ChargingSchedulePeriod[] chargingSchedulePeriod) {
		this.chargingRateUnit = chargingRateUnit;
		this.chargingSchedulePeriod = chargingSchedulePeriod;
	}

	@Override
	public boolean validate() {
		boolean valid = chargingRateUnit != null;
		if (valid &= chargingSchedulePeriod != null) {
			for (ChargingSchedulePeriod period : chargingSchedulePeriod) {
				valid &= period.validate();
			}
		}
		return valid;
	}

	/**
	 * Duration of the charging schedule in seconds.
	 *
	 * @return duration in seconds.
	 */
	public Integer getDuration() {
		return duration;
	}

	/**
	 * Optional. Duration of the charging schedule in seconds. If the duration is
	 * left empty, the last period will continue indefinitely or until end of the
	 * transaction in case startSchedule is absent.
	 *
	 * @param duration integer, duration in seconds.
	 */
	@XmlElement
	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	/**
	 * Starting point of an absolute schedule.
	 *
	 * @return start time.
	 */
	public Calendar getStartSchedule() {
		return startSchedule;
	}

	/**
	 * Starting point of an absolute schedule.
	 *
	 * @return start time.
	 */
	@Deprecated
	public Calendar objStartSchedule() {
		return startSchedule;
	}

	/**
	 * Optional. Starting point of an absolute schedule. If absent the schedule will
	 * be relative to start of charging.
	 *
	 * @param startSchedule Calendar, start time.
	 */
	@XmlElement
	public void setStartSchedule(Calendar startSchedule) {
		this.startSchedule = startSchedule;
	}

	/**
	 * The unit of measure Limit is expressed in.
	 *
	 * @return the {@link ChargingRateUnitType}.
	 */
	public ChargingRateUnitType getChargingRateUnit() {
		return chargingRateUnit;
	}

	/**
	 * The unit of measure Limit is expressed in.
	 *
	 * @return the {@link ChargingRateUnitType}.
	 */
	@Deprecated
	public ChargingRateUnitType objChargingRateUnit() {
		return chargingRateUnit;
	}

	/**
	 * Required. The unit of measure Limit is expressed in.
	 *
	 * @param chargingRateUnit the {@link ChargingRateUnitType}.
	 */
	@XmlElement
	public void setChargingRateUnit(ChargingRateUnitType chargingRateUnit) {
		this.chargingRateUnit = chargingRateUnit;
	}

	/**
	 * List of ChargingSchedulePeriod elements defining maximum power or current
	 * usage over time.
	 *
	 * @return array of {@link ChargingSchedulePeriod}.
	 */
	public ChargingSchedulePeriod[] getChargingSchedulePeriod() {
		return chargingSchedulePeriod;
	}

	/**
	 * Required. List of ChargingSchedulePeriod elements defining maximum power or
	 * current usage over time.
	 *
	 * @param chargingSchedulePeriod array of {@link ChargingSchedulePeriod}.
	 */
	@XmlElement
	public void setChargingSchedulePeriod(ChargingSchedulePeriod[] chargingSchedulePeriod) {
		this.chargingSchedulePeriod = chargingSchedulePeriod;
	}

	/**
	 * Minimum charging rate supported by the electric vehicle. The unit of measure
	 * is defined by {@link #getChargingRateUnit()}.
	 *
	 * @return min charge rate.
	 */
	public Double getMinChargingRate() {
		return minChargingRate;
	}

	/**
	 * Optional. Minimum charging rate supported by the electric vehicle. The unit
	 * of measure is defined by {@link #getChargingRateUnit()}. This parameter is
	 * intended to be used by a local smart charging algorithm to optimize the power
	 * allocation for in the case a charging process is inefficient at lower
	 * charging rates. Accepts at most one digit fraction (e.g. 8.1)
	 *
	 * @param minChargingRate decimal, min charge rate.
	 */
	@XmlElement
	public void setMinChargingRate(Double minChargingRate) {
		this.minChargingRate = minChargingRate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ChargingSchedule that = (ChargingSchedule) o;
		return Objects.equals(duration, that.duration) && Objects.equals(startSchedule, that.startSchedule)
				&& chargingRateUnit == that.chargingRateUnit
				&& Arrays.equals(chargingSchedulePeriod, that.chargingSchedulePeriod)
				&& Objects.equals(minChargingRate, that.minChargingRate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(duration, startSchedule, chargingRateUnit, chargingSchedulePeriod, minChargingRate);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("duration", duration).add("startSchedule", startSchedule)
				.add("chargingRateUnit", chargingRateUnit).add("chargingSchedulePeriod", chargingSchedulePeriod)
				.add("minChargingRate", minChargingRate).add("isValid", validate()).toString();
	}
}
