package io.openems.edge.controller.api.modbus.readwrite;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.CUMULATED_SECONDS;
import static io.openems.common.channel.Unit.VOLT_AMPERE_REACTIVE;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.STRING;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.channel.ChannelId.channelIdCamelToUpper;
import static io.openems.edge.common.channel.ChannelId.channelIdUpperToCamel;
import static io.openems.edge.common.channel.ChannelUtils.setValue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.controller.api.common.Status;
import io.openems.edge.controller.api.common.WriteObject;
import io.openems.edge.controller.api.modbus.AbstractModbusApi;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

public abstract class AbstractModbusReadWriteApi extends AbstractModbusApi implements TimedataProvider {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		OVERRIDE_STATUS(Doc.of(Status.values())//
				.persistencePriority(HIGH)), //

		CUMULATED_ACTIVE_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS)//
				.persistencePriority(HIGH)), //

		CUMULATED_INACTIVE_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS)//
				.persistencePriority(HIGH)), //

		API_WORKER_LOG(Doc.of(STRING)//
				.text("Logs Write-Commands via ApiWorker")), //

		ESS0_ACTUAL_SET_ACTIVE_POWER_EQUALS(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(HIGH)//
				.text("Last value written via Modbus API to ess0/SetActivePowerEquals")), //

		ESS0_ACTUAL_SET_REACTIVE_POWER_EQUALS(Doc.of(INTEGER)//
				.unit(VOLT_AMPERE_REACTIVE)//
				.persistencePriority(HIGH)//
				.text("Last value written via Modbus API to ess0/SetReactivePowerEquals")), //

		ESS0_ACTUAL_SET_ACTIVE_POWER_LESS_OR_EQUALS(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(HIGH)//
				.text("Last value written via Modbus API to ess0/SetActivePowerLessOrEquals")), //

		ESS0_ACTUAL_SET_ACTIVE_POWER_GREATER_OR_EQUALS(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(HIGH)//
				.text("Last value written via Modbus API to ess0/SetActivePowerGreaterOrEquals")), //

		ESS0_ACTUAL_SET_REACTIVE_POWER_LESS_OR_EQUALS(Doc.of(INTEGER)//
				.unit(VOLT_AMPERE_REACTIVE)//
				.persistencePriority(HIGH)//
				.text("Last value written via Modbus API to ess0/SetReactivePowerLessOrEquals")), //

		ESS0_ACTUAL_SET_REACTIVE_POWER_GREATER_OR_EQUALS(Doc.of(INTEGER)//
				.unit(VOLT_AMPERE_REACTIVE)//
				.persistencePriority(HIGH)//
				.text("Last value written via Modbus API to ess0/SetActivePowerGreaterOrEquals"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#API_WORKER_LOG}.
	 *
	 * @return the Channel
	 */
	public StringReadChannel getApiWorkerLogChannel() {
		return this.channel(ChannelId.API_WORKER_LOG);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS0_ACTUAL_SET_ACTIVE_POWER_EQUALS}.
	 *
	 * @return the Channel
	 */
	public IntegerReadChannel getActualSetActivePowerEqualsChannel() {
		return this.channel(ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_EQUALS);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS0_ACTUAL_SET_REACTIVE_POWER_EQUALS}.
	 *
	 * @return the Channel
	 */
	public IntegerReadChannel getActualSetReactivePowerEqualsChannel() {
		return this.channel(ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_EQUALS);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#ESS0_ACTUAL_SET_ACTIVE_POWER_LESS_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public IntegerReadChannel getActualSetActivePowerLessOrEqualsChannel() {
		return this.channel(ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_LESS_OR_EQUALS);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#ESS0_ACTUAL_SET_ACTIVE_POWER_GREATER_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public IntegerReadChannel getActualSetActivePowerGreaterOrEqualsChannel() {
		return this.channel(ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_GREATER_OR_EQUALS);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#ESS0_ACTUAL_SET_REACTIVE_POWER_LESS_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public IntegerReadChannel getActualSetReactivePowerLessOrEqualsChannel() {
		return this.channel(ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_LESS_OR_EQUALS);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#ESS0_ACTUAL_SET_REACTIVE_POWER_GREATER_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public IntegerReadChannel getActualSetReactivePowerGreaterOrEqualsChannel() {
		return this.channel(ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_GREATER_OR_EQUALS);
	}

	private final CalculateActiveTime calculateCumulatedActiveTime = new CalculateActiveTime(this,
			AbstractModbusReadWriteApi.ChannelId.CUMULATED_ACTIVE_TIME);

	private final CalculateActiveTime calculateCumulatedInactiveTime = new CalculateActiveTime(this,
			AbstractModbusReadWriteApi.ChannelId.CUMULATED_INACTIVE_TIME);

	private final CopyOnWriteArrayList<String> writeChannels = new CopyOnWriteArrayList<String>();

	private boolean isActive = false;

	protected AbstractModbusReadWriteApi(//
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	protected void applyConfig(String[] writeChannels) {
		synchronized (this.writeChannels) {
			this.writeChannels.clear();
			for (var c : writeChannels) {
				this.writeChannels.add(c);
			}
		}
	}

	@Override
	protected final AccessMode getAccessMode() {
		return AccessMode.READ_WRITE;
	}

	protected abstract ConfigurationAdmin getConfigurationAdmin();

	@Override
	public void run() throws OpenemsNamedException {
		this.isActive = false;
		super.run();

		this.calculateCumulatedActiveTime.update(this.isActive);
		this.calculateCumulatedInactiveTime.update(!this.isActive);
	}

	@Override
	protected void handleWrites(Entry<WriteChannel<?>, WriteObject> entry) {
		this.isActive = true;
		WriteChannel<?> channel = entry.getKey();
		var writeObject = entry.getValue();

		var channelNameCamel = getChannelNameCamel(channel.getComponent().id(), channel.channelId());

		@SuppressWarnings("deprecation")
		var logChannel = this._channel(channelNameCamel);
		if (logChannel == null) {
			var channelNameUpper = getChannelNameUpper(channel.getComponent().id(), channel.channelId());
			var currentChannel = new ChannelIdImpl(channelNameUpper,
					Doc.of(channel.getType()).persistencePriority(HIGH));
			addChannel(currentChannel);
			logChannel = channel(currentChannel);
		}
		logChannel.setNextValue(writeObject.value());
		this.configUpdate("writeChannels", logChannel.channelId().id());
		this.mirrorActualSetChannel(channel, writeObject);
	}

	/**
	 * Updating the configuration property to given value.
	 *
	 * @param targetProperty Property that should be changed
	 * @param requiredValue  Value that should be set
	 */
	private void configUpdate(String targetProperty, String requiredValue) {
		Configuration c;
		try {
			var pid = this.servicePid();
			if (pid.isEmpty()) {
				this.logInfo(this.log, "PID of " + this.id() + " is Empty");
				return;
			}
			c = this.getConfigurationAdmin().getConfiguration(pid, "?");
			var properties = c.getProperties();
			if (!this.writeChannels.contains(requiredValue)) {
				this.writeChannels.add(requiredValue);
				properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_BY, "Internal ModbusReadWriteApi");
				properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_AT,
						LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
				properties.put(targetProperty, this.writeChannels.toArray(String[]::new));
				c.update(properties);
			}
		} catch (IOException | SecurityException e) {
			this.logError(this.log, "ERROR: " + e.getMessage());
		}
	}

	@Override
	protected void setOverrideStatus(Status status) {
		setValue(this, AbstractModbusReadWriteApi.ChannelId.OVERRIDE_STATUS, status);
	}

	@Override
	protected void handleTimeouts() {
		this.resetWriteChannels();
		this.resetActualReadChannels();
	}

	protected void resetActualReadChannels() {
		this.channels().forEach(channel -> {
			switch (channel.channelId()) {
			case ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_EQUALS, //
					ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_EQUALS, //
					ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_LESS_OR_EQUALS, //
					ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_LESS_OR_EQUALS, //
					ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_GREATER_OR_EQUALS, //
					ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_GREATER_OR_EQUALS -> //
				channel.setNextValue(null);
			default -> {
				// ignore
			}
			}
		});
	}

	private void resetWriteChannels() {
		this.channels() //
				.stream() //
				.filter(channel -> this.writeChannels.contains(channel.channelId().id())) //
				.forEach(channel -> channel.setNextValue(null));
	}

	private void mirrorActualSetChannel(WriteChannel<?> channel, WriteObject writeObject) {
		Object value = writeObject.value();
		if (!(value instanceof Number v)) {
			return;
		}

		if (!Objects.equals(channel.getComponent().id(), "ess0")) {
			return;
		}
		var channelId = channel.channelId();
		if (!(channelId instanceof ManagedSymmetricEss.ChannelId essChannelId)) {
			return;
		}

		switch (essChannelId) {
		case SET_ACTIVE_POWER_EQUALS ->
			setValue(this, AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_EQUALS, v);

		case SET_REACTIVE_POWER_EQUALS ->
			setValue(this, AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_EQUALS, v);

		case SET_ACTIVE_POWER_LESS_OR_EQUALS ->
			setValue(this, AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_LESS_OR_EQUALS, v);

		case SET_ACTIVE_POWER_GREATER_OR_EQUALS ->
			setValue(this, AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_GREATER_OR_EQUALS, v);

		case SET_REACTIVE_POWER_LESS_OR_EQUALS ->
			setValue(this, AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_LESS_OR_EQUALS, v);

		case SET_REACTIVE_POWER_GREATER_OR_EQUALS ->
			setValue(this, AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_GREATER_OR_EQUALS, v);

		default -> doNothing();
		}
	}

	/**
	 * Provides a Modbus table for the Channels of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(AbstractModbusReadWriteApi.class, accessMode, 100) //
				.channel(0, AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_EQUALS, //
						ModbusType.FLOAT32) //
				.channel(2, AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_EQUALS, //
						ModbusType.FLOAT32) //
				.channel(4, AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_LESS_OR_EQUALS,
						ModbusType.FLOAT32) //
				.channel(6, AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_LESS_OR_EQUALS,
						ModbusType.FLOAT32) //
				.channel(8, AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_GREATER_OR_EQUALS,
						ModbusType.FLOAT32) //
				.channel(10, AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_GREATER_OR_EQUALS,
						ModbusType.FLOAT32) //
				.build();
	}

	protected static String getChannelNameUpper(String componentId,
			io.openems.edge.common.channel.ChannelId channelId) {
		return channelIdCamelToUpper(componentId) + "_" + channelId.name();
	}

	protected static String getChannelNameCamel(String componentId,
			io.openems.edge.common.channel.ChannelId channelId) {
		return channelIdUpperToCamel(getChannelNameUpper(componentId, channelId));
	}

}
