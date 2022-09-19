package io.openems.edge.edge2edge.ess;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Edge2Edge.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
		})
public class Edge2EdgeEssImpl extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, Edge2EdgeEss, ModbusComponent, OpenemsComponent, EventHandler {

	private final ModbusProtocol modbusProtocol;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected Power power;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public Edge2EdgeEssImpl() throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Edge2EdgeEss.ChannelId.values() //
		);
		this.modbusProtocol = new ModbusProtocol(this);
		this._setMaxApparentPower(Integer.MAX_VALUE); // TODO read proper limits from Modbus
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
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
		return (value & 0xFFFF) == (text.hashCode() & 0xFFFF);
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
		final var resultVoid = new CompletableFuture<Void>();
		try {
			ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(startAddress + 16), false)
					.thenAccept(lengthOfBlock -> {
						var lastAddress = startAddress + 20 + lengthOfBlock;
						this._readNatureBlock(startAddress + 20, lastAddress).thenAccept(map -> {
							map.entrySet().forEach(entry -> {
								try {
									switch (entry.getKey()) {
									case "SymmetricEss":
										this.defineModbusProtocol()
												.addTasks(new FC3ReadRegistersTask(entry.getValue() + 2, Priority.HIGH, //
														m(SymmetricEss.ChannelId.SOC,
																new UnsignedWordElement(entry.getValue() + 2)),
														m(SymmetricEss.ChannelId.GRID_MODE,
																new UnsignedWordElement(entry.getValue() + 3)),
														m(SymmetricEss.ChannelId.ACTIVE_POWER,
																new FloatDoublewordElement(entry.getValue() + 4)),
														m(SymmetricEss.ChannelId.REACTIVE_POWER,
																new FloatDoublewordElement(entry.getValue() + 6))));
										break;
									case "ManagedSymmetricEss":
										this.defineModbusProtocol().addTasks(
												new FC3ReadRegistersTask(entry.getValue() + 2, Priority.HIGH, //
														m(Edge2EdgeEss.ChannelId.MINIMUM_POWER_SET_POINT,
																new FloatDoublewordElement(entry.getValue() + 2)),
														m(Edge2EdgeEss.ChannelId.MAXIMUM_POWER_SET_POINT,
																new FloatDoublewordElement(entry.getValue() + 4))), //
												new FC16WriteRegistersTask(entry.getValue() + 6, //
														m(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS,
																new FloatDoublewordElement(entry.getValue() + 6)), //
														m(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS,
																new FloatDoublewordElement(entry.getValue() + 8)), //
														m(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_LESS_OR_EQUALS,
																new FloatDoublewordElement(entry.getValue() + 10)), //
														m(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_LESS_OR_EQUALS,
																new FloatDoublewordElement(entry.getValue() + 12)), //
														m(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_GREATER_OR_EQUALS,
																new FloatDoublewordElement(entry.getValue() + 14)), //
														m(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_GREATER_OR_EQUALS,
																new FloatDoublewordElement(entry.getValue() + 16)) //
										));
										break;
									case "StartStoppable":
										this.defineModbusProtocol()
												.addTasks(new FC3ReadRegistersTask(entry.getValue() + 2, Priority.HIGH, //
														m(StartStoppable.ChannelId.START_STOP,
																new UnsignedWordElement(entry.getValue() + 2)) //
										));
										break;
									}
								} catch (OpenemsException e) {
									e.printStackTrace();
								}
							});
						});
					});
		} catch (OpenemsException e) {
			resultVoid.completeExceptionally(e);
		}
		return resultVoid;
	}

	private CompletableFuture<Integer> _findNature(int startAddress, String... mapValue) {
		final var result = new CompletableFuture<Integer>();
		try {
			ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(startAddress), false)
					.thenAccept(value -> {
						for (int i = 0; i < mapValue.length; i++) {
							if (isHashEqual(value, mapValue[i])) {
								result.complete(i);
								return;
							}
						}
						result.complete(-1);
					});
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
		return result;
	}

	private CompletableFuture<Map<String, Integer>> _readNatureBlock(int startAddress, int lastAddress) {
		return this._readNatureBlock(startAddress, lastAddress, new HashMap<>());
	}

	private CompletableFuture<Map<String, Integer>> _readNatureBlock(int startAddress, int lastAddress,
			final Map<String, Integer> map) {
		final var result = new CompletableFuture<Map<String, Integer>>();
		try {
			final var natures = new String[] { "SymmetricEss", "ManagedSymmetricEss", "StartStoppable" };
			ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(startAddress), false)
					.thenAccept(hash -> {
						try {
							ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(startAddress + 1),
									false).thenAccept(lengthOfBlock -> {
										var nextStartAddress = startAddress + lengthOfBlock;
										if (nextStartAddress == lastAddress) {
											result.complete(map);
										} else {
											this._findNature(nextStartAddress, natures).thenAccept((t) -> {
												if (t != -1) {
													map.put(natures[t], nextStartAddress);
													if (map.size() == natures.length) {
														result.complete(map);
														return;
													}
												}
												this._readNatureBlock(nextStartAddress, lastAddress, map)
														.whenComplete((t1, u) -> {
															result.complete(t1);
														});
											});
										}
									});
						} catch (OpenemsException e) {
							result.completeExceptionally(e);
						}
					});
		} catch (OpenemsException e) {
			result.completeExceptionally(e);
		}
		return result;
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
