package eu.chargetime.ocpp.model.core;
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

import eu.chargetime.ocpp.PropertyConstraintException;
import eu.chargetime.ocpp.model.Validatable;
import eu.chargetime.ocpp.utilities.ModelUtil;
import eu.chargetime.ocpp.utilities.MoreObjects;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single sampled value in {@link MeterValue}. Each value can be accompanied by
 * optional fields.
 */
@XmlRootElement
@XmlType(propOrder = { "value", "context", "format", "measurand", "phase", "location", "unit" })
public class SampledValue implements Validatable {
	private static final Logger logger = LoggerFactory.getLogger(SampledValue.class);

	private String value;
	private String context;
	private ValueFormat format;
	private String measurand;
	private String phase;
	private Location location;
	private String unit;

	public SampledValue() {
		try {
			setContext("Sample.Periodic");
			setFormat(ValueFormat.Raw);
			setMeasurand("Energy.Active.Import.Register");
			setLocation(Location.Outlet);
			setUnit("Wh");
		} catch (PropertyConstraintException ex) {
			logger.error("constructor of SampledValue failed", ex);
		}
	}

	@Override
	public boolean validate() {
		return this.value != null;
	}

	/**
	 * Value as a Raw (@code decimal) number or {@code SignedData}. Field Type is
	 * String to allow for digitally signed data readings. Decimal numeric values
	 * are also acceptable to allow fractional values for measurands such as
	 * Temperature and Current.
	 *
	 * @return String, the value.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Required. Value as a {@code Raw} (decimal) number or {@code SignedData}.
	 * Field Type is String to allow for digitally signed data readings. Decimal
	 * numeric values are also acceptable to allow fractional values for measurands
	 * such as Temperature and Current.
	 *
	 * @param value String, the value.
	 */
	@XmlElement
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Type of detail value: start, end or sample.
	 *
	 * @return enum value for context.
	 */
	public String getContext() {
		return context;
	}

	/**
	 * Optional. Type of detail value: start, end or sample. Default =
	 * {@code Sample.Periodic}
	 *
	 * <p>
	 * Enum value with accepted values: {@code Interruption.Begin},
	 * {@code Interruption.End}, {@code Other}, {@code Sample.Clock},
	 * {@code Sample.Periodic}, {@code Transaction.Begin}, {@code
	 * Transaction.End}, {@code Trigger}
	 *
	 * @param context String, see description for accepted values.
	 */
	// TODO: Change to enum, solve format issue and change exception message.
	@XmlElement
	public void setContext(String context) {
		if (!isValidContext(context)) {
			throw new PropertyConstraintException(context, "context is not properly defined");
		}

		this.context = context;
	}

	private boolean isValidContext(String context) {
		String[] readingContext = { "Interruption.Begin", "Interruption.End", "Other", "Sample.Clock",
				"Sample.Periodic", "Transaction.Begin", "Transaction.End", "Trigger" };
		return ModelUtil.isAmong(context, readingContext);
	}

	/**
	 * Raw or signed data.
	 *
	 * @return the {@link ValueFormat}.
	 */
	public ValueFormat getFormat() {
		return format;
	}

	/**
	 * Raw or signed data.
	 *
	 * @return the {@link ValueFormat}.
	 */
	@Deprecated
	public ValueFormat objFormat() {
		return format;
	}

	/**
	 * Optional. Raw or signed data. Default = {@code Raw}.
	 *
	 * @param format the {@link ValueFormat}.
	 */
	@XmlElement
	public void setFormat(ValueFormat format) {
		this.format = format;
	}

	/**
	 * Type of measurement.
	 *
	 * @return enum value of measurand.
	 */
	public String getMeasurand() {
		return measurand;
	}

	/**
	 * Optional. Type of measurement. Default =
	 * {@code Energy.Active.Import.Register}.
	 *
	 * <p>
	 * Enum value with accepted values: {@code Current.Export},
	 * {@code Current.Import}, {@code
	 * Current.Offered}, {@code Energy.Active.Export.Register},
	 * {@code Energy.Active.Import.Register},
	 * {@code Energy.Reactive.Export.Register},
	 * {@code Energy.Reactive.Import.Register}, {@code
	 * Energy.Active.Export.Interval}, {@code Energy.Active.Import.Interval}, {@code
	 * Energy.Reactive.Export.Interval}, {@code Energy.Reactive.Import.Interval},
	 * {@code Frequency}, {@code Power.Active.Export}, {@code Power.Active.Import},
	 * {@code Power.Factor}, {@code
	 * Power.Offered}, {@code Power.Reactive.Export}, {@code Power.Reactive.Import},
	 * {@code RPM}, {@code SoC}, {@code Temperature}, {@code Voltage}
	 *
	 * @param measurand String, enum value of measurand.
	 */
	// TODO: Change to enum, solve format issue and change exception message.
	@XmlElement
	public void setMeasurand(String measurand) {
		if (!isValidMeasurand(measurand))
			throw new PropertyConstraintException(measurand, "measurand value is not properly defined");

		this.measurand = measurand;
	}

	private boolean isValidMeasurand(String measurand) {
		String[] measurandValues = { "Current.Export", "Current.Import", "Current.Offered",
				"Energy.Active.Export.Register", "Energy.Active.Import.Register", "Energy.Reactive.Export.Register",
				"Energy.Reactive.Import.Register", "Energy.Active.Export.Interval", "Energy.Active.Import.Interval",
				"Energy.Reactive.Export.Interval", "Energy.Reactive.Import.Interval", "Frequency",
				"Power.Active.Export", "Power.Active.Import", "Power.Factor", "Power.Offered", "Power.Reactive.Export",
				"Power.Reactive.Import", "RPM", "SoC", "Temperature", "Voltage" };
		return ModelUtil.isAmong(measurand, measurandValues);
	}

	/**
	 * Indicates how the measured value is to be interpreted. For instance between
	 * L1 and neutral (L1-N).
	 *
	 * @return enum value of phase.
	 */
	public String getPhase() {
		return phase;
	}

	/**
	 * Optional. Indicates how the measured value is to be interpreted. For instance
	 * between L1 and neutral (L1-N). Please note that not all values of phase are
	 * applicable to all Measurands. When phase is absent, the measured value is
	 * interpreted as an overall value.
	 *
	 * <p>
	 * Enum value with accepted values: {@code L1}, {@code L2}, {@code L3},
	 * {@code N}, {@code
	 * L1-N}, {@code L2-N}, {@code L3-N}, {@code L1-L2}, {@code L2-L3},
	 * {@code L3-L1}
	 *
	 * @param phase String, enum value of phase.
	 */
	// TODO: Change to enum, solve format issue and change exception message.
	@XmlElement
	public void setPhase(String phase) {
		if (!isValidPhase(phase)) {
			throw new PropertyConstraintException(phase, "phase is not properly defined");
		}

		this.phase = phase;
	}

	private boolean isValidPhase(String phase) {
		return ModelUtil.isAmong(phase, "L1", "L2", "L3", "N", "L1-N", "L2-N", "L3-N", "L1-L2", "L2-L3", "L3-L1");
	}

	/**
	 * Location of measurement.
	 *
	 * @return the {@link Location}.
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Location of measurement.
	 *
	 * @return the {@link Location}.
	 */
	@Deprecated
	public Location objLocation() {
		return location;
	}

	/**
	 * Optional. Location of measurement. Default={@code Outlet}
	 *
	 * @param location the {@link Location}.
	 */
	@XmlElement
	public void setLocation(Location location) {
		this.location = location;
	}

	/**
	 * Unit of the value.
	 *
	 * @return Unit of Measure.
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Optional. Unit of the value. Default = {@code Wh} if the (default) measurand
	 * is an {@code
	 * Energy} type.
	 *
	 * <p>
	 * Enum value with accepted values: {@code Wh}, {@code kWh}, {@code varh},
	 * {@code kvarh}, {@code W}, {@code kW}, {@code VA}, {@code kVA}, {@code var},
	 * {@code kvar}, {@code A}, {@code
	 * V}, {@code Celsius}, {@code Fahrenheit}, {@code K}, {@code Percent}
	 *
	 * @param unit String, enum value, Unit of Measure.
	 */
	// TODO: Change to enum, solve format issue and change exception message.
	@XmlElement
	public void setUnit(String unit) {
		if (!isValidUnit(unit)) {
			throw new PropertyConstraintException(unit, "unit is not properly defined");
		}

		this.unit = unit;
	}

	private boolean isValidUnit(String unit) {
		String[] unitOfMeasure = { "Wh", "kWh", "varh", "kvarh", "W", "kW", "VA", "kVA", "var", "kvar", "A", "V",
				"Celsius", "Fahrenheit", "K", "Percent" };
		return ModelUtil.isAmong(unit, unitOfMeasure);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SampledValue that = (SampledValue) o;
		return Objects.equals(value, that.value) && Objects.equals(context, that.context) && format == that.format
				&& Objects.equals(measurand, that.measurand) && Objects.equals(phase, that.phase)
				&& location == that.location && Objects.equals(unit, that.unit);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, context, format, measurand, phase, location, unit);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("value", value).add("context", context).add("format", format)
				.add("measurand", measurand).add("phase", phase).add("location", location).add("unit", unit)
				.add("isValid", validate()).toString();
	}
}
