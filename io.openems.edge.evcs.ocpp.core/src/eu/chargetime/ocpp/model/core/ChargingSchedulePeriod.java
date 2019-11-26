package eu.chargetime.ocpp.model.core;

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
 * Class Type used with {@link ChargingSchedule}. 
 */
@XmlRootElement
@XmlType(propOrder = { "startPeriod", "limit", "numberPhases" })
public class ChargingSchedulePeriod implements Validatable {
	private Integer startPeriod;
	private Double limit;
	private Integer numberPhases = 3;

	public ChargingSchedulePeriod() {
	}

	public ChargingSchedulePeriod(Integer startPeriod, Double limit) {
		this.startPeriod = startPeriod;
		this.limit = limit;
	}

	@Override
	public boolean validate() {
		boolean valid = true;
		valid &= startPeriod != null;
		valid &= limit != null;
		return valid;
	}

	/**
	 * Required. Start of the period, in seconds from the start of schedule. The
	 * value of StartPeriod also defines the stop time of the previous period.
	 *
	 * @param startPeriod integer, seconds from start of schedule.
	 */
	@XmlElement
	public void setStartPeriod(Integer startPeriod) {
		this.startPeriod = startPeriod;
	}

	/**
	 * Start of the period, in seconds from the start of schedule. The value of
	 * StartPeriod also defines the stop time of the previous period.
	 *
	 * @return Seconds from start of schedule.
	 */
	public Integer getStartPeriod() {
		return startPeriod;
	}

	/**
	 * Required. Power limit during the schedule period, expressed in Amperes.
	 * Accepts at most one digit fraction (e.g. 8.1).
	 *
	 * @param limit decimal, power limit.
	 */
	@XmlElement
	public void setLimit(Double limit) {
		this.limit = limit;
	}

	/**
	 * Power limit during the schedule period, expressed in Amperes. Accepts at most
	 * one digit fraction (e.g. 8.1).
	 *
	 * @return Power limit.
	 */
	public Double getLimit() {
		return limit;
	}

	/**
	 * Optional. The number of phases that can be used for charging. Value is set to
	 * 3 by default.
	 *
	 * @param numberPhases integer, default is 3.
	 */
	@XmlElement
	public void setNumberPhases(Integer numberPhases) {
		this.numberPhases = numberPhases;
	}

	/**
	 * The number of phases that can be used for charging. Value is set to 3 by
	 * default.
	 *
	 * @return Number of phases.
	 */
	public Integer getNumberPhases() {
		return numberPhases;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ChargingSchedulePeriod that = (ChargingSchedulePeriod) o;
		return Objects.equals(startPeriod, that.startPeriod) && Objects.equals(limit, that.limit)
				&& Objects.equals(numberPhases, that.numberPhases);
	}

	@Override
	public int hashCode() {
		return Objects.hash(startPeriod, limit, numberPhases);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("startPeriod", startPeriod).add("limit", limit)
				.add("numberPhases", numberPhases).add("isValid", validate()).toString();
	}
}
