package io.openems.edge.solaredge.ess.charger;

import java.util.function.Consumer;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.solaredge.ess.SolarEdgeEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "SolarEdge.ESS.Charger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class SolarEdgeChargerImpl extends AbstractOpenemsComponent
		implements SolarEdgeCharger, EssDcCharger, OpenemsComponent, EventHandler, TimedataProvider, ModbusSlave {

	private final CalculateEnergyFromPower calculateActualEnergy = new CalculateEnergyFromPower(this,
			EssDcCharger.ChannelId.ACTUAL_ENERGY);	
	
	private SolarEdgeListener powerListener;
	private SolarEdgeListener voltageListener;
	private SolarEdgeListener chargeCurrentListener;
	private SolarEdgeListener chargePowerListener;
		
	@Reference
	private ConfigurationAdmin cm;
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SolarEdgeEss essInverter;	
	
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;	
	
	public SolarEdgeChargerImpl() throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(),
				SolarEdgeCharger.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		
		this.powerListener = new SolarEdgeListener(this, this.essInverter, SymmetricEss.ChannelId.ACTIVE_POWER,
				SolarEdgeCharger.ChannelId.INVERTER_POWER);		
		this.voltageListener = new SolarEdgeListener(this, this.essInverter, SolarEdgeEss.ChannelId.VOLTAGE_DC,
				EssDcCharger.ChannelId.VOLTAGE);	
		this.chargeCurrentListener = new SolarEdgeListener(this, this.essInverter, SolarEdgeEss.ChannelId.BATTERY1_ACTUAL_CURRENT,
				SolarEdgeCharger.ChannelId.CHARGE_CURRENT);
		this.chargePowerListener = new SolarEdgeListener(this, this.essInverter, SolarEdgeEss.ChannelId.BATTERY1_ACTUAL_POWER,
				SolarEdgeCharger.ChannelId.CHARGE_POWER);
		
		this.essInverter.addCharger(this);
		
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "essInverter",
				config.essInverter_id())) {
			return;
		}	
	}
	
	@Override
	@Deactivate
	protected void deactivate() {
		this.essInverter.removeCharger(this);
		this.powerListener.deactivate();
		this.voltageListener.deactivate();
		this.chargeCurrentListener.deactivate();
		this.chargePowerListener.deactivate();
		super.deactivate();
	}
	
	
	@Override
	public String debugLog() {
		/*
		return "DC Voltage:"+this.getVoltage()+"|Battery Charge Current:"+this.getChargeCurrent()
				+ "|Inverter Power:"+this.getInverterPower()+"|Battery Charge Power:"+this.getChargePower()+"|Actual Power (PV Generator):" + this.getActualPower().asString();
		*/
		
		return "Production:" + this.getActualPower().asString();
	}	

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}	
		
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.calculateAndSetActualPower();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			break;
		}
	}
		
	/**
	 * Calculates Actual PV Power out of total DC power (PV + battery charge/discharge) - (positive while Charging).
	 */
	public void calculateAndSetActualPower() {
		try {
			// calculate actual PV Power
			int pvProduction = this.getInverterPower().getOrError() + this.getChargePower().getOrError();		
			pvProduction = ignoreImpossibleMinPower(pvProduction, this.getChargePower().getOrError());
			
			if (pvProduction <= 0) {
				this._setActualPower(0);
			} else {
				this._setActualPower(pvProduction);
			}			
		} catch (Exception e) {
			return;
		}		
	}
	
	/**
	 * Ignore impossible minimum power.
	 * 
	 * <p>
	 * Even if there is no real power from PV, the Inverter Power Channel
	 * could remain on minimum power values. These values are ignored.
	 * 
	 * @param pvProduction	SolarEdge PV Production in W (Inverter Power + ChargePower)
	 * @param chargerPower	Battery charge power in W
	 * @return possible PV Production power
	 */
	protected static Integer ignoreImpossibleMinPower(Integer pvProduction, Integer chargePower) {
		if (pvProduction == null || chargePower == null) {
			return pvProduction;
		}

		if (chargePower==0) {
			return Math.abs(pvProduction) < 50 /* W */ ? 0 : pvProduction;
		}

		return pvProduction;
	}	
		
	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		var actualPower = this.getActualPower().get();
		if (actualPower == null) {
			// Not available
			this.calculateActualEnergy.update(null);
		} else if (actualPower > 0) {
			this.calculateActualEnergy.update(actualPower);
		} else {
			this.calculateActualEnergy.update(0);
		}
	}
	
	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				EssDcCharger.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(SolarEdgeChargerImpl.class, accessMode, 100) //
						.build());
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
	
	private static class SolarEdgeListener implements Consumer<Value<Integer>> {

		private final IntegerReadChannel solarEdgeChannel;
		private final IntegerReadChannel mirrorChannel;

		public SolarEdgeListener(SolarEdgeChargerImpl parent, SolarEdgeEss essInverter,
				io.openems.edge.common.channel.ChannelId solarEdgeChannel, io.openems.edge.common.channel.ChannelId mirrorChannel) {
			this.solarEdgeChannel = essInverter.channel(solarEdgeChannel);
			this.mirrorChannel = parent.channel(mirrorChannel);
			this.solarEdgeChannel.onSetNextValue(this);
		}

		public void deactivate() {
			this.solarEdgeChannel.removeOnSetNextValueCallback(this);
		}

		@Override
		public void accept(Value<Integer> t) {
			this.mirrorChannel.setNextValue(t);
		}
	}	

}
