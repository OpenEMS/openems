package io.openems.impl.controller.api.modbustcp;

import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelDoc;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.api.ApiWorker;

@ThingInfo(title = "Modbus/TCP API", description = "Modbus/TCP slave implementation.")
public class ModbusTcpApiController extends Controller {

	public static final int UNIT_ID = 1;
	public static final int MAX_CONCURRENT_CONNECTIONS = 2;
	private final Logger log = LoggerFactory.getLogger(ModbusTcpApiController.class);

	private Optional<ModbusSlave> slaveOpt = Optional.empty();
	private final ApiWorker apiWorker = new ApiWorker();
	private final MyProcessImage processImage = new MyProcessImage(apiWorker);
	private ThingStateChannels thingState = new ThingStateChannels(this);

	/*
	 * Constructors
	 */
	public ModbusTcpApiController() {
		super();
	}

	public ModbusTcpApiController(String thingId) {
		super(thingId);
	}

	@Override
	public void init() {
		this.updateChannelMapping(this.mapping.valueOptional());
		this.restartSlave(this.port.valueOptional());
		super.init();
	}

	/*
	 * Config
	 */
	@SuppressWarnings("unchecked")
	@ChannelInfo(title = "Port", description = "Sets the port of the Modbus/TCP slave.", type = Integer.class, defaultValue = "502")
	public final ConfigChannel<Integer> port = new ConfigChannel<Integer>("port", this)
	.addChangeListener((channel, newValue, oldValue) -> {
		this.restartSlave((Optional<Integer>) newValue);
	});

	@SuppressWarnings("unchecked")
	@ChannelInfo(title = "Mapping", description = "Defines the Modbus-to-Channel-mapping.", type = JsonObject.class, defaultValue = "{ '0': 'system0/OpenemsVersionMajor' }")
	public final ConfigChannel<JsonObject> mapping = new ConfigChannel<JsonObject>("mapping", this)
	.addChangeListener((channel, newValue, oldValue) -> {
		this.updateChannelMapping((Optional<JsonObject>) newValue);
	});

	@ChannelInfo(title = "ChannelTimeout", description = "Sets the timeout for updates to channels.", type = Integer.class, defaultValue = ""
			+ ApiWorker.DEFAULT_TIMEOUT_SECONDS)
	public final ConfigChannel<Integer> channelTimeout = new ConfigChannel<Integer>("channelTimeout", this)
	.addChangeListener((Channel channel, Optional<?> newValue, Optional<?> oldValue) -> {
		if(newValue.isPresent() && Integer.parseInt(newValue.get().toString()) >= 0) {
			apiWorker.setTimeoutSeconds(Integer.parseInt(newValue.get().toString()));
		} else {
			apiWorker.setTimeoutSeconds(ApiWorker.DEFAULT_TIMEOUT_SECONDS);
		}
	});

	/*
	 * Methods
	 */
	@Override
	public void run() {
		this.apiWorker.run();
	}

	protected void restartSlave(Optional<Integer> portOpt) {
		// remove and close old slave if existing
		if (this.slaveOpt.isPresent()) {
			ModbusSlave oldSlave = this.slaveOpt.get();
			oldSlave.close();
			this.slaveOpt = Optional.empty();
		}
		// create new slave, initialize and start
		try {
			if (!portOpt.isPresent()) {
				throw new OpenemsException("Port was not set");
			}
			int port = portOpt.get();
			ModbusSlave newSlave = ModbusSlaveFactory.createTCPSlave(port, MAX_CONCURRENT_CONNECTIONS);
			newSlave.addProcessImage(UNIT_ID, this.processImage);
			newSlave.open();
			// TODO slave should be closed on dispose of Controller
			log.info("Modbus/TCP Api started on port [" + port + "] with UnitId [" + UNIT_ID + "].");
			this.slaveOpt = Optional.of(newSlave);
		} catch (OpenemsException | ModbusException e) {
			log.error("Unable to start Modbus/TCP slave: " + e.getMessage());
		}
	}

	protected void updateChannelMapping(Optional<JsonObject> jMappingOpt) {
		processImage.clearMapping();

		ThingRepository thingRepository = ThingRepository.getInstance();
		if (jMappingOpt.isPresent()) {
			JsonObject jMapping = jMappingOpt.get();
			for (Entry<String, JsonElement> entry : jMapping.entrySet()) {
				try {
					int ref = Integer.parseInt(entry.getKey());
					ChannelAddress channelAddress = ChannelAddress.fromString(JsonUtils.getAsString(entry.getValue()));
					Optional<ChannelDoc> channelDocOpt = thingRepository.getChannelDoc(channelAddress);
					if (channelDocOpt.isPresent()) {
						processImage.addMapping(ref, channelAddress, channelDocOpt.get());
					} else {
						Optional<Channel> channelOpt = thingRepository.getChannel(channelAddress);
						if (channelOpt.isPresent()) {
							throw new OpenemsException(
									"ChannelDoc for channel [" + channelAddress + "] is not available.");
						} else {
							throw new OpenemsException("Channel [" + channelAddress + "] does not exist.");
						}
					}
				} catch (Exception e) {
					log.error("Unable to add channel mapping: " + e.getMessage());
				}
			}
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}
}