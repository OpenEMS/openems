package io.openems.edge.edge2edge.common;

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
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
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
import io.openems.edge.common.taskmanager.Priority;

public abstract class AbstractEdge2Edge extends AbstractOpenemsModbusComponent
		implements Edge2Edge, ModbusComponent, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractEdge2Edge.class);

	private final List<Function<AccessMode, ModbusSlaveNatureTable>> modbusSlaveNatureTableMethods;
	private final ModbusProtocol modbusProtocol;

	private AccessMode remoteAccessMode;

	protected AbstractEdge2Edge(List<Function<AccessMode, ModbusSlaveNatureTable>> modbusSlaveNatureTableMethods,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) throws OpenemsException {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.modbusSlaveNatureTableMethods = modbusSlaveNatureTableMethods;
		this.modbusProtocol = new ModbusProtocol(this);
	}

	@Override
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId) throws OpenemsException {
		throw new IllegalArgumentException("Use the other activate() method.");
	}

	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId, String remoteComponentId,
			AccessMode remoteAccessMode) throws OpenemsException {
		this.remoteAccessMode = remoteAccessMode;
		if (super.activate(context, id, alias, enabled, unitId, cm, modbusReference, modbusId)) {
			return true;
		}

		this.isOpenems().thenAccept(isOpenems -> {
			this._setRemoteNoOpenems(!isOpenems);
			if (!isOpenems) {
				return;
			}

			try {
				ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(1), true).thenAccept(value -> {
					if (value == null) {
						return;
					}
					this.findComponentBlock(remoteComponentId, 1 + value) //
							.whenComplete((startAddress, e1) -> {
								if (e1 != null) {
									this._setMappingRemoteProtocolFault(true);
									e1.printStackTrace();
									return;
								}

								// Found Component Block -> read each nature block
								this.readNatureBlocks(startAddress).whenComplete((ignore, e2) -> {
									if (e2 != null) {
										this._setMappingRemoteProtocolFault(true);
										// TODO restart with timeout finding the component block on exception
										e2.printStackTrace();
										return;
									}

									this._setMappingRemoteProtocolFault(false);
									this.logInfo(this.log, "Finished reading remote Modbus/TCP protocol");
								});
							});
				});

			} catch (OpenemsException e) {
				this._setMappingRemoteProtocolFault(true);
			}
		});
		return false;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Validates that this device is a OpenEMS.
	 *
	 * <p>
	 * Tests if first register is 0x6201 ("OpenEMS").
	 *
	 * @return a future true if it is OpenEMS; otherwise false
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
	 * @param componentId  the remote Component-ID
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
		var modbusSlaveNatureTables = this.modbusSlaveNatureTableMethods.stream() //
				.map(method -> method.apply(this.remoteAccessMode)) //
				.collect(Collectors.toUnmodifiableList());

		var readElements = new ArrayDeque<ModbusElement>();
		var writeElements = new ArrayDeque<ModbusElement>();
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
						var gap = address - lastElement.startAddress - lastElement.length;
						if (gap > 0) {
							readElements.add(new DummyRegisterElement(//
									lastElement.startAddress + lastElement.length,
									lastElement.startAddress + lastElement.length + gap - 1));
						}
					}

					if (record instanceof ModbusRecordChannel r) {
						m(r.getChannelId(), element);

					} else {
						var onUpdateCallback = this.getOnUpdateCallback(modbusSlaveNatureTable, record);
						if (onUpdateCallback != null) {
							// This is guaranteed to work because of sealed abstract classes
							((AbstractModbusElement<?, ?, ?>) m(element).build())
									.onUpdateCallback(value -> onUpdateCallback.accept(value));
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
			var taskElements = new ArrayDeque<ModbusElement>();
			var element = readElements.pollFirst();
			while (element != null) {
				if (length + element.length > 126 /* limit of j2mod */) {
					this.addReadTask(taskElements);
					length = 0;
					taskElements.clear();
				}
				taskElements.add(element);
				length += element.length;
				element = readElements.pollFirst();
			}
			this.addReadTask(taskElements);
		}

		/*
		 * Add the Write-Task(s)
		 */
		{
			var taskElements = new ArrayDeque<ModbusElement>();
			var element = writeElements.pollFirst();
			while (element != null) {
				var lastElement = taskElements.peekLast();
				if (lastElement != null && (lastElement.startAddress + lastElement.length < element.startAddress)) {
					// Found gap
					this.addWriteTask(taskElements);
					taskElements.clear();
				}
				taskElements.add(element);
				element = writeElements.pollFirst();
			}
			this.addWriteTask(taskElements);
		}
	}

	/**
	 * Provide a Consumer for Registers that cannot be automatically mapped to
	 * Channels.
	 * 
	 * @param modbusSlaveNatureTable the {@link ModbusSlaveNatureTable} of the
	 *                               Register
	 * @param record                 the {@link ModbusRecord}
	 * @return a Consumer that receives the value from Modbus
	 */
	protected abstract Consumer<Object> getOnUpdateCallback(ModbusSlaveNatureTable modbusSlaveNatureTable,
			ModbusRecord record);

	/**
	 * Provide a local ChannelId that gets mapped to remote READ_WRITE or WRITE_ONLY
	 * registers.
	 * 
	 * @param modbusSlaveNatureTable the {@link ModbusSlaveNatureTable} of the
	 *                               Register
	 * @param record                 the {@link ModbusRecord}
	 * @return a local ChannelId
	 */
	protected abstract io.openems.edge.common.channel.ChannelId getWriteChannelId(
			ModbusSlaveNatureTable modbusSlaveNatureTable, ModbusRecord record);

	/**
	 * Create ModbusElement from type and address.
	 * 
	 * @param type    the {@link ModbusType}
	 * @param address the address of the {@link AbstractModbusElement}
	 * @return the {@link AbstractModbusElement}
	 */
	private static ModbusElement generateModbusElement(ModbusType type, int address) {
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
	 * @param elements the {@link AbstractModbusElement}s
	 * @throws OpenemsException on error
	 */
	private void addReadTask(Deque<ModbusElement> elements) throws OpenemsException {
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
						elements.peekFirst().startAddress, Priority.HIGH,
						elements.toArray(new ModbusElement[elements.size()])));
	}

	/**
	 * Adds a Write-Task with ModbusElements.
	 * 
	 * @param elements the {@link AbstractModbusElement}s
	 * @throws OpenemsException on error
	 */
	private void addWriteTask(Deque<ModbusElement> elements) throws OpenemsException {
		if (elements.isEmpty()) {
			return;
		}
		this.modbusProtocol.addTask(//
				new FC16WriteRegistersTask(//
						elements.peekFirst().startAddress, elements.toArray(new ModbusElement[elements.size()])));
	}

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
										// TODO get Remote-Nature name from this.modbusSlaveNatureTableMethods
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
}
