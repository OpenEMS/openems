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
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.mr.gridcon.battery.SoltaroBattery;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId0;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId1;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

//@Designate(ocd = EssGridconConfig.class, factory = true)
//@Component(//
//		name = "ESS.Gridcon", //
//		immediate = true, //
//		configurationPolicy = ConfigurationPolicy.REQUIRE, //
//		property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
//		}) //
public abstract class EssGridcon extends AbstractOpenemsComponent
		implements OpenemsComponent, ManagedSymmetricEss, SymmetricEss, ModbusSlave, EventHandler {



	//------- Components for an ESS with gridcon and soltaro
	protected GridconPCS gridconPCS;
	protected SoltaroBattery batteryA;
	protected SoltaroBattery batteryB;
	protected SoltaroBattery batteryC;
	//-------------------------------------------- 
	
	protected boolean enableIPU1 = false;
	protected boolean enableIPU2 = false;
	protected boolean enableIPU3 = false;

	protected ParameterSet parameterSet;

//	StateMachine stateMachine;
	protected io.openems.edge.ess.mr.gridcon.State stateObject = null;
//	protected Map<String, Map<GridConChannelId, Float>> weightingMap = initializeMap();

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

//	@Activate
	public void activate(ComponentContext context, String id, String alias, boolean enabled, boolean enableIPU1,
			boolean enableIPU2, boolean enableIPU3, ParameterSet parameterSet, String gridcon, String bmsA, String bmsB,
			String bmsC) throws OpenemsNamedException {
		super.activate(context, id, alias, enabled);
		this.enableIPU1 = enableIPU1;
		this.enableIPU2 = enableIPU2;
		this.enableIPU3 = enableIPU3;
		this.parameterSet = parameterSet;

		calculateMaxApparentPower();
		gridconPCS = getComponentManager().getComponent(gridcon);

		try {
			batteryA = getComponentManager().getComponent(bmsA);
		} catch (OpenemsNamedException e) {
			// if battery is null, no battery is connected on string a
		}

		try {
			batteryB = getComponentManager().getComponent(bmsB);
		} catch (OpenemsNamedException e) {
			// if battery is null, no battery is connected on string b
		}

		try {
			batteryC = getComponentManager().getComponent(bmsC);
		} catch (OpenemsNamedException e) {
			// if battery is null, no battery is connected on string c
		}

	}



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

//				weightingMap = getWeightingMap();
//				stringControlMode = getStringControlMode();

				io.openems.edge.ess.mr.gridcon.IState nextState = this.stateObject.getNextState();
				this.stateObject = StateController.getStateObject(nextState);
				this.stateObject.act();
				this.writeChannelValues();

//				channel(ChannelId.STATE_CYCLE_ERROR).setNextValue(false);
			} catch (IllegalArgumentException | OpenemsNamedException e) {
//				channel(ChannelId.STATE_CYCLE_ERROR).setNextValue(true);
//				logError(log, "State-Cycle Error: " + e.getMessage());
			}
			break;
		}
	}
	

	private void calculateMinCellVoltage() {
		
		float minCellVoltage = Float.MAX_VALUE;
		
		if (batteryA != null) {
			minCellVoltage = Math.min(minCellVoltage, batteryA.getMinimalCellVoltage());
		}
		
		if (batteryB != null) {
			minCellVoltage = Math.min(minCellVoltage, batteryB.getMinimalCellVoltage());
		}
		
		if (batteryC != null) {
			minCellVoltage = Math.min(minCellVoltage, batteryC.getMinimalCellVoltage());
		}
		
		int minCellVoltageMilliVolt = (int) (minCellVoltage * 1000);
		
		getMinCellVoltage().setNextValue(minCellVoltageMilliVolt);
		
	}

	private void writeChannelValues() throws OpenemsNamedException {
		this.channel(io.openems.edge.ess.mr.gridcon.ongrid.ChannelId.STATE_MACHINE)
				.setNextValue(this.stateObject.getState());
	}

	private void calculateMaxApparentPower() {

		int maxPower = 0;
		if (enableIPU1) {
			maxPower = maxPower + GridconPCSImpl.MAX_POWER_PER_INVERTER;
		}
		if (enableIPU2) {
			maxPower = maxPower + GridconPCSImpl.MAX_POWER_PER_INVERTER;
		}
		if (enableIPU3) {
			maxPower = maxPower + GridconPCSImpl.MAX_POWER_PER_INVERTER;
		}

		getMaxApparentPower().setNextValue(maxPower);
	}

	protected void calculateActivePower() {
		// Calculate Total Active Power

		float activePowerIpu1 = gridconPCS.getActivePowerInverter1();
		float activePowerIpu2 = gridconPCS.getActivePowerInverter2();
		float activePowerIpu3 = gridconPCS.getActivePowerInverter3();
		float activePower = activePowerIpu1 + activePowerIpu2 + activePowerIpu3;
		getActivePower().setNextValue(activePower);
	}


	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
		if (getGridMode().value().get() != GridMode.ON_GRID.getValue() && !gridconPCS.isRunning()) {

			log.info("Gridmode nicht on grid oder ccu state nicht run!!");

			return new Constraint[] {
					createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
					createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) };
		}
		return Power.NO_CONSTRAINTS;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {

		gridconPCS.setPower(activePower, reactivePower);
		
	}

	@Override
	public int getPowerPrecision() {
		return 100; // TODO estimated value
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



	protected boolean isBatteryReady(SoltaroBattery battery) {
		if (battery == null) {
			return false;
		}
		return battery.isRunning();
	}

	 public void setWeightStringA(float weight) {
		 gridconPCS.setWeightStringA(weight);
	 }
	 
	 public void setWeightStringB(float weight) {
		 gridconPCS.setWeightStringB(weight);
	 }
	 
	 public void setWeightStringC(float weight) {
		 gridconPCS.setWeightStringC(weight);
	 }
	 
	 public void setStringControlMode(int weightingMode) {
		 gridconPCS.setStringControlMode(weightingMode);
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

		public boolean isAtLeastOneBatteryReady() {
			for (SoltaroBattery battery : getBatteries()) {
				if (battery.isRunning()) {
					return true;
				}
			}
			return false;
		}
		
	protected abstract void calculateGridMode() throws IllegalArgumentException, OpenemsNamedException;

	/**
	 * Calculates the State-of-charge of all Batteries; if all batteries are
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
		if (batteryA != null) {
			batteries.add(batteryA);
		}

		if (batteryB != null) {
			batteries.add(batteryB);
		}

		if (batteryC != null) {
			batteries.add(batteryC);
		}

		return batteries;
	}

	public void start() {
		gridconPCS.setEnableIPU1(enableIPU1);
		gridconPCS.setEnableIPU2(enableIPU2);
		gridconPCS.setEnableIPU3(enableIPU3);

		gridconPCS.setPlay(true);
		gridconPCS.setSyncApproval(true);
		gridconPCS.setBlackStartApproval(false);
		gridconPCS.setShortCircuitHAndling(true);
		gridconPCS.setModeSelection(Mode.CURRENT_CONTROL);
		gridconPCS.setParameterSet(parameterSet);
		gridconPCS.setU0(GridconPCSImpl.ON_GRID_VOLTAGE_FACTOR);
		gridconPCS.setF0(GridconPCSImpl.ON_GRID_FREQUENCY_FACTOR);

		gridconPCS.setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		gridconPCS.setQLimit(1f);

		float maxPower = GridconPCSImpl.MAX_POWER_PER_INVERTER;
		if (enableIPU1) {
			gridconPCS.setPMaxChargeIPU1(maxPower);
			gridconPCS.setPMaxDischargeIPU1(-maxPower);
		}
		if (enableIPU2) {
			gridconPCS.setPMaxChargeIPU2(maxPower);
			gridconPCS.setPMaxDischargeIPU2(-maxPower);
		}
		if (enableIPU3) {
			gridconPCS.setPMaxChargeIPU3(maxPower);
			gridconPCS.setPMaxDischargeIPU3(-maxPower);
		}

		// Enable DC DC
		gridconPCS.enableDCDC();

		gridconPCS.setDcLinkVoltage(GridconPCSImpl.DC_LINK_VOLTAGE_SETPOINT);
	}

	public void runSystem() {
		gridconPCS.setEnableIPU1(enableIPU1);
		gridconPCS.setEnableIPU2(enableIPU2);
		gridconPCS.setEnableIPU3(enableIPU3);

		gridconPCS.setPlay(false);
		gridconPCS.setSyncApproval(true);
		gridconPCS.setBlackStartApproval(false);
		gridconPCS.setShortCircuitHAndling(true);
		gridconPCS.setModeSelection(Mode.CURRENT_CONTROL);
		gridconPCS.setParameterSet(parameterSet);
		gridconPCS.setU0(GridconPCSImpl.ON_GRID_VOLTAGE_FACTOR);
		gridconPCS.setF0(GridconPCSImpl.ON_GRID_FREQUENCY_FACTOR);

		gridconPCS.setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		gridconPCS.setQLimit(1f);
		
		float maxPower = GridconPCSImpl.MAX_POWER_PER_INVERTER;
		if (enableIPU1) {
			gridconPCS.setPMaxChargeIPU1(maxPower);
			gridconPCS.setPMaxDischargeIPU1(-maxPower);
		}
		if (enableIPU2) {
			gridconPCS.setPMaxChargeIPU2(maxPower);
			gridconPCS.setPMaxDischargeIPU2(-maxPower);
		}
		if (enableIPU3) {
			gridconPCS.setPMaxChargeIPU3(maxPower);
			gridconPCS.setPMaxDischargeIPU3(-maxPower);
		}

		// Enable DC DC
		gridconPCS.enableDCDC();

		gridconPCS.setDcLinkVoltage(GridconPCSImpl.DC_LINK_VOLTAGE_SETPOINT);
				
	}

	public void stopSystem() {
		gridconPCS.stop();
	}

	public boolean isRunning() {
		return gridconPCS.isRunning();
	}

	public boolean isError() {
		return gridconPCS.isError();
	}

	public boolean isStopped() {		// 
		return gridconPCS.isStopped();
	}

	public Integer getErrorCount() {
		return gridconPCS.getErrorCount();
	}

	public int getCurrentErrorCode() {
		return gridconPCS.getErrorCode();
	}
	
	public void acknowledgeErrors() {
		gridconPCS.acknowledgeErrors();
	}

	public void setErrorCodeFeedBack(int errorCodeFeedback) {
		gridconPCS.setErrorCodeFeedback(errorCodeFeedback);
	}
}
