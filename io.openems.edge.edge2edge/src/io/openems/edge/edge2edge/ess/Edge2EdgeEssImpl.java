package io.openems.edge.edge2edge.ess;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.collect.Lists;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusRecordChannel;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Edge2Edge.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { // TODO anderes Event-Format
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
		})
public class Edge2EdgeEssImpl extends AbstractOpenemsModbusComponent implements ManagedSymmetricEss, AsymmetricEss,
		SymmetricEss, Edge2EdgeEss, ModbusComponent, OpenemsComponent, EventHandler {

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
							// TODO Fehlermeldung
							result.completeExceptionally(
									new OpenemsException("Unable to find remote Component with ID " + componentId));
						}
						if (remoteComponentId.equals(componentId)) {
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

		Deque<AbstractModbusElement<?>> elements = new ArrayDeque<>();
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

			System.out.println(modbusSlaveNatureTable.getNatureName() + ": " + natureStartAddress);
			for (var record : modbusSlaveNatureTable.getModbusRecords()) {
				var address = natureStartAddress + 2 /* hash & length */ + record.getOffset();

				// Fill gaps with DummyModbusElements
				var lastElement = elements.peekLast();
				if (lastElement != null) {
					var gap = address - lastElement.getStartAddress() - lastElement.getLength();
					if (gap > 0) {
						elements.add(new DummyRegisterElement(//
								lastElement.getStartAddress() + lastElement.getLength(),
								lastElement.getStartAddress() + lastElement.getLength() + gap - 1));
					}
				}

				if (record instanceof ModbusRecordChannel) {
					// TODO handle non ModbusRecordChannels
					var r = (ModbusRecordChannel) record;
					System.out.println("  " + r.getOffset() + ": " + r.getChannelId());

					// Create ModbusElement from type and address
					AbstractModbusElement<?> element = null;
					switch (r.getType()) {
					case ENUM16:
					case UINT16:
						element = new UnsignedWordElement(address);
						break;
					case UINT32:
						element = new UnsignedDoublewordElement(address);
						break;
					case FLOAT32:
						element = new FloatDoublewordElement(address);
						break;
					case FLOAT64:
						element = new UnsignedQuadruplewordElement(address);
						break;
					case STRING16:
						element = new StringWordElement(address, 16);
						break;
					}
					if (element == null) {
						continue;
					}

					elements.add(element);
					m(r.getChannelId(), element);
				}
			}
		}

		/**
		 * Adds a Task with ModbusElements.
		 * 
		 * <ul>
		 * <li>Makes sure there is no DummyRegisterElement in beginning or end of the
		 * queue
		 * <li>Adds only if queue is not empty
		 * </ul>
		 */
		ThrowingConsumer<Deque<AbstractModbusElement<?>>, OpenemsException> addTask = (es) -> {
			if (es.isEmpty()) {
				return;
			}
			while (es.peekFirst() instanceof DummyRegisterElement) {
				es.removeFirst();
			}
			while (es.peekLast() instanceof DummyRegisterElement) {
				es.removeLast();
			}
			this.modbusProtocol.addTask(//
					new FC3ReadRegistersTask(//
							es.peekFirst().getStartAddress(), Priority.HIGH,
							es.toArray(new AbstractModbusElement[es.size()])));
		};

		// Add the Read-Task(s)
		var length = 0;
		var taskElements = new ArrayDeque<AbstractModbusElement<?>>();
		var element = elements.pollFirst();
		while (element != null) {
			if (length + element.getLength() > 126 /* limit of j2mod */) {
				addTask.accept(taskElements);
				taskElements.clear();
			}
			taskElements.add(element);
			length += element.getLength();
			element = elements.pollFirst();
		}
		addTask.accept(taskElements);

		// TODO add Write-Tasks

		System.out.println("X");
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
				+ "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		this.setActivePowerEquals(activePower);
		this.setReactivePowerEquals(reactivePower);
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.handleStateMachine();
			break;
		}
	}

	private void handleStateMachine() {
		try {
			// TODO distribute power for different scenarios
			this.setActivePowerEquals(500);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}
}
