package io.openems.edge.io.weidmueller;

import static io.openems.edge.bridge.modbus.api.ModbusUtils.readElementOnce;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.readElementsOnce;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.FunctionCode.FC3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.IteratorUtils;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ChannelMetaInfoBitReadAndWrite;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.ModbusUtils.ReadElementsResult;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Weidmueller.UR20", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class IoWeidmuellerUr20Impl extends AbstractOpenemsModbusComponent
		implements IoWeidmuellerUr20, DigitalOutput, DigitalInput, ModbusComponent, OpenemsComponent {

	// TODO Error/Warning state handling
	// TODO Implement hardware watchdog
	// - 0x1030: remaining watchdog time (read, 10ms steps; 0x0000=expired,
	// 0xFFFF=disabled)
	// - 0x1120: watchdog timeout (write to enable/retrigger; every write resets the
	// timer; 0=disable)

	private static final int PROCESS_DATA_INPUT_BASE_REGISTER = 0x8000;
	private static final int PROCESS_DATA_OUTPUT_BASE_REGISTER = 0x9000;
	private static final int PROCESS_DATA_OUTPUT_BASE_COIL = 0x8000;

	private final Logger log = LoggerFactory.getLogger(IoWeidmuellerUr20Impl.class);
	private final ModbusProtocol modbusProtocol;

	public record Modules(URemoteModule module, List<BooleanReadChannel> channels) {

	}

	private final List<Modules> modules = Collections.synchronizedList(new ArrayList<>());

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private BooleanReadChannel[] digitalInputChannels;
	private BooleanWriteChannel[] digitalOutputChannels;

	public IoWeidmuellerUr20Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				IoWeidmuellerUr20.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				DigitalInput.ChannelId.values() //
		);
		this.modbusProtocol = new ModbusProtocol(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		this.readNumberOfEntriesInTheCurrentModuleList().thenAccept(numberOfEntries -> {
			this.readCurrentModuleList(numberOfEntries).thenAccept(moduleIds -> {
				var writeChannels = new ArrayList<BooleanWriteChannel>();
				var nextOutputCoil = PROCESS_DATA_OUTPUT_BASE_COIL;

				for (var entry : IteratorUtils.indexedIterable(moduleIds)) {
					final var moduleCount = entry.index();
					final var moduleId = entry.value();
					// Parse URemoteModule from moduleId
					var moduleOpt = URemoteModule.getByModuleId(moduleId);
					if (moduleOpt.isEmpty()) {
						this.logError(this.log, "Unable to identify U-Remote-Module #" + moduleCount //
								+ " [0x" + Long.toHexString(moduleId) + "]");
						continue;
					}
					var module = moduleOpt.get();
					var currentModule = new Modules(module, new ArrayList<>());
					this.modules.add(currentModule);

					var inputRegisterOffset = PROCESS_DATA_INPUT_BASE_REGISTER + moduleCount * 32;
					var outputRegisterOffset = PROCESS_DATA_OUTPUT_BASE_REGISTER + moduleCount * 32;
					Task[] tasks = null;
					switch (module) {

					case UR20_4DI_P: {
						var element = new BitsWordElement(inputRegisterOffset, this);
						for (var i = 0; i < 4; i++) {
							var channelId = FieldbusChannelId.forDigitalInput(moduleCount, i + 1);
							var channel = (BooleanReadChannel) this.addChannel(channelId);
							currentModule.channels.add(channel);
							element.bit(i, channelId);
						}

						tasks = new Task[] { new FC3ReadRegistersTask(element.startAddress, Priority.HIGH, element) };
						break;
					}

					case UR20_8DO_P: {
						var myTasks = new ArrayList<Task>();
						var inputElement = new BitsWordElement(outputRegisterOffset, this);
						var moduleCoilBase = nextOutputCoil;
						for (var i = 0; i < 8; i++) {
							var channelId = FieldbusChannelId.forDigitalOutput(moduleCount, i + 1);
							var channel = (BooleanWriteChannel) this.addChannel(channelId);

							var outputElement = new CoilElement(nextOutputCoil++);
							var channelMetaInfoBit = new ChannelMetaInfoBitReadAndWrite(inputElement.startAddress, i,
									moduleCoilBase, outputElement.startAddress);
							writeChannels.add(channel);
							myTasks.add(new FC5WriteCoilTask(outputElement.startAddress,
									m(channelId, outputElement, channelMetaInfoBit)));

							currentModule.channels.add(channel);
							inputElement.bit(i, channelId, channelMetaInfoBit);
						}
						myTasks.add(new FC3ReadRegistersTask(inputElement.startAddress, Priority.HIGH, inputElement));
						tasks = myTasks.toArray(Task[]::new);
						break;
					}

					case UR20_16DI_P: {
						var element = new BitsWordElement(inputRegisterOffset, this);
						for (var i = 0; i < 16; i++) {
							var channelId = FieldbusChannelId.forDigitalInput(moduleCount, i + 1);
							var channel = (BooleanReadChannel) this.addChannel(channelId);
							currentModule.channels.add(channel);
							element.bit(i, channelId);
						}
						tasks = new Task[] { new FC3ReadRegistersTask(element.startAddress, Priority.HIGH, element) };
						break;
					}
					}

					if (tasks == null || tasks.length == 0) {
						this.logError(this.log, "Unable to build Modbus-Task for U-Remote-Module #" + moduleCount //
								+ " [" + module.name() + "]");
						continue;
					}

					this.modbusProtocol.addTasks(tasks);

					this.digitalInputChannels = this.modules.stream() //
							.flatMap(m -> m.channels().stream()) //
							.toArray(BooleanReadChannel[]::new);
					this.digitalOutputChannels = writeChannels.toArray(BooleanWriteChannel[]::new);
				}

			});
		});
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return this.modbusProtocol;
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		return this.digitalInputChannels;
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}

	@Override
	public String debugLog() {
		if (this.modules.isEmpty()) {
			return "";
		}
		var b = new StringBuilder();
		var i = 0;
		for (var module : this.modules) {
			var channels = module.channels;
			if (channels.isEmpty()) {
				continue;
			}
			b.append("M" + i + ":");
			for (var channel : channels) {
				var valueOpt = channel.value().asOptional();
				if (valueOpt.isPresent()) {
					b.append(valueOpt.get() ? "x" : "-");
				} else {
					b.append("?");
				}
			}
			if (i < this.modules.size() - 1) {
				b.append("|");
			}
			i++;
		}
		return b.toString();
	}

	private CompletableFuture<Integer> readNumberOfEntriesInTheCurrentModuleList() {
		return readElementOnce(FC3, this.modbusProtocol, ModbusUtils::retryOnNull, new UnsignedWordElement(0x27FE));
	}

	@SuppressWarnings("unchecked")
	private CompletableFuture<List<Long>> readCurrentModuleList(int numberOfEntries) {
		var elements = IntStream.range(0, numberOfEntries) //
				.map(index -> 0x2A00 + index * 2) //
				.mapToObj(address -> new UnsignedDoublewordElement(address)) //
				.toArray(ModbusRegisterElement[]::new);
		return readElementsOnce(FC3, this.modbusProtocol, ModbusUtils::retryOnNull, elements) //
				.thenApply(rsr -> ((ReadElementsResult<Long>) rsr).values());
	}

}
