package io.openems.edge.onewire.thermometer;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.TemperatureContainer;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.onewire.BridgeOnewire;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.thermometer.api.Thermometer;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "OneWire.Thermometer", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class OneWireThermometerImpl extends AbstractOpenemsComponent implements Thermometer, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(OneWireThermometerImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private BridgeOnewire bridge;

	private Config config;
	private TemperatureContainer _container = null;
	private byte[] state = null;

	public OneWireThermometerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Thermometer.ChannelId.values(), //
				OneWireThermometer.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		// update filter for 'bridge'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "bridge", config.bridge_id())) {
			return;
		}

		if (this.isEnabled()) {
			this.bridge.addTask(this.task);
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.bridge.removeTask(this.task);
		super.deactivate();
	}

	private final Consumer<DSPortAdapter> task = adapter -> {
		try {
			var container = this.getDeviceContainer(adapter);
			if (this.state != null) {
				container.doTemperatureConvert(this.state);
			}
			this.state = container.readDevice();
			var temp = container.getTemperature(this.state);

			this._setTemperature((int) (temp * 10 /* convert to decidegree */));
			this._setCommunicationFailed(false);

		} catch (OneWireException | OpenemsException e) {
			this.logError(this.log, e.getMessage());

			this._setTemperature(null);
			this._setCommunicationFailed(true);
		}
	};

	private TemperatureContainer getDeviceContainer(DSPortAdapter adapter) throws OpenemsException {
		if (this._container != null) {
			return this._container;
		}
		var owc = adapter.getDeviceContainer(this.config.address());
		if (!(owc instanceof OneWireContainer)) {
			throw new OpenemsException("This is not a OneWire Temperature Container");
		}
		var container = (TemperatureContainer) owc;
		this._container = container;
		return this._container;
	}

	@Override
	public String debugLog() {
		return this.getTemperature().asString();
	}

	/**
	 * Gets the Channel for {@link ThisChannelId#COMMUNICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public StateChannel getCommunicationFailedChannel() {
		return this.channel(OneWireThermometer.ChannelId.COMMUNICATION_FAILED);
	}

	/**
	 * Gets the Communication Failed Fault State. See
	 * {@link ThisChannelId#COMMUNICATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public Value<Boolean> getCommunicationFailed() {
		return this.getCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ThisChannelId#COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public void _setCommunicationFailed(boolean value) {
		this.getCommunicationFailedChannel().setNextValue(value);
	}

}
