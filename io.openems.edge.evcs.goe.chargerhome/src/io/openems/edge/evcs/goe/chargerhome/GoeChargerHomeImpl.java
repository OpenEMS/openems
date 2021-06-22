package io.openems.edge.evcs.goe.chargerhome;

import java.net.UnknownHostException;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Evcs.Goe.ChargerHome", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class GoeChargerHomeImpl extends AbstractOpenemsComponent
		implements ManagedEvcs, Evcs, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(GoeChargerHomeImpl.class);
	private GoeAPI goeapi = null;
	
	protected Config config;
	
	@Reference
	private EvcsPower evcsPower;

	public boolean Active;
	public int activeCurrent;
	private int MinCurrent;
	private int MaxCurrent;
	private int lastEnergySession;

	/**
	 * Constructor.
	 */
	public GoeChargerHomeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				GoeChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.channel(GoeChannelId.ALIAS).setNextValue(config.alias());
		this.config = config;
		this.MinCurrent = config.minHwCurrent();
		this.MaxCurrent = config.maxHwCurrent();
		this._setChargingType(ChargingType.AC);
		this._setPowerPrecision(230);
		this._setMinimumHardwarePower(this.MinCurrent * 1 * 230);
		this._setMaximumHardwarePower(this.MaxCurrent * 3 * 230);

		// start api-Worker
		this.goeapi = new GoeAPI(config.ip(), false, "", config.StatusAfterCycles(), this);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		//this.readWorker.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:

			// handle writes
			JsonObject json = this.goeapi.getStatus();
			if (json != null) {

				String err = json.get("err").getAsString();
				JsonArray nrg = json.get("nrg").getAsJsonArray();
				int cabelCurrent = json.get("cbl").getAsInt()*1000;
				int phases = ConvertGoePhase(json.get("pha").getAsInt());				
				this.activeCurrent = json.get("amp").getAsInt()*1000;
				int alw = json.get("alw").getAsInt();
				if (alw==1) {
					this.Active = true;
				}
				else {
					this.Active = false;
				}
				
				this.channel(GoeChannelId.SERIAL).setNextValue(json.get("sse").getAsString());
				this.channel(GoeChannelId.FIRMWARE).setNextValue(json.get("fwv").getAsString());								
				this.channel(GoeChannelId.CURR_USER).setNextValue(this.activeCurrent);
				this.channel(GoeChannelId.STATUS_GOE).setNextValue(json.get("car").getAsInt());			
				this.channel(Evcs.ChannelId.STATUS).setNextValue(ConvertGoeStatus(json.get("car").getAsInt()));				
				this.channel(GoeChannelId.VOLTAGE_L1).setNextValue(nrg.get(0).getAsInt());
				this.channel(GoeChannelId.VOLTAGE_L2).setNextValue(nrg.get(1).getAsInt());
				this.channel(GoeChannelId.VOLTAGE_L3).setNextValue(nrg.get(2).getAsInt());
				this.channel(GoeChannelId.CURRENT_L1).setNextValue(nrg.get(4).getAsInt()*100);
				this.channel(GoeChannelId.CURRENT_L2).setNextValue(nrg.get(5).getAsInt()*100);
				this.channel(GoeChannelId.CURRENT_L3).setNextValue(nrg.get(6).getAsInt()*100);
				this.channel(GoeChannelId.ACTUAL_POWER).setNextValue(nrg.get(11).getAsInt()*10);
		
				this.channel(Evcs.ChannelId.PHASES).setNextValue(phases);				
				this.channel(Evcs.ChannelId.MAXIMUM_POWER).setNextValue(GetMaxPower(phases));	
				this.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER).setNextValue(GetMinHardwarePower(phases, cabelCurrent));	
				this.channel(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER).setNextValue(GetMaxHardwarePower(cabelCurrent));	
				this.channel(Evcs.ChannelId.CHARGE_POWER).setNextValue(nrg.get(11).getAsInt()*10);
				this.channel(Evcs.ChannelId.CHARGING_TYPE).setNextValue(ChargingType.AC);
				
				this.channel(GoeChannelId.ENERGY_TOTAL).setNextValue(json.get("eto").getAsInt()*100);				
				this.channel(Evcs.ChannelId.ENERGY_SESSION).setNextValue(json.get("dws").getAsInt()*10/3600);
			
				this.channel(GoeChannelId.ERROR).setNextValue(err);
				this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(false);
				
				this.setEnergySession();
				this.setPower();				
				
			}
			else{
				this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(true);
			}
			
			break;
		}
	}
	
	private int ConvertGoeStatus(int status) {
		switch(status) {
		case 1: // ready for charging, car unplugged
			return Status.NOT_READY_FOR_CHARGING.getValue();
		case 2: // charging
			return Status.CHARGING.getValue();
		case 3: // waiting for car
			return Status.READY_FOR_CHARGING.getValue(); 
		case 4: // charging finished, car plugged
			return Status.CHARGING_FINISHED.getValue();
		default:
			return Status.UNDEFINED.getValue();
		}
	}
	
	private int ConvertGoePhase(int phase) {
		int Phasen = (byte)phase & 0b00111000;			
		switch(Phasen) {
		case 8: // 0b00001000: Phase 1 is active
			return 1;
		case 24: // 0b00011000: Phase 1+2 is active
			return 2;
		case 56: // 0b00111000: Phase1-3 are active
			return 3;
		default:
			return 0;
		}
	}
	
	private int GetMaxHardwarePower(int cableCurrent) {
		int MaxPower = 0;
		int phases = 3;
		if (cableCurrent < this.MaxCurrent && cableCurrent > 0) {
			MaxPower = phases*cableCurrent*230/1000;
		}
		else {
			MaxPower = phases*this.MaxCurrent*230/1000;
		}
		return MaxPower;
	}
	
	private int GetMinHardwarePower(int phases, int cableCurrent) {
		int MinPower = 0;
		int MinPowerHw = phases*this.MinCurrent*230/1000;
		if (cableCurrent < this.MinCurrent && cableCurrent > 0) {
			MinPower = phases*cableCurrent*230/1000;
		}
		else {
			MinPower = phases*this.MinCurrent*230/1000;
		}
		if (MinPower > MinPowerHw || MinPower < 0) {
			return MinPowerHw;
		}
		else {
			return MinPower;
		}
	}
		
	private int GetMaxPower(int phases) {
		return phases*this.MaxCurrent*230/1000;
	}
	
	/**
	 * Sets the current from SET_CHARGE_POWER channel.
	 * 
	 * <p>
	 * Allowed loading current are between MinCurrent and MaxCurrent. Invalid values are
	 * discarded. The value is also depending on the DIP-switch settings and the
	 * used cable of the charging station.
	 */
	private void setPower() {
		
		WriteChannel<Integer> energyLimitChannel = this.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT);
		int energyLimit = energyLimitChannel.getNextValue().orElse(0);

		// Check energy limit
		if (energyLimit == 0 || energyLimit > this.getEnergySession().orElse(0)) {
			WriteChannel<Integer> channel = this.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT);
			Optional<Integer> valueOpt = channel.getNextWriteValueAndReset();
			if (valueOpt.isPresent()) {
	
				Integer power = valueOpt.get();
				Channel<Integer> minimumHardwarePowerChannel = this.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER);
				if (power < minimumHardwarePowerChannel.value()
						.orElse(0)) { /* charging under MINIMUM_HARDWARE_POWER isn't possible */
					power = 0;
					this.goeapi.setActive(false);
				}
				else {
					this.goeapi.setActive(true);
				}
				
				Value<Integer> phases = this.getPhases();
				Integer current = power * 1000 / phases.orElse(3) /* e.g. 3 phases */ / 230; /* voltage */
				// limits the charging value because go-e knows only values between MinCurrent and MaxCurrent
				if (current > this.MaxCurrent) {
					current = this.MaxCurrent;
				}
				if (current < this.MinCurrent) {
					current = this.MaxCurrent;
				}
				this.goeapi.setCurrent(current);
			}
		}
		else {
			this.goeapi.setActive(false);
			this.logInfoInDebugmode(this.log, "Maximum energy limit reached");
			this._setStatus(Status.ENERGY_LIMIT_REACHED);
		}
	}
	
	/**
	 * Sets the Energy Limit for this session from SET_ENERGY_SESSION channel.
	 * 
	 * <p>
	 * Allowed values for the command setenergy are 0; 1-65535 the value of the
	 * command is 0.1 Wh. The charging station will charge till this limit.
	 */
	private void setEnergySession() {

		WriteChannel<Integer> channel = this.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT);

		Optional<Integer> valueOpt = channel.getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {

			Integer energyTarget = valueOpt.get();

			if (energyTarget < 0) {
				return;
			}

			/*
			 * limits the target value because go-e knows only values between 0 and
			 * 65535 0.1Wh
			 */
			energyTarget /= 100;
			energyTarget = energyTarget > 65535 ? 65535 : energyTarget;
			energyTarget = energyTarget > 0 && energyTarget < 1 ? 1 : energyTarget;

			if (!energyTarget.equals(this.lastEnergySession)) {

				// Set energy limit
				this.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT).setNextValue(energyTarget*100);
				this.logInfoInDebugmode(this.log, "Setting go-e " + this.alias()
						+ " Energy Limit in this Session to [" + energyTarget / 10 + " kWh]");

				if (this.goeapi.setMaxEnergy(energyTarget)) {
					this.lastEnergySession = energyTarget;
				}
			}
		}
	}
	
	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public String debugLog() {
		if (this.config.debugMode()) {
			return "Limit:" + this.channel(GoeChannelId.CURR_USER).value().asString() + 					
					"|" + this.getStatus().getName() +
					"|Phases:" + this.getPhases() +
					"|Energylimit:" + this.lastEnergySession/10+"kWh" +
					"|max Hardwarepower:" + this.getMaximumHardwarePower() +
					"|min Hardwarepower:" + this.getMinimumHardwarePower();			
		}
		else {
			return "Limit:" + this.channel(GoeChannelId.CURR_USER).value().asString() + 
					"|" + this.getStatus().getName();
		}		
	}

	/**
	 * Logs are displayed if the debug mode is configured
	 * 
	 * @param log    Logger
	 * @param string Text to display
	 */
	protected void logInfoInDebugmode(Logger log, String string) {
		if (this.config.debugMode()) {
			this.logInfo(log, string);
		}
	}

}
