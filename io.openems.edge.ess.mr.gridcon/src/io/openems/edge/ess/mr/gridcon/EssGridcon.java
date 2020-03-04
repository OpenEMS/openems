package io.openems.edge.ess.mr.gridcon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.mr.gridcon.battery.SoltaroBattery;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId0;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId1;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public abstract class EssGridcon extends AbstractOpenemsComponent
		implements OpenemsComponent, ManagedSymmetricEss, SymmetricEss, ModbusSlave, EventHandler {

	String gridconId;
	String bmsAId;
	String bmsBId;
	String bmsCId;

	protected io.openems.edge.ess.mr.gridcon.StateObject stateObject = null;

	protected abstract ComponentManager getComponentManager();

	private final Logger log = LoggerFactory.getLogger(EssGridcon.class);

	public EssGridcon(io.openems.edge.common.channel.ChannelId[] otherChannelIds) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ErrorCodeChannelId0.values(), //
				ErrorCodeChannelId1.values(), //
				otherChannelIds //
		);
	}

	public void activate(ComponentContext context, String id, String alias, boolean enabled, String gridconId,
			String bmsA, String bmsB, String bmsC) throws OpenemsNamedException {
		
		super.activate(context, id, alias, enabled);
		
		this.gridconId = gridconId;
		this.bmsAId = bmsA;
		this.bmsBId = bmsB;
		this.bmsCId = bmsC;

		initializeStateController(gridconId, bmsA, bmsB, bmsC);
		stateObject = getFirstStateObjectUndefined();
		calculateMaxApparentPower();
	}

	protected abstract StateObject getFirstStateObjectUndefined();

	protected abstract void initializeStateController(String gridconPCS, String b1, String b2,
			String b3);

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			try {
				// prepare calculated Channels
				calculateGridMode();
				calculateAllowedPowerAndCapacity();
				calculateSoc();
				calculateActivePower();
				calculateMinCellVoltage();

				IState nextState = this.stateObject.getNextState();
				StateObject nextStateObject = StateController.getStateObject(nextState);
				
				System.out.println("  ----- CURRENT STATE:" + this.stateObject.getState().getName());
				System.out.println("  ----- NEXT STATE:" + nextStateObject.getState().getName());
				
				this.stateObject = nextStateObject;
				
				this.stateObject.act();
				this.writeChannelValues();
			} catch (IllegalArgumentException | OpenemsNamedException e) {
				logError(log, "Error: " + e.getMessage());
			}
			break;
		}
	}

	private void calculateMinCellVoltage() {

		float minCellVoltage = Float.MAX_VALUE;

		if (getBattery1() != null) {
			minCellVoltage = Math.min(minCellVoltage, getBattery1().getMinimalCellVoltage());
		}

		if (getBattery2() != null) {
			minCellVoltage = Math.min(minCellVoltage, getBattery2().getMinimalCellVoltage());
		}

		if (getBattery3() != null) {
			minCellVoltage = Math.min(minCellVoltage, getBattery3().getMinimalCellVoltage());
		}

		int minCellVoltageMilliVolt = (int) (minCellVoltage * 1000);

		getMinCellVoltage().setNextValue(minCellVoltageMilliVolt);

	}

	private void writeChannelValues() throws OpenemsNamedException {
		this.channel(io.openems.edge.ess.mr.gridcon.ongrid.ChannelId.STATE_MACHINE)
				.setNextValue(this.stateObject.getState());
	}

	private void calculateMaxApparentPower() {
		int maxPower = (int) getGridconPCS().getMaxApparentPower();
		getMaxApparentPower().setNextValue(maxPower);
	}

	protected void calculateActivePower() {
		float activePower = getGridconPCS().getActivePower();
		getActivePower().setNextValue(activePower);
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
//		if (getGridMode().value().get() != GridMode.ON_GRID.getValue() && !gridconPCS.isRunning()) {
		if (!getGridconPCS().isRunning()) {

			log.info("ccu state nicht run!!");

			return new Constraint[] {
					createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
					createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) };
		}
		return Power.NO_CONSTRAINTS;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		getGridconPCS().setPower(activePower, reactivePower);
	}

	@Override
	public int getPowerPrecision() {
		return GridconPCS.POWER_PRECISION_WATT;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(GridconPCSImpl.class, accessMode, 300) //
						.build());
	}

	/**
	 * Handles Battery data, i.e. setting allowed charge/discharge power.
	 */
	protected void calculateAllowedPowerAndCapacity() {
		int allowedCharge = 0;
		int allowedDischarge = 0;
		int capacity = 0;

		for (SoltaroBattery battery : getBatteries()) {
			allowedCharge += battery.getVoltageX() * battery.getMaxChargeCurrentX() * -1;
			allowedDischarge += battery.getVoltageX() * battery.getMaxDischargeCurrentX();
			capacity += battery.getCapacityX();
		}
		getAllowedCharge().setNextValue(allowedCharge);
		getAllowedDischarge().setNextValue(allowedDischarge);
		getCapacity().setNextValue(capacity);
	}

	protected abstract void calculateGridMode() throws IllegalArgumentException, OpenemsNamedException;

	/**
	 * Calculates the StateObject-of-charge of all Batteries; if all batteries are
	 * available. Otherwise sets UNDEFINED.
	 */
	protected void calculateSoc() {
		float sumTotalCapacity = 0;
		float sumCurrentCapacity = 0;
		for (SoltaroBattery b : getBatteries()) {
			Optional<Float> totalCapacityOpt = Optional.of(b.getCapacityX());
			Optional<Float> socOpt = Optional.of(b.getSoCX());
			if (!totalCapacityOpt.isPresent() || !socOpt.isPresent()) {
				// if at least one Battery has no valid value -> set UNDEFINED
				getSoc().setNextValue(null);
				return;
			}
			float totalCapacity = totalCapacityOpt.get();
			float soc = socOpt.get();
			sumTotalCapacity += totalCapacity;
			sumCurrentCapacity += totalCapacity * soc / 100.0;
		}
		int soc = Math.round(sumCurrentCapacity * 100 / sumTotalCapacity);
		getSoc().setNextValue(soc);
	}

	/**
	 * Gets all Batteries.
	 * 
	 * @return a collection of Batteries; guaranteed to be not-null.
	 */
	protected Collection<SoltaroBattery> getBatteries() {
		Collection<SoltaroBattery> batteries = new ArrayList<>();
		if (getBattery1() != null) {
			batteries.add(getBattery1());
		}

		if (getBattery2() != null) {
			batteries.add(getBattery2());
		}

		if (getBattery3() != null) {
			batteries.add(getBattery3());
		}
		return batteries;
	}
	
	GridconPCS getGridconPCS() {
		return getComponent(gridconId);
	}
	
	SoltaroBattery getBattery1() {
		return getComponent(bmsAId);
	}
	
	SoltaroBattery getBattery2() {
		return getComponent(bmsBId);
	}
	
	SoltaroBattery getBattery3() {
		return getComponent(bmsCId);
	}
	
	<T> T getComponent(String id) {
		T component = null;
		try {
			component = getComponentManager().getComponent(id);
		} catch (OpenemsNamedException e) {
			
		}
		return component;
	}
}
