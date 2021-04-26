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

// This class processes messages from a Wireless M-Bus or M-Bus devices. The constructor receives the variable data
// structure from the WM-Bus/M-Bus message, as well as the channel data records list. This list contains channels and an
// associated data record address. Each channel is then filled with the value from the associated data record. Before
// doing that, the units of the channel and the data record are compared. If the units do not match, an error message is
// logged and possibly no data is put in the channel. If the units are compatible, the value of the data record is
// scaled to the unit of the channel before it is written to the channel.
// This class will generate a timestamp in epoch when the channel is set to data record address -1. A string
// representation of this timestamp is created when the address is set to -2. Note that the timestamp as string does not
// create a timestamp. It only converts the last timestamp (read from the device or created by this class) to a string.

public class ChannelDataRecordMapper {
	protected VariableDataStructure data;
	private long timestamp = 0;	// transfer variable to set TIMESTAMP_STRING with the same value as TIMESTAMP_SECONDS in interface WaterMeter
	private static final DateTimeFormatter timeformat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
	protected List<ChannelRecord> channelDataRecordsList;

	// The ChannelDataRecordMapper can be used with or without error logging to a channel.
	private StringBuilder errorMessage = new StringBuilder();

	public ChannelDataRecordMapper(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList) {
		this.data = data;
		this.channelDataRecordsList = channelDataRecordsList;

		for (ChannelRecord channelRecord : channelDataRecordsList) {
			this.mapDataToChannel(data, channelRecord.getDataRecordPosition(), channelRecord.getChannel(),
					channelRecord.getDataType());
		}
	}

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

	public void setChannelDataRecordsList(List<ChannelRecord> channelDataRecordsList) {
		this.channelDataRecordsList = channelDataRecordsList;
	}

	protected void mapDataToChannel(VariableDataStructure data, int index, Channel<?> channel, DataType dataType) {

		if (dataType == null) {
			if (data.getDataRecords().size() > index && index >= 0) {
				//	channel.setNextValue(data.getDataRecords().get(index).getScaledDataValue());
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

	// Generate a timestamp as a string from the "timestamp" variable. The "timestamp" variable is updated
	// every time "setTimestamp()" is called.
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

	@SuppressWarnings("incomplete-switch")
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
		}
		return divisor;
	}

	@SuppressWarnings("incomplete-switch")
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

		//Nominator and denominator for multipliers which are not powers of 10, e.g. hours, minutes
		int nominator = -1;
		int denominator = -1;
		//Exponent for scaling by powers of 10
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

		//replacing unit by its BaseUnit. For this reason channelScaleFactor has to be evaluated beforehand
		if (channel.channelDoc().getUnit().getBaseUnit() != null) {
			openemsUnit = openemsUnit.getBaseUnit();
		}

		//setting nominator and denominator: [mbusunit]=nominator/denominator * [openemsbaseunit]
		switch (openemsUnit) {
			case CUBIC_METER:
				switch (mbusUnit) {
					case CUBIC_METRE:
						nominator = 1;
						denominator = 1;
						break;
				}
				break;
			//return error
			case CUBIC_METER_PER_HOUR:
				switch (mbusUnit) {
					case CUBIC_METRE_PER_DAY:
						nominator = 1;
						denominator = 24;
						break;
					case CUBIC_METRE_PER_HOUR:
						nominator = 1;
						denominator = 1;
						break;
					case CUBIC_METRE_PER_MINUTE:
						nominator = 60;
						denominator = 1;
						break;
					case CUBIC_METRE_PER_SECOND:
						nominator = 3600;
						denominator = 1;
						break;
				}
				break;
			case CUBIC_METER_PER_SECOND:
				switch (mbusUnit) {
					case CUBIC_METRE_PER_DAY:
						nominator = 1;
						denominator = 86400;
						break;
					case CUBIC_METRE_PER_HOUR:
						nominator = 1;
						denominator = 3600;
						break;
					case CUBIC_METRE_PER_MINUTE:
						nominator = 1;
						denominator = 60;
						break;
					case CUBIC_METRE_PER_SECOND:
						nominator = 1;
						denominator = 1;
						break;
				}
				break;
			case SECONDS:
				switch (mbusUnit) {
					case SECOND:
						nominator = 1;
						denominator = 1;
						break;
					case MIN:
						nominator = 60;
						denominator = 1;
						break;
					case HOUR:
						nominator = 3600;
						denominator = 1;
						break;
					case DAY:
						nominator = 86400;
						denominator = 1;
						break;
				}
				break;
			case MINUTE:
				switch (mbusUnit) {
					case SECOND:
						nominator = 1;
						denominator = 60;
						break;
					case MIN:
						nominator = 1;
						denominator = 1;
						break;
					case HOUR:
						nominator = 60;
						denominator = 1;
						break;
					case DAY:
						nominator = 1440;
						denominator = 1;
						break;
				}
				break;
			case HOUR:
				switch (mbusUnit) {
					case SECOND:
						nominator = 1;
						denominator = 3660;
						break;
					case MIN:
						nominator = 1;
						denominator = 60;
						break;
					case HOUR:
						nominator = 1;
						denominator = 1;
						break;
					case DAY:
						nominator = 60;
						denominator = 1;
						break;
				}
				break;
			case DEGREE_CELSIUS:
				switch (mbusUnit) {
					case DEGREE_CELSIUS:
						nominator = 1;
						denominator = 1;
						break;
				}
				break;
			case BAR:
				switch (mbusUnit) {
					case BAR:
						nominator = 1;
						denominator = 1;
						break;
				}
				break;
			case WATT:
				switch (mbusUnit) {
					case WATT:
						nominator = 1;
						denominator = 1;
						break;
					case JOULE_PER_HOUR:
						nominator = 1;
						denominator = 3600;
						break;
				}
				break;
			case WATT_SECONDS:
				switch (mbusUnit) {
					case JOULE:
						nominator = 1;
						denominator = 1;
						break;
					case WATT_HOUR:
						nominator = 3600;
						denominator = 1;
						break;
				}
				break;
			case VOLT_AMPERE_REACTIVE:
				switch (mbusUnit) {
					case VAR:
						nominator = 1;
						denominator = 1;
						break;
				}
				break;
			case VOLT_AMPERE_REACTIVE_HOURS:
				switch (mbusUnit) {
					case VAR_HOUR:
						nominator = 1;
						denominator = 1;
						break;
				}
				break;
			case VOLT_AMPERE:
				switch (mbusUnit) {
					case VOLT_AMPERE:
						nominator = 1;
						denominator = 1;
						break;
				}
				break;
			case VOLT_AMPERE_HOURS:
				switch (mbusUnit) {
					case VOLT_AMPERE_HOUR:
						nominator = 1;
						denominator = 1;
						break;
				}
				break;
			case WATT_HOURS:
				switch (mbusUnit) {
					case JOULE:
						nominator = 1;
						denominator = 3600;
						break;
					case WATT_HOUR:
						nominator = 1;
						denominator = 1;
						break;
				}
				break;
			case VOLT:
				switch (mbusUnit) {
					case VOLT:
						nominator = 1;
						denominator = 1;
						break;
				}
				break;
			case AMPERE:
				switch (mbusUnit) {
					case AMPERE:
						nominator = 1;
						denominator = 1;
						break;
				}
				break;
		}
		if (nominator > 0 & denominator > 0) {
			channel.setNextValue(((Number) record.getDataValue()).doubleValue() * nominator * Math.pow(10, scaleFactor) / denominator);
		} else {
			this.errorMessage.append("Error: Unit mismatch. Cannot write data with unit " + mbusUnit.toString()
					+ " into channel " + channel.channelId().toString()
					+ " with unit " + channel.channelDoc().getUnit().toString() + ". ");
		}
	}


}
