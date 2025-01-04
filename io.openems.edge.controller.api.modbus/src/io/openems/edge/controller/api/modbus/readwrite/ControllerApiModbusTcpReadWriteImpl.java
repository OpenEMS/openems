package io.openems.edge.controller.api.modbus.readwrite;

import static io.openems.edge.common.channel.ChannelId.channelIdCamelToUpper;
import static io.openems.edge.common.channel.ChannelId.channelIdUpperToCamel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.common.Status;
import io.openems.edge.controller.api.common.WriteObject;
import io.openems.edge.controller.api.modbus.AbstractModbusTcpApi;
import io.openems.edge.controller.api.modbus.ModbusTcpApi;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.ModbusTcp.ReadWrite", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerApiModbusTcpReadWriteImpl extends AbstractModbusTcpApi
		implements ControllerApiModbusTcpReadWrite, ModbusTcpApi, Controller, OpenemsComponent, ComponentJsonApi,
		TimedataProvider, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(ControllerApiModbusTcpReadWriteImpl.class);

	private final CalculateActiveTime calculateCumulatedActiveTime = new CalculateActiveTime(this,
			ControllerApiModbusTcpReadWrite.ChannelId.CUMULATED_ACTIVE_TIME);

	private final CalculateActiveTime calculateCumulatedInactiveTime = new CalculateActiveTime(this,
			ControllerApiModbusTcpReadWrite.ChannelId.CUMULATED_INACTIVE_TIME);

	private List<String> writeChannels;

	private List<OpenemsComponent> components = new ArrayList<>();

	private boolean isActive = false;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private Meta metaComponent = null;

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE //
	)
	protected void addComponent(OpenemsComponent component) {
		super.addComponent(component);
		this.components.add(component);
	}

	protected void removeComponent(OpenemsComponent component) {
		super.removeComponent(component);
		this.components.remove(component);
	}

	public ControllerApiModbusTcpReadWriteImpl() {
		super("Modbus/TCP-Api Read-Write", //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ModbusTcpApi.ChannelId.values(), //
				ControllerApiModbusTcpReadWrite.ChannelId.values() //
		);
		this.apiWorker.setLogChannel(this.getApiWorkerLogChannel());
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws ModbusException, OpenemsException {
		super.activate(context, this.cm,
				new ConfigRecord(config.id(), config.alias(), config.enabled(), this.metaComponent,
						config.component_ids(), config.apiTimeout(), config.port(), config.maxConcurrentConnections()));
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, this.cm,
				new ConfigRecord(config.id(), config.alias(), config.enabled(), this.metaComponent,
						config.component_ids(), config.apiTimeout(), config.port(), config.maxConcurrentConnections()));
		this.applyConfig(config);
	}

	private void applyConfig(Config config) {
		this.writeChannels = new ArrayList<>(Arrays.asList(config.writeChannels()));
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.isActive = false;
		super.run();

		this.calculateCumulatedActiveTime.update(this.isActive);
		this.calculateCumulatedInactiveTime.update(!this.isActive);
	}

	@Override
	protected AccessMode getAccessMode() {
		return AccessMode.READ_WRITE;
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
			c = this.cm.getConfiguration(pid, "?");
			var properties = c.getProperties();
			if (!this.writeChannels.contains(requiredValue)) {
				this.writeChannels.add(requiredValue);
				properties.put(targetProperty, this.writeChannels.toArray(String[]::new));
				c.update(properties);
			}
		} catch (IOException | SecurityException e) {
			this.logError(this.log, "ERROR: " + e.getMessage());
		}
	}

	protected static String getChannelNameUpper(String componentId,
			io.openems.edge.common.channel.ChannelId channelId) {
		return channelIdCamelToUpper(componentId) + "_" + channelId.name();
	}

	protected static String getChannelNameCamel(String componentId,
			io.openems.edge.common.channel.ChannelId channelId) {
		return channelIdUpperToCamel(getChannelNameUpper(componentId, channelId));
	}

	@Override
	protected Consumer<Entry<WriteChannel<?>, WriteObject>> handleWrites() {
		return entry -> {
			this.isActive = true;
			WriteChannel<?> channel = entry.getKey();
			var writeObject = entry.getValue();

			var channelNameCamel = getChannelNameCamel(channel.getComponent().id(), channel.channelId());

			@SuppressWarnings("deprecation")
			var logChannel = this._channel(channelNameCamel);
			if (logChannel == null) {
				var channelNameUpper = getChannelNameUpper(channel.getComponent().id(), channel.channelId());
				var currentChannel = new ChannelIdImpl(channelNameUpper,
						Doc.of(channel.getType()).persistencePriority(PersistencePriority.HIGH));
				addChannel(currentChannel);
				logChannel = channel(currentChannel);
			}
			logChannel.setNextValue(writeObject.value());
			this.configUpdate("writeChannels", logChannel.channelId().id());
		};
	}

	@Override
	protected void setOverrideStatus(Status status) {
		this._setOverrideStatus(status);
	}

	@Override
	protected Runnable handleTimeouts() {
		return () -> {
			this.writeChannels.forEach(c -> {
				channels().stream().filter(channel -> channel.channelId().id().equals(c)).findFirst()
						.ifPresent(channel -> channel.setNextValue(null));
			});
		};
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	protected Integer getChannelValue(String componentId, io.openems.edge.common.channel.ChannelId channelId) {
		@SuppressWarnings("deprecation")
		var channel = this._channel(getChannelNameCamel(componentId, channelId));
		if (channel == null) {
			return null;
		}
		return ((IntegerReadChannel) channel).value().get();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(AccessMode.READ_ONLY),
				ModbusSlaveNatureTable.of(ControllerApiModbusTcpReadWriteImpl.class, AccessMode.READ_ONLY, 300)
						.cycleValue(0, this.id() + "/  Ess0ActivePowerLimit", Unit.WATT, "", ModbusType.FLOAT32,
								t -> this.getChannelValue("ess0",
										ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS))
						.cycleValue(2, this.id() + "/Ess0ReactivePowerLimit", Unit.WATT, "", ModbusType.FLOAT32,
								t -> this.getChannelValue("ess0",
										ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS))
						.build());
	}

}
