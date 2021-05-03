package io.openems.edge.bridge.mbus.api;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mbus.api.ChannelRecord.DataType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StringReadChannel;
import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.DlmsUnit;
import org.openmuc.jmbus.VariableDataStructure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * This class processes messages from a Wireless M-Bus or M-Bus devices. The constructor receives the variable data
 * structure from the WM-Bus/M-Bus message, as well as the channel data records list. This list contains channels and an
 * associated data record address. Each channel is then filled with the value from the associated data record. Before
 * doing that, the units of the channel and the data record are compared. If the units do not match, an error message is
 * logged and possibly no data is put in the channel. If the units are compatible, the value of the data record is
 * scaled to the unit of the channel before it is written to the channel.
 * This class will generate a timestamp in epoch when the channel is set to data record address -1. A string
 * representation of this timestamp is created when the address is set to -2. Note that the timestamp as string does not
 * create a timestamp. It only converts the last timestamp (read from the device or created by this class) to a string.
 */

public class ChannelDataRecordMapper {
	protected VariableDataStructure data;
	private long timestamp = 0;	// transfer variable to set TIMESTAMP_STRING with the same value as TIMESTAMP_SECONDS in interface WaterMeter
	private static final DateTimeFormatter timeformat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
	protected List<ChannelRecord> channelDataRecordsList;
	private int nominator;
	private int denominator;

	// The ChannelDataRecordMapper can be used with or without error logging to a channel.
	private StringBuilder errorMessage = new StringBuilder();

	/**
	 * Constructor. Is called in the "processData()" method of MbusTask or WMbusProtocol.
	 * This iterates the contents of the channelDataRecordsList and calls the "mapDataToChannel()" on each item.
	 * This is the constructor without an error message channel. This will disable error logging.
	 *
	 * @param data        				The VariableDataStructure of a M-Bus or Wireless M-Bus message.
	 * @param channelDataRecordsList 	The channel data record list.
	 */
	public ChannelDataRecordMapper(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList) {
		this.data = data;
		this.channelDataRecordsList = channelDataRecordsList;

		for (ChannelRecord channelRecord : channelDataRecordsList) {
			this.mapDataToChannel(data, channelRecord.getDataRecordPosition(), channelRecord.getChannel(),
					channelRecord.getDataType());
		}
	}

	/**
	 * Constructor. Is called in the "processData()" method of MbusTask or WMbusProtocol.
	 * This iterates the contents of the channelDataRecordsList and calls the "mapDataToChannel()" on each item.
	 * This is the constructor using an error message channel for error logging.
	 *
	 * @param data        				The VariableDataStructure of a M-Bus or Wireless M-Bus message.
	 * @param channelDataRecordsList 	The channel data record list.
	 * @param errorMessageChannel 		The channel for the error messages.
	 */
	public ChannelDataRecordMapper(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList,
								   StringReadChannel errorMessageChannel) {
		this.data = data;
		this.channelDataRecordsList = channelDataRecordsList;

		for (ChannelRecord channelRecord : channelDataRecordsList) {
			this.mapDataToChannel(data, channelRecord.getDataRecordPosition(), channelRecord.getChannel(),
					channelRecord.getDataType());
		}

		if (this.errorMessage.length() > 0) {
			this.errorMessage.deleteCharAt(this.errorMessage.length() - 1);
			errorMessageChannel.setNextValue(this.errorMessage);
		} else {
			errorMessageChannel.setNextValue("No error");
		}
	}

	/**
	 * Get the variable data structure of this ChannelDataRecordMapper.
	 *
	 * @return the variable data structure.
	 */
	public VariableDataStructure getData() {
		return this.data;
	}

	/**
	 * Set the variable data structure of this ChannelDataRecordMapper.
	 *
	 * @param data	The variable data structure.
	 */
	public void setData(VariableDataStructure data) {
		this.data = data;
	}

	/**
	 * Get the channel data record list of this ChannelDataRecordMapper.
	 *
	 * @return the channel data record list.
	 */
	public List<ChannelRecord> getChannelDataRecordsList() {
		return this.channelDataRecordsList;
	}

	/**
	 * Set the channel data record list of this ChannelDataRecordMapper.
	 *
	 * @param channelDataRecordsList	The channel data record list.
	 */
	public void setChannelDataRecordsList(List<ChannelRecord> channelDataRecordsList) {
		this.channelDataRecordsList = channelDataRecordsList;
	}

	/**
	 * Decide what data from VariableDataStructure to put in the channel.
	 *
	 * @param data		The VariableDataStructure.
	 * @param index		The data record position.
	 * @param channel	The channel.
	 * @param dataType	The data type, if available.
	 */
	protected void mapDataToChannel(VariableDataStructure data, int index, Channel<?> channel, DataType dataType) {

		// For the actual data, the dataType is null. See switch statement below to see what dataType is used for.
		if (dataType == null) {
			// Check that the index is not out of bounds.
			if (data.getDataRecords().size() > index && index >= 0) {
				// Call method to write the data to the channel. Before the data is written, it is scaled based on a
				// unit comparison between the unit of the data and the unit of the channel. Does not write to the
				// channel if the units are not compatible (and logs an error if error logging is enabled).
				this.mapScaledCheckedDataToChannel(data.getDataRecords().get(index),channel);
			} else {
				// Special case or error
				switch (index) {
					case -1:
						// Address -1 signifies to let OpenEMS set the timestamp instead of reading it from the meter.
						this.setTimestamp(channel);
						break;
					case -2:
						// Address -2 is used to write the timestamp as a string format. This is an additional channel that does
						// not function on it's own. It just converts the contents of the variable "timestamp" into a string.
						// That variable is updated when the meter timestamp is read or the address -1 is used.
						this.setTimestampString(channel);
						break;
					default:
						// If you land here then something went wrong -> Error
						if (index < 0) {
							this.errorMessage.append("Address " + index + " is not a valid address. Cannot get data for channel "
									+ channel.channelId().toString() + ". ");
						} else {
							this.errorMessage.append("Address " + index + " is out of bounds. Tried to read record number "
									+ (index + 1) + ", but the meter only sent " + data.getDataRecords().size()
									+ " records. Cannot get data for channel " + channel.channelId().toString() + ". ");
						}
				}
			}
			return;
		}

		switch (dataType) {
			case Manufacturer:
				channel.setNextValue(data.getSecondaryAddress().getManufacturerId());
				break;
			case DeviceId:
				channel.setNextValue(data.getSecondaryAddress().getDeviceId());
				break;
			case MeterType:
				channel.setNextValue(data.getSecondaryAddress().getDeviceType());
				break;
			default:
				this.errorMessage.append("Requested data type " + dataType.toString()
						+ ", but that is not yet supported by the software. Cannot get data for channel "
						+ channel.channelId().toString() + ". ");
		}
	}

	/**
	 * Generate a timestamp as a string from the "timestamp" variable and write it in the channel. The "timestamp"
	 * variable is updated every time "setTimestamp()" is called.
	 *
	 * @param channel	The channel.
	 */
	protected void setTimestampString(Channel<?> channel) {
		if (channel.getType() == OpenemsType.STRING) {
			ZoneOffset timezone = ZoneOffset.from(ZonedDateTime.now());
			LocalDateTime localDateTime = LocalDateTime.ofEpochSecond((this.timestamp / 1000), 0, timezone);
			channel.setNextValue(localDateTime.format(timeformat));
		} else {
			this.errorMessage.append("Tried to write a string timestamp into channel " + channel.channelId().toString()
					+ ", but that channel is not of type \"String\". Aborted write. ");
		}
	}

	/**
	 * Generates a timestamp from the current system time and writes it to a channel. The channels needs to have a unit
	 * of time. The timestamp has the format "time since Epoch" and is scaled to the channel unit.
	 *
	 * @param channel	The channel.
	 */
	protected void setTimestamp(Channel<?> channel) {
		Unit openemsUnit = channel.channelDoc().getUnit();
		long currentTimeMillis = System.currentTimeMillis();
		int divisor = this.timeUnitDivisor(openemsUnit);
		if (divisor > 0) {
			this.timestamp = currentTimeMillis;
			channel.setNextValue(currentTimeMillis / divisor);
		} else {
			this.errorMessage.append("Tried to write a timestamp into channel " + channel.channelId().toString()
					+ ", but that channel is not a timestamp channel. Aborted write. The channel needs to have the "
					+ "unit MILLISECONDS, SECONDS, MINUTE or HOUR. ");
		}
	}

	/**
	 * Generates a divisor for scaling a time value in milliseconds to the openemsUnit of a channel. Will return -1 if
	 * the unit is not a time unit.
	 *
	 * @param openemsUnit	The unit of the channel.
	 * @return the divisor.
	 */
	protected int timeUnitDivisor(Unit openemsUnit) {
		int divisor = -1;
		switch (openemsUnit) {
			case MILLISECONDS:
				divisor = 1;
				break;
			case SECONDS:
				divisor = 1000;
				break;
			case MINUTE:
				divisor = 60000;
				break;
			case HOUR:
				divisor = 3600000;
				break;
			default:
		}
		return divisor;
	}

	/**
	 * Checks if the units from a DataRecord and a channel are compatible. If yes, scales the data accordingly and
	 * writes the data to the channel. Does not write to the channel if the units are not compatible (and logs an error).
	 *
	 * @param record	The unit of the channel.
	 * @param channel	The unit of the channel.
	 */
	protected void mapScaledCheckedDataToChannel(DataRecord record, Channel<?> channel) {
		Unit openemsUnit = channel.channelDoc().getUnit();

		// Timestamp unit from meter is milliseconds since epoch, same as System.currentTimeMillis().
		// However, meter time accuracy is minutes. If you want seconds accuracy, set your timestamp channel to address
		// -1 which then uses "setTimestamp()" method and lets OpenEMS calculate the timestamp.
		if (record.getDataValueType() == DataRecord.DataValueType.DATE) {
			int divisor = this.timeUnitDivisor(openemsUnit);
			if (divisor > 0) {
				this.timestamp = ((Date) record.getDataValue()).getTime();
				channel.setNextValue(((Date) record.getDataValue()).getTime() / divisor);
			} else {
				this.errorMessage.append("Tried to write a timestamp into channel " + channel.channelId().toString()
						+ ", but that channel is not a timestamp channel. Aborted write. The channel needs to have the "
						+ "unit MILLISECONDS, SECONDS, MINUTE or HOUR. ");
			}
			return;
		}

		// Nominator and denominator for multipliers which are not powers of 10, e.g. hours, minutes
		this.nominator = -1;
		this.denominator = -1;
		// Exponent for scaling by powers of 10
		int scaleFactor = record.getMultiplierExponent() - openemsUnit.getScaleFactor();
		DlmsUnit mbusUnit = record.getUnit();

		// mbusUnit can be null. If the meter Channel is the error Channel for example.
		if (mbusUnit == null) {
			if (channel.channelDoc().getUnit() != null) {
				this.errorMessage.append("Warning: Unit mismatch. The data written into channel " + channel.channelId().toString()
						+ " has no unit, while the channel has the unit " + channel.channelDoc().getUnit().toString() + ". ");
			}
			channel.setNextValue(((Number) record.getDataValue()).doubleValue());
			return;
		}

		// Replacing unit by its BaseUnit. For this reason channelScaleFactor has to be evaluated beforehand
		if (channel.channelDoc().getUnit().getBaseUnit() != null) {
			openemsUnit = openemsUnit.getBaseUnit();
		}

		// Set nominator and denominator: [mbusunit] = nominator / denominator * [openemsbaseunit]
		this.setNominatorAndDenominator(openemsUnit, mbusUnit);

		if (this.nominator > 0 & this.denominator > 0) {
			channel.setNextValue(((Number) record.getDataValue()).doubleValue() * this.nominator
					* Math.pow(10, scaleFactor) / this.denominator);
		} else {
			this.errorMessage.append("Error: Unit mismatch. Cannot write data with unit " + mbusUnit.toString()
					+ " into channel " + channel.channelId().toString()
					+ " with unit " + channel.channelDoc().getUnit().toString() + ". ");
		}
	}

	/**
	 * Sets nominator and denominator for scaling a value from a data record to the openemsUnit of a channel. Will not
	 * set nominator and denominator if the unit from the data record is not compatible with the unit of the channel.
	 * [mbusunit] = nominator / denominator * [openemsbaseunit]
	 *
	 * @param openemsUnit	The unit of the channel.
	 * @param mbusUnit		The unit of the data record.
	 */
	protected void setNominatorAndDenominator(Unit openemsUnit, DlmsUnit mbusUnit) {
		if (openemsUnit == Unit.CUBIC_METER && mbusUnit == DlmsUnit.CUBIC_METRE
			|| openemsUnit == Unit.DEGREE_CELSIUS && mbusUnit == DlmsUnit.DEGREE_CELSIUS
			|| openemsUnit == Unit.BAR && mbusUnit == DlmsUnit.BAR
			|| openemsUnit == Unit.VOLT_AMPERE_REACTIVE && mbusUnit == DlmsUnit.VAR
			|| openemsUnit == Unit.VOLT_AMPERE_REACTIVE_HOURS && mbusUnit == DlmsUnit.VAR_HOUR
			|| openemsUnit == Unit.VOLT_AMPERE && mbusUnit == DlmsUnit.VOLT_AMPERE
			|| openemsUnit == Unit.VOLT_AMPERE_HOURS && mbusUnit == DlmsUnit.VOLT_AMPERE_HOUR
			|| openemsUnit == Unit.VOLT && mbusUnit == DlmsUnit.VOLT
			|| openemsUnit == Unit.AMPERE && mbusUnit == DlmsUnit.AMPERE) {
			this.nominator = 1;
			this.denominator = 1;
			return;
		}

		switch (openemsUnit) {
			case CUBIC_METER_PER_HOUR:
				switch (mbusUnit) {
					case CUBIC_METRE_PER_DAY:
						this.nominator = 1;
						this.denominator = 24;
						break;
					case CUBIC_METRE_PER_HOUR:
						this.nominator = 1;
						this.denominator = 1;
						break;
					case CUBIC_METRE_PER_MINUTE:
						this.nominator = 60;
						this.denominator = 1;
						break;
					case CUBIC_METRE_PER_SECOND:
						this.nominator = 3600;
						this.denominator = 1;
						break;
					default:
				}
				break;
			case CUBIC_METER_PER_SECOND:
				switch (mbusUnit) {
					case CUBIC_METRE_PER_DAY:
						this.nominator = 1;
						this.denominator = 86400;
						break;
					case CUBIC_METRE_PER_HOUR:
						this.nominator = 1;
						this.denominator = 3600;
						break;
					case CUBIC_METRE_PER_MINUTE:
						this.nominator = 1;
						this.denominator = 60;
						break;
					case CUBIC_METRE_PER_SECOND:
						this.nominator = 1;
						this.denominator = 1;
						break;
					default:
				}
				break;
			case SECONDS:
				switch (mbusUnit) {
					case SECOND:
						this.nominator = 1;
						this.denominator = 1;
						break;
					case MIN:
						this.nominator = 60;
						this.denominator = 1;
						break;
					case HOUR:
						this.nominator = 3600;
						this.denominator = 1;
						break;
					case DAY:
						this.nominator = 86400;
						this.denominator = 1;
						break;
					default:
				}
				break;
			case MINUTE:
				switch (mbusUnit) {
					case SECOND:
						this.nominator = 1;
						this.denominator = 60;
						break;
					case MIN:
						this.nominator = 1;
						this.denominator = 1;
						break;
					case HOUR:
						this.nominator = 60;
						this.denominator = 1;
						break;
					case DAY:
						this.nominator = 1440;
						this.denominator = 1;
						break;
					default:
				}
				break;
			case HOUR:
				switch (mbusUnit) {
					case SECOND:
						this.nominator = 1;
						this.denominator = 3660;
						break;
					case MIN:
						this.nominator = 1;
						this.denominator = 60;
						break;
					case HOUR:
						this.nominator = 1;
						this.denominator = 1;
						break;
					case DAY:
						this.nominator = 60;
						this.denominator = 1;
						break;
					default:
				}
				break;
			case WATT:
				switch (mbusUnit) {
					case WATT:
						this.nominator = 1;
						this.denominator = 1;
						break;
					case JOULE_PER_HOUR:
						this.nominator = 1;
						this.denominator = 3600;
						break;
					default:
				}
				break;
			case WATT_SECONDS:
				switch (mbusUnit) {
					case JOULE:
						this.nominator = 1;
						this.denominator = 1;
						break;
					case WATT_HOUR:
						this.nominator = 3600;
						this.denominator = 1;
						break;
					default:
				}
				break;
			case WATT_HOURS:
				switch (mbusUnit) {
					case JOULE:
						this.nominator = 1;
						this.denominator = 3600;
						break;
					case WATT_HOUR:
						this.nominator = 1;
						this.denominator = 1;
						break;
					default:
				}
				break;
			default:
		}
	}


}
