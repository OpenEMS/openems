package io.openems.edge.edge2edge.ess;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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

import com.google.common.collect.Lists;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusRecord;
import io.openems.edge.common.modbusslave.ModbusRecordChannel;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Edge2Edge.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Edge2EdgeEssImpl extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, AsymmetricEss, SymmetricEss, Edge2EdgeEss, ModbusComponent, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(Edge2EdgeEssImpl.class);
	private final ModbusProtocol modbusProtocol;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected Power power;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	public Edge2EdgeEssImpl() throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Edge2EdgeEss.ChannelId.values() //
		);
		this.modbusProtocol = new ModbusProtocol(this);
		this._setMaxApparentPower(Integer.MAX_VALUE); // has no effect, as long as AllowedCharge/DischargePower are null
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.isOpenems().thenAccept(isOpenems -> {
			this.channel(Edge2EdgeEss.ChannelId.REMOTE_NO_OPENEMS).setNextValue(!isOpenems);
			if (!isOpenems) {
				return;
			}

			try {
				ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(1), true).thenAccept(value -> {
					if (value == null) {
						return;
					}
					this.findComponentBlock(config.remoteComponentId(), 1 + value) //
							.whenComplete((startAddress, e1) -> {
								this.channel(Edge2EdgeEss.ChannelId.REMOTE_COMPONENT_ID_NOT_FOUND)
										.setNextValue(e1 != null);
								if (e1 != null) {
									// TODO restart with timeout finding the component block on exception
									e1.printStackTrace();
									return;
								}

								// Found Component Block -> read each nature block
								this.readNatureBlocks(startAddress).whenComplete((ignore, e2) -> {
									if (e2 != null) {
										// TODO restart with timeout finding the component block on exception
										e2.printStackTrace();
										return;
									}

									System.out.println("FINISHED");
								});
							});
				});
			} catch (OpenemsException e) {
				e.printStackTrace();
			}
		});
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Validates that this device complies to SunSpec specification.
	 *
	 * <p>
	 * Tests if first registers are 0x53756e53 ("SunS").
	 *
	 * @return a future true if it is SunSpec; otherwise false
	 * @throws OpenemsException on error
	 */
	private CompletableFuture<Boolean> isOpenems() throws OpenemsException {
		final var result = new CompletableFuture<Boolean>();
		ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(0), true).thenAccept(value -> {
			result.complete(isHashEqual(value, "OpenEMS"));
		});
		return result;
	}

	/**
	 * Compares a Integer (e.g. read from Modbus) with the hash-code of a text.
	 * 
	 * @param value an Integer, possibly null
	 * @param text  the text for the hash-code
	 * @return true on match
	 */
	protected static boolean isHashEqual(Integer value, String text) {
		if (value == null) {
			return false;
		}
		return (short) (int) value == ModbusSlaveNatureTable.generateHash(text);
	}

	@Override
	protected final ModbusProtocol defineModbusProtocol() {
		return this.modbusProtocol;
	}

	/**
	 * Reads the next OpenEMS Modbus-API block.
	 *
	 * @param startAddress the startAddress
	 * @return a future that completes once reading the block finished
	 */
	private CompletableFuture<Integer> findComponentBlock(String componentId, int startAddress) {
		final var result = new CompletableFuture<Integer>();
		this._findComponentBlock(result, componentId, startAddress);
		return result;
	}

	private void _findComponentBlock(CompletableFuture<Integer> result, String componentId, int startAddress) {
		try {
			ModbusUtils.readELementOnce(this.modbusProtocol, new StringWordElement(startAddress, 16), false)
					.thenAccept(remoteComponentId -> {
						if (remoteComponentId == null) {
							result.completeExceptionally(
									new OpenemsException("Unable to find remote Component with ID " + componentId));
						}
						if (remoteComponentId.equals(componentId)) {
							this.logInfo(this.log,
									"Found Remote-Component '" + componentId + "' on address " + startAddress);
							result.complete(startAddress);
							return;
						}
						try {
							ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(startAddress + 16),
									false).thenAccept(lengthOfBlock -> {
										this._findComponentBlock(result, componentId, startAddress + lengthOfBlock);
									});
						} catch (OpenemsException e) {
							result.completeExceptionally(e);
						}
					});
		} catch (OpenemsException e) {
			result.completeExceptionally(e);
		}
	}

	private CompletableFuture<Void> readNatureBlocks(int startAddress) {
		final var result = new CompletableFuture<Void>();
		try {
			ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(startAddress + 16), false)
					.thenAccept(lengthOfComponentBlock -> {
						var lastAddress = startAddress + lengthOfComponentBlock + 20;
						// TODO fix length of last component blocks in Slave Modbus/TCP-Api
						this.readNatureStartAddresses(startAddress + 20, lastAddress)
								.thenAccept(natureStartAddresses -> {
									try {
										this.mapRemoteChannels(natureStartAddresses);
										result.complete(null);

									} catch (OpenemsException e) {
										result.completeExceptionally(e);
									}
								});
					});
		} catch (OpenemsException e) {
			result.completeExceptionally(e);
		}
		return result;
	}

	/**
	 * Do the actual mapping of remote Nature Channels to local Channels.
	 * 
	 * @param natureStartAddresses a map of Nature-Hashes to Modbus start addresses
	 * @throws OpenemsException on error
	 */
	private void mapRemoteChannels(TreeMap<Integer, Short> natureStartAddresses) throws OpenemsException {
		List<Function<AccessMode, ModbusSlaveNatureTable>> methods = Lists.newArrayList(//
				OpenemsComponent::getModbusSlaveNatureTable, //
				SymmetricEss::getModbusSlaveNatureTable, //
				AsymmetricEss::getModbusSlaveNatureTable, //
				ManagedSymmetricEss::getModbusSlaveNatureTable, //
				StartStoppable::getModbusSlaveNatureTable //
		);

		var modbusSlaveNatureTables = methods.stream() //
				.map(method -> method.apply(this.config.remoteAccessMode())) //
				.collect(Collectors.toUnmodifiableList());

		Deque<AbstractModbusElement<?>> readElements = new ArrayDeque<>();
		Deque<AbstractModbusElement<?>> writeElements = new ArrayDeque<>();
		for (var entry : natureStartAddresses.entrySet()) {
			var natureStartAddress = entry.getKey();
			var hash = entry.getValue();
			var modbusSlaveNatureTableOpt = modbusSlaveNatureTables.stream() //
					.filter(t -> t.getNatureHash() == hash) //
					.findFirst();
			if (modbusSlaveNatureTableOpt.isEmpty()) {
				continue;
			}
			var modbusSlaveNatureTable = modbusSlaveNatureTableOpt.get();

			for (var record : modbusSlaveNatureTable.getModbusRecords()) {
				var address = natureStartAddress + 2 /* hash & length */ + record.getOffset();

				/*
				 * Add element to Read-Task
				 */
				if (record.getAccessMode() == AccessMode.READ_ONLY || record.getAccessMode() == AccessMode.READ_WRITE) {
					var element = generateModbusElement(record.getType(), address);

					// Fill gaps with DummyModbusElements
					var lastElement = readElements.peekLast();
					if (lastElement != null) {
						var gap = address - lastElement.getStartAddress() - lastElement.getLength();
						if (gap > 0) {
							readElements.add(new DummyRegisterElement(//
									lastElement.getStartAddress() + lastElement.getLength(),
									lastElement.getStartAddress() + lastElement.getLength() + gap - 1));
						}
					}

					if (record instanceof ModbusRecordChannel) {
						var r = (ModbusRecordChannel) record;
						m(r.getChannelId(), element);

					} else {
						var onUpdateCallback = this.getOnUpdateCallback(modbusSlaveNatureTable, record);
						if (onUpdateCallback != null) {
							m(element).build().onUpdateCallback(value -> onUpdateCallback.accept(value));
						}
					}

					readElements.add(element);
				}

				/*
				 * Add element to Write-Task
				 */
				if (record.getAccessMode() == AccessMode.WRITE_ONLY
						|| record.getAccessMode() == AccessMode.READ_WRITE) {
					var element = generateModbusElement(record.getType(), address);
					var channelId = this.getWriteChannelId(modbusSlaveNatureTable, record);
					if (channelId != null) {
						m(channelId, element);
						writeElements.add(element);
					}
				}
			}
		}

		/*
		 * Add the Read-Task(s)
		 */
		{
			var length = 0;
			var taskElements = new ArrayDeque<AbstractModbusElement<?>>();
			var element = readElements.pollFirst();
			while (element != null) {
				if (length + element.getLength() > 126 /* limit of j2mod */) {
					this.addReadTask(taskElements);
					length = 0;
					taskElements.clear();
				}
				taskElements.add(element);
				length += element.getLength();
				element = readElements.pollFirst();
			}
			this.addReadTask(taskElements);
		}

		/*
		 * Add the Write-Task(s)
		 */
		{
			var taskElements = new ArrayDeque<AbstractModbusElement<?>>();
			var element = writeElements.pollFirst();
			while (element != null) {
				var lastElement = taskElements.peekLast();
				if (lastElement != null
						&& (lastElement.getStartAddress() + lastElement.getLength() < element.getStartAddress())) {
					// Found gap
					this.addWriteTask(taskElements);
					taskElements.clear();
				}
				taskElements.add(element);
				element = writeElements.pollFirst();
			}
			this.addWriteTask(taskElements);
		}

		System.out.println("X");
	}

	private Consumer<Object> getOnUpdateCallback(ModbusSlaveNatureTable modbusSlaveNatureTable, ModbusRecord record) {
		if (modbusSlaveNatureTable.getNatureClass() == ManagedSymmetricEss.class) {
			switch (record.getOffset()) {
			case 0: // "Minimum Power Set-Point"
				return (value) -> this._setAllowedChargePower(TypeUtils.getAsType(OpenemsType.INTEGER, value));

			case 2: // "Maximum Power Set-Point"
				return (value) -> this._setAllowedDischargePower(TypeUtils.getAsType(OpenemsType.INTEGER, value));
			}
		}
		return null;
	}

	private io.openems.edge.common.channel.ChannelId getWriteChannelId(ModbusSlaveNatureTable modbusSlaveNatureTable,
			ModbusRecord record) {
		if (record instanceof ModbusRecordChannel) {
			var c = ((ModbusRecordChannel) record).getChannelId();
			if (c == ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS) {
				return Edge2EdgeEss.ChannelId.REMOTE_SET_ACTIVE_POWER_EQUALS;
			}
		}
		return null;
	}

	/**
	 * Create ModbusElement from type and address.
	 * 
	 * @param type    the {@link ModbusType}
	 * @param address the address of the {@link ModbusElement}
	 * @return the {@link ModbusElement}
	 */
	private static AbstractModbusElement<?> generateModbusElement(ModbusType type, int address) {
		switch (type) {
		case ENUM16:
		case UINT16:
			return new UnsignedWordElement(address);
		case UINT32:
			return new UnsignedDoublewordElement(address);
		case FLOAT32:
			return new FloatDoublewordElement(address);
		case FLOAT64:
			return new UnsignedQuadruplewordElement(address);
		case STRING16:
			return new StringWordElement(address, 16);
		}
		return null;
	}

	/**
	 * Adds a Reak-Task with ModbusElements.
	 * 
	 * <ul>
	 * <li>Makes sure there is no DummyRegisterElement in beginning or end of the
	 * queue
	 * <li>Adds only if queue is not empty
	 * </ul>
	 * 
	 * @throws OpenemsException on error
	 */
	private void addReadTask(Deque<AbstractModbusElement<?>> elements) throws OpenemsException {
		if (elements.isEmpty()) {
			return;
		}
		while (elements.peekFirst() instanceof DummyRegisterElement) {
			elements.removeFirst();
		}
		while (elements.peekLast() instanceof DummyRegisterElement) {
			elements.removeLast();
		}
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(//
						elements.peekFirst().getStartAddress(), Priority.HIGH,
						elements.toArray(new AbstractModbusElement[elements.size()])));
	};

	/**
	 * Adds a Write-Task with ModbusElements.
	 * 
	 * @throws OpenemsException on error
	 */
	private void addWriteTask(Deque<AbstractModbusElement<?>> elements) throws OpenemsException {
		if (elements.isEmpty()) {
			return;
		}
		this.modbusProtocol.addTask(//
				new FC16WriteRegistersTask(//
						elements.peekFirst().getStartAddress(),
						elements.toArray(new AbstractModbusElement[elements.size()])));
	};

	/**
	 * Reads all Nature Start Addresses of a Component-Block.
	 * 
	 * @param startAddress the address of the first Nature-Block inside the
	 *                     Component-Block
	 * @param lastAddress  the start address of the following Component-Block
	 * @return a map of modbus start address to Nature-Hash
	 */
	private CompletableFuture<TreeMap<Integer, Short>> readNatureStartAddresses(int startAddress, int lastAddress) {
		final var result = new CompletableFuture<TreeMap<Integer, Short>>();
		this._readNatureStartAddresses(result, startAddress, lastAddress, new TreeMap<>());
		return result;
	}

	private void _readNatureStartAddresses(CompletableFuture<TreeMap<Integer, Short>> result, int startAddress,
			int lastAddress, final TreeMap<Integer, Short> natureStartAddresses) {
		try {
			ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(startAddress), false)
					.thenAccept(rawHash -> {
						if (rawHash == null) {
							result.completeExceptionally(
									new OpenemsException("Unable to read hash at " + startAddress));
							return;
						}
						var hash = (short) (int) rawHash;

						try {
							ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(startAddress + 1),
									false).thenAccept(lengthOfNatureBlock -> {
										this.logInfo(this.log, "Found Remote-Nature '0x"
												+ Integer.toHexString(hash & 0xffff) + "' on address " + startAddress);
										natureStartAddresses.put(startAddress, hash);

										var nextStartAddress = startAddress + lengthOfNatureBlock;
										if (nextStartAddress >= lastAddress) {
											result.complete(natureStartAddresses);

										} else {
											// recursive call of _readNatureStartAddresses
											this._readNatureStartAddresses(result, nextStartAddress, lastAddress,
													natureStartAddresses);
										}
									});
						} catch (OpenemsException e) {
							result.completeExceptionally(e);
						}
					});
		} catch (OpenemsException e) {
			result.completeExceptionally(e);
		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";" //
				+ this.getAllowedDischargePower().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		this.setRemoteActivePowerEquals((float) activePower);
		this.setRemoteReactivePowerEquals((float) reactivePower);
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

}
