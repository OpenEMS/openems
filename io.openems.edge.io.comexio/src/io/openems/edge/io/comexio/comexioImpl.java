package io.openems.edge.io.comexio;

import java.util.Objects;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.generic_http_worker.*;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO Generic HTTP Comexio Implementation", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class comexioImpl extends AbstractOpenemsComponent
		implements comexio, DigitalOutput, SymmetricMeter, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(comexioImpl.class);

	private final BooleanWriteChannel[] digitalOutputChannels;
	private generic_http_worker worker[] = new generic_http_worker[9];
	private MeterType meterType = null;
	Config config = null;
	float voltage = (float) 0;
	private String base_url = null;
	private String on_urls[] = new String[9];
	private String off_urls[] = new String[9];
	private String on_successful_return = "1";
	private String off_successful_retrun = "1";
	private String state_successful_return = "1";

	public comexioImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				comexio.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(comexio.ChannelId.RELAY_1), //
				this.channel(comexio.ChannelId.RELAY_2), //
				this.channel(comexio.ChannelId.RELAY_3), //
				this.channel(comexio.ChannelId.RELAY_4), //
				this.channel(comexio.ChannelId.RELAY_5), //
				this.channel(comexio.ChannelId.RELAY_6), //
				this.channel(comexio.ChannelId.RELAY_7), //
				this.channel(comexio.ChannelId.RELAY_8), //
				this.channel(comexio.ChannelId.RELAY_9), //
		};
	}
	void generate_worker() {
		if(this.config.username() != "" && this.config.password() != "") {
			this.base_url = "http://" + this.config.username() + ":" + this.config.password() + "@" + this.config.ip() + "/api/?ext=" + this.config.group_prefix() + "&action=";
		}else {
			this.base_url = "http://" + this.config.ip() + "/api/?ext=" + this.config.group_prefix() + "&action="; 
		}
		for(int i = 0; i < 9; i++) {
			this.on_urls[i] = this.base_url + "set&io=Q" + (i+1) + "&value=1"; //+1 da Comexio bei 1 zum zählen beginnt		
			this.off_urls[i] = this.base_url + "set&io=Q" + (i+1) + "&value=0"; // +1 da Comexio bei 1 zum zählen beginnt
			String state_url = this.base_url + "get&io=Q" + (i+1); // +1 da Comexio bei 1 zum zählen beginnt
			String meter_url = this.base_url + "get&io=QI" + (i+1); // +1 da Comexio bei 1 zum zählen beginnt
			int timeout = this.config.timeout();
			String tmp[] = {state_url,meter_url};
			this.worker[i] = new generic_http_worker(tmp,timeout);
		}
	}
	void destroy_worker() {
		for(int i=0; i<this.worker.length; i++) {
			generic_http_worker tmp_worker = this.worker[i];
			tmp_worker.deactivate();
		}
	}
	void activate_worker() {
		for(int i=0; i<this.worker.length; i++) {
			generic_http_worker tmp_worker = this.worker[i];
			tmp_worker.activate("Comexio Worker"+String.valueOf(i));
			tmp_worker.triggerNextRun();
		}
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.voltage = Float.parseFloat(this.config.voltage());
		this.generate_worker();
		this.activate_worker();
		this.meterType = config.type();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		//Needed to destroy the API-Workers
		this.destroy_worker();
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder();
		var i = 1;
		for (WriteChannel<Boolean> channel : this.digitalOutputChannels) {
			String valueText;
			var valueOpt = channel.value().asOptional();
			if (valueOpt.isPresent()) {
				valueText = valueOpt.get() ? "x" : "-";
			} else {
				valueText = "?";
			}
			b.append(i + valueText);

			// add space for all but the last
			if (++i <= this.digitalOutputChannels.length) {
				b.append(" ");
			}
		}
		return b.toString();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.eventBeforeProcessImage();
			break;

		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.eventExecuteWrite();
			break;
		}
	}

	/**
	 * Execute on Cycle Event "Before Process Image".
	 */
	private void eventBeforeProcessImage() {
		
		Boolean relayIson[] = new Boolean[9];
		float Current_Per_Relay[] = new float[9];
		Integer Power_Per_Relay[] = new Integer[9];
		Integer power = 0;
		Long energy = (long) 0; //Comexio stellt keine Energy-Daten bereit daher können diese auch nicht abgefragt werden.
		try {
			for(int i = 0; i < 9; i++) {
				generic_http_worker tmp_worker = this.worker[i];
				tmp_worker.triggerNextRun();
				String state = tmp_worker.get_last_by_id(0).replace("\n", "").replace("\\n", "").replace("\r", "").replace("\\r", "").trim(); //ID "0" ist der State-URL	
				if(state == "_undefined_") {
					this.logError(this.log, "Generic HTTP Worker Called on an Undefined Index 0");
					throw tmp_worker.get_last_error();
				}else if(state == "_no_value_") {
					this.logInfo(this.log, "Generic HTTP Worker has not recieved Data yet");
					relayIson[i] = null;
				}else if(state == "_com_error_") {
					this.logError(this.log, "Generic HTTP Worker gave back the State \"Com-Error\"");
					throw tmp_worker.get_last_error();
				}else if(state.contains(this.state_successful_return) && state.length() == this.state_successful_return.length()) {
					relayIson[i] = true; 
				}else {
					//Es ist kein Fehler und auch nicht der Erwartete Wert zurück gekommen daher wird false geschrieben
					relayIson[i] = false;
				}
				String non_escaped = tmp_worker.get_last_by_id(1); //ID "1" ist der Meter-URL
				if(non_escaped == "_undefined_") {
					this.logError(this.log, "Generic HTTP Worker Called on an Undefined Index 1");
					throw tmp_worker.get_last_error();
				}else if(non_escaped == "_no_value_") {
					this.logInfo(this.log, "Generic HTTP Worker has not recieved Data yet");
				}else if(non_escaped == "_com_error_") {
					this.logError(this.log, "Generic HTTP Worker gave back the State \"Com-Error\"");
					throw tmp_worker.get_last_error();
				}else {
					String escaped = non_escaped.replace("\n", "").replace("\\n", "").replace("\r", "").replace("\\r", "").trim();
					float tmp_current = Float.parseFloat(escaped);
					Current_Per_Relay[i] = tmp_current;
					float tmp_power = (tmp_current * this.voltage);
					
					
					Integer rounded_power = Math.round(tmp_power);
					
					Power_Per_Relay[i] = rounded_power;
					
					power = power + rounded_power;
				}
			}

		} catch (Exception e) {
			this.logError(this.log, "Unable to read from Device: " + e.getMessage());
			this._setSlaveCommunicationFailed(true);
		}
		
		//Die Array-Werte weichen um -1 ab zu dem _setRelay[x] channel. Comexio beginnt bei 1 zu zählen
		this._setRelay1(relayIson[0]);
		this._setRelay2(relayIson[1]);
		this._setRelay3(relayIson[2]);
		this._setRelay4(relayIson[3]);
		this._setRelay5(relayIson[4]);
		this._setRelay6(relayIson[5]);
		this._setRelay7(relayIson[6]);
		this._setRelay8(relayIson[7]);
		this._setRelay9(relayIson[8]);
		
		//Set Current
		this._setCurrent1(Current_Per_Relay[0]);
		this._setCurrent2(Current_Per_Relay[1]);
		this._setCurrent3(Current_Per_Relay[2]);
		this._setCurrent4(Current_Per_Relay[3]);
		this._setCurrent5(Current_Per_Relay[4]);
		this._setCurrent6(Current_Per_Relay[5]);
		this._setCurrent7(Current_Per_Relay[6]);
		this._setCurrent8(Current_Per_Relay[7]);
		this._setCurrent9(Current_Per_Relay[8]);

		//Set Current
		this._setPower1(Power_Per_Relay[0]);
		this._setPower2(Power_Per_Relay[1]);
		this._setPower3(Power_Per_Relay[2]);
		this._setPower4(Power_Per_Relay[3]);
		this._setPower5(Power_Per_Relay[4]);
		this._setPower6(Power_Per_Relay[5]);
		this._setPower7(Power_Per_Relay[6]);
		this._setPower8(Power_Per_Relay[7]);
		this._setPower9(Power_Per_Relay[8]);
		
		//Dieser Wert ist die Summe der einzelnen Relay-Werte
		this._setActivePower(power);
		//Hier wird immer (long) 0 geschrieben da Comexio keine Daten bereitstellt
		this._setActiveProductionEnergy(energy);
	}

	/**
	 * Execute on Cycle Event "Execute Write".
	 */
	private void eventExecuteWrite() {
		try {
			this.executeWrite(this.getRelay1Channel(), 0);
			this.executeWrite(this.getRelay2Channel(), 1);
			this.executeWrite(this.getRelay3Channel(), 2);
			this.executeWrite(this.getRelay4Channel(), 3);
			this.executeWrite(this.getRelay5Channel(), 4);
			this.executeWrite(this.getRelay6Channel(), 5);
			this.executeWrite(this.getRelay7Channel(), 6);
			this.executeWrite(this.getRelay8Channel(), 7);
			this.executeWrite(this.getRelay9Channel(), 8);

			this._setSlaveCommunicationFailed(false);
		} catch (OpenemsNamedException e) {
			this._setSlaveCommunicationFailed(true);
		}
	}

	private void executeWrite(BooleanWriteChannel channel, int index) throws OpenemsNamedException {
		var readValue = channel.value().get();
		var writeValue = channel.getNextWriteValueAndReset();
		if (!writeValue.isPresent()) {
			// no write value
			return;
		}
		if (Objects.equals(readValue, writeValue.get())) {
			// read value = write value
			return;
		}
		if(writeValue.get()) {
			this.worker[index].send_external_url(this.on_urls[index], this.on_successful_return);
		}else {
			this.worker[index].send_external_url(this.off_urls[index], this.off_successful_retrun);
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

}