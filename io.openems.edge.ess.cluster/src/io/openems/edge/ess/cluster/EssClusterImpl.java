package io.openems.edge.ess.cluster;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
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
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class EssClusterImpl extends AbstractOpenemsComponent implements EssCluster, ManagedAsymmetricEss, AsymmetricEss,
		ManagedSymmetricEss, SymmetricEss, MetaEss, OpenemsComponent, ModbusSlave, EventHandler, StartStoppable {

	private final Logger log = LoggerFactory.getLogger(EssCluster.class);
	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);
	private final ChannelManager channelManager = new ChannelManager(this);
	private final List<SymmetricEss> esss = new CopyOnWriteArrayList<>();

	@Reference
	private Power power;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Ess.Cluster)))")

	protected synchronized void addEss(ManagedSymmetricEss ess) {
		this.esss.add(ess);
		this.channelManager.deactivate();
		this.channelManager.activate(this.esss);
	}

	protected synchronized void removeEss(ManagedSymmetricEss ess) {
		this.esss.remove(ess);
		this.channelManager.deactivate();
		this.channelManager.activate(this.esss);
	}

	private Config config;

	public EssClusterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				EssCluster.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Ess", config.ess_ids())) {
			return;
		}
		this.channelManager.activate(this.esss);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.channelManager.deactivate();
		super.deactivate();
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsException {
		throw new OpenemsException("EssClusterImpl.applyPower() should never be called.");
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsException {
		throw new OpenemsException("EssClusterImpl.applyPower() should never be called.");
	}

	@Override
	public int getPowerPrecision() {
		Integer result = null;
		for (SymmetricEss ess : this.esss) {
			if (ess instanceof ManagedSymmetricEss) {
				result = TypeUtils.min(result, ((ManagedSymmetricEss) ess).getPowerPrecision());
			}
		}
		return TypeUtils.orElse(result, 1);
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				AsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedAsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(EssClusterImpl.class, accessMode, 300) //
						.build());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.handleStartStop();
			break;
		}
	}

	/**
	 * Handles the Start/Stop target from {@link Config} or set via
	 * {@link #setStartStop(StartStop)}.
	 */
	private void handleStartStop() {
		StartStop target = StartStop.UNDEFINED;

		switch (this.config.startStop()) {
		case AUTO: {
			target = this.startStopTarget.get();
			break;
		}
		case START: {
			target = StartStop.START;
			break;
		}
		case STOP: {
			target = StartStop.STOP;
			break;
		}
		}

		if (target == StartStop.UNDEFINED) {
			this.logInfo(this.log, "Start-Stop-Target is Undefined");
			return;
		}

		for (SymmetricEss ess : this.esss) {
			if (ess instanceof StartStoppable) {
				try {
					((StartStoppable) ess).setStartStop(target);
				} catch (OpenemsNamedException e) {
					this.logError(this.log, e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public synchronized String[] getEssIds() {
		return this.config.ess_ids();
	}

	@Override
	public void setStartStop(StartStop value) {
		this.startStopTarget.set(value);
	}
}
