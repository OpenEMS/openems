package io.openems.edge.ess.generic.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.BatteryInverterConstraint;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.generic.symmetric.ChannelManager;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

/**
 * Parent class for different implementations of Managed Energy Storage Systems,
 * consisting of a Battery-Inverter component and a Battery component.
 */
public abstract class AbstractGenericManagedEss<ESS extends SymmetricEss & CycleProvider, BATTERY extends Battery, BATTERY_INVERTER extends ManagedSymmetricBatteryInverter>
		extends AbstractOpenemsComponent implements GenericManagedEss, ManagedSymmetricEss, HybridEss, SymmetricEss,
		OpenemsComponent, EventHandler, StartStoppable, ModbusSlave {

	/**
	 * Helper wrapping class to handle everything related to Channels.
	 *
	 * @return the {@link ChannelManager}
	 */
	protected abstract AbstractChannelManager<ESS, BATTERY, BATTERY_INVERTER> getChannelManager();

	protected abstract ComponentManager getComponentManager();

	protected abstract BATTERY getBattery();

	protected abstract BATTERY_INVERTER getBatteryInverter();

	private StartStopConfig startStopConfig;

	protected AbstractGenericManagedEss(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() method!");
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled, ConfigurationAdmin cm,
			String batteryInverterId, String batteryId, StartStopConfig startStop) {
		super.activate(context, id, alias, enabled);
		this.startStopConfig = startStop;

		// update filter for 'BatteryInverter'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "batteryInverter", batteryInverterId)) {
			return;
		}

		// update filter for 'Battery'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "battery", batteryId)) {
			return;
		}

		this.getChannelManager().activate(this.getComponentManager(), this.getBattery(), this.getBatteryInverter());
	}

	@Override
	protected void deactivate() {
		this.getChannelManager().deactivate();
		super.deactivate();
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

	/**
	 * Handles the State-Machine.
	 */
	protected abstract void handleStateMachine();

	protected void genericDebugLog(StringBuilder sb) {
		// Get DC-PV-Power for Hybrid ESS
		Integer dcPvPower = null;
		var batteryInverter = this.getBatteryInverter();
		if (batteryInverter instanceof HybridManagedSymmetricBatteryInverter) {
			dcPvPower = ((HybridManagedSymmetricBatteryInverter) batteryInverter).getDcPvPower();
		}

		sb //
				.append("|SoC:").append(this.getSoc().asString()) //
				.append("|L:").append(this.getActivePower().asString());

		// For HybridEss show actual Battery charge power and PV production power
		if (dcPvPower != null) {
			HybridEss me = this;
			sb //
					.append("|Battery:").append(me.getDcDischargePower().asString()) //
					.append("|PV:").append(dcPvPower);
		}

		// Show max AC export/import active power:
		// minimum of MaxAllowedCharge/DischargePower and MaxApparentPower
		sb //
				.append("|Allowed:") //
				.append(TypeUtils.max(//
						this.getAllowedChargePower().get(), TypeUtils.multiply(this.getMaxApparentPower().get(), -1)))
				.append(";") //
				.append(TypeUtils.min(//
						this.getAllowedDischargePower().get(), this.getMaxApparentPower().get()));
	}

	/**
	 * Forwards the power request to the {@link SymmetricBatteryInverter}.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		this.getBatteryInverter().run(this.getBattery(), activePower, reactivePower);
	}

	/**
	 * Retrieves PowerPrecision from {@link SymmetricBatteryInverter}.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public int getPowerPrecision() {
		return this.getBatteryInverter().getPowerPrecision();
	}

	/**
	 * Retrieves StaticConstraints from {@link SymmetricBatteryInverter}.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public Constraint[] getStaticConstraints() throws OpenemsNamedException {

		List<Constraint> result = new ArrayList<>();

		// Get BatteryInverterConstraints
		var constraints = this.getBatteryInverter().getStaticConstraints();

		for (BatteryInverterConstraint c : constraints) {
			result.add(this.getPower().createSimpleConstraint(c.description, this, c.phase, c.pwr, c.relationship,
					c.value));
		}

		// If the GenericEss is not in State "STARTED" block ACTIVE and REACTIVE Power!
		if (!this.isStarted()) {
			result.add(this.createPowerConstraint("ActivePower Constraint ESS not Started", Phase.ALL, Pwr.ACTIVE,
					Relationship.EQUALS, 0));
			result.add(this.createPowerConstraint("ReactivePower Constraint ESS not Started", Phase.ALL, Pwr.REACTIVE,
					Relationship.EQUALS, 0));
		}
		return result.toArray(new Constraint[result.size()]);
	}

	@Override
	public Integer getSurplusPower() {
		var batteryInverter = this.getBatteryInverter();
		if (batteryInverter instanceof HybridManagedSymmetricBatteryInverter) {
			return ((HybridManagedSymmetricBatteryInverter) batteryInverter).getSurplusPower();
		}
		return null;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode) //
		);
	}

	protected final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

	@Override
	public StartStop getStartStopTarget() {
		return switch (this.startStopConfig) {
		case AUTO -> this.startStopTarget.get();
		case START -> StartStop.START;
		case STOP -> StartStop.STOP;
		};
	}
}
