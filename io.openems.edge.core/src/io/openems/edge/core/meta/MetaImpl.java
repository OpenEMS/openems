package io.openems.edge.core.meta;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.OpenemsConstants;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusSlave;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = Meta.SINGLETON_SERVICE_PID, //
		immediate = true, //
		property = { //
				"enabled=true" //
		})
public class MetaImpl extends AbstractOpenemsComponent implements Meta, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	public MetaImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Meta.ChannelId.values() //
		);
		this.channel(Meta.ChannelId.VERSION).setNextValue(OpenemsConstants.VERSION.toString());
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, SINGLETON_COMPONENT_ID, Meta.SINGLETON_SERVICE_PID, true);

		this.applyConfig(config);

		if (OpenemsComponent.validateSingleton(this.cm, Meta.SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, SINGLETON_COMPONENT_ID, Meta.SINGLETON_SERVICE_PID, true);

		this.applyConfig(config);
		if (OpenemsComponent.validateSingleton(this.cm, Meta.SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void applyConfig(Config config) {
		this._setCurrency(config.currency());
	}
}
