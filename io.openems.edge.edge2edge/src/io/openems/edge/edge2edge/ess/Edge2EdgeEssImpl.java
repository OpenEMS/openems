package io.openems.edge.edge2edge.ess;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
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

	private final Logger log = LoggerFactory.getLogger(Edge2EdgeEssImpl.class);

	// TODO this should be read dynamically using the RemoteComponentId-config
	// property
	private final ModbusProtocol modbusProtocol;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected Power power;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

//	private Config config = null;

	public Edge2EdgeEssImpl() throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
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
//
			} catch (OpenemsException e) {
//				this.logWarn(this.log, "Error while reading SunSpec identifier block: " + e.getMessage());
				e.printStackTrace();
//				this.isSunSpecInitializationCompleted = true;
//				this.onSunSpecInitializationCompleted();
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
							// Found Component Block
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
					.thenAccept(lengthOfBlock -> {
						var lastAddress = startAddress + 20 + lengthOfBlock; // TODO overlap!
						this._readNatureBlock(result, startAddress + 20, lastAddress);
					});
		} catch (OpenemsException e) {
			result.completeExceptionally(e);
		}
		return result;
	}

	private void _readNatureBlock(CompletableFuture<Void> result, int startAddress, int lastAddress) {

		try {
			ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(startAddress), false)
					.thenAccept(hash -> {
						this._readNatureBlock(hash);

						try {
							this._findManagedSymmetric(startAddress).thenAccept(value -> {
								if (value) {
									try {
										this.defineModbusProtocol().addTask(//
												new FC16WriteRegistersTask(startAddress + 6, //
														m(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS,
																new FloatDoublewordElement(startAddress + 6))) //
										);
									} catch (OpenemsException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							});

							ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(startAddress + 1),
									false).thenAccept(lengthOfBlock -> {
										var nextStartAddress = startAddress + lengthOfBlock;
										if (nextStartAddress == lastAddress) {
											result.complete(null);
										} else {
											this._readNatureBlock(result, nextStartAddress, lastAddress);
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

	private CompletableFuture<Boolean> _findManagedSymmetric(int startAddress) throws OpenemsException {
		final var result = new CompletableFuture<Boolean>();
		ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedWordElement(startAddress), true)
				.thenAccept(value -> {
					result.complete(isHashEqual(value, "ManagedSymmetricEss"));
				});
		return result;
	}

	private void _readNatureBlock(Integer hash) {
		System.out.println("_readNatureBlock. " + hash);
	}

//
//
//	@Override
//	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
//		return new ModbusProtocol(this, //
//		/*
//		 * Block 'OpenemsComponent'
//		 */
////				new FC3ReadRegistersTask(BASE_ADDRESS + 22, Priority.LOW, //
////						m(new UnsignedWordElement(BASE_ADDRESS + 22)).build().onUpdateCallback(value -> {
////							if (value == null) {
////								return;
////							}
////							this.channel(Edge2EdgeEss.ChannelId.REMOTE_FAULT)
////									.setNextValue(value == Level.FAULT.getValue());
////							this.channel(Edge2EdgeEss.ChannelId.REMOTE_WARNING)
////									.setNextValue(value == Level.WARNING.getValue());
////							this.channel(Edge2EdgeEss.ChannelId.REMOTE_INFO)
////									.setNextValue(value == Level.INFO.getValue());
////						})), //
//				/*
//				 * Block 'SymmetricEss'
//				 */
//				new FC3ReadRegistersTask(BASE_ADDRESS + 102, Priority.LOW, //
//						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(BASE_ADDRESS + 102)), //
//						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(BASE_ADDRESS + 103)), //
//						m(SymmetricEss.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(BASE_ADDRESS + 104)), //
//						m(SymmetricEss.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(BASE_ADDRESS + 106)) //
//				), //
//				/*
//				 * Block 'ManagedSymmetricEss'
//				 */
//				new FC3ReadRegistersTask(BASE_ADDRESS + 202, Priority.LOW, //
//						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER,
//								new FloatDoublewordElement(BASE_ADDRESS + 202)), //
//						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER,
//								new FloatDoublewordElement(BASE_ADDRESS + 204)) //
//				), //
//				new FC16WriteRegistersTask(BASE_ADDRESS + 206, //
//						m(Edge2EdgeEss.ChannelId.SET_ACTIVE_POWER, new FloatDoublewordElement(BASE_ADDRESS + 206)), //
//						m(Edge2EdgeEss.ChannelId.SET_REACTIVE_POWER, new FloatDoublewordElement(BASE_ADDRESS + 208)) //
//				) //
//		);
//	}

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
		IntegerWriteChannel setActivePowerChannel = this.channel(Edge2EdgeEss.ChannelId.SET_ACTIVE_POWER);
		setActivePowerChannel.setNextWriteValue(activePower);
		IntegerWriteChannel setReactivePowerChannel = this.channel(Edge2EdgeEss.ChannelId.SET_REACTIVE_POWER);
		setReactivePowerChannel.setNextWriteValue(reactivePower);
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
			this.setActivePowerEquals(1000);
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
