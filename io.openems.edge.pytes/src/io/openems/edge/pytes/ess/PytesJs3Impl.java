package io.openems.edge.pytes.ess;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.pytes.dccharger.PytesDcChargerImpl;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Pytes.Hybrid.ESS", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class PytesJs3Impl extends AbstractOpenemsComponent implements PytesJs3, OpenemsComponent, EventHandler {

    @Reference
    private ConfigurationAdmin cm;
    
	private Config config = null;

	private PytesDcCharger charger;
	
	public PytesJs3Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				PytesJs3.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}


	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			// TODO: fill channels
			break;
		}
	}

	@Override
	public String debugLog() {
		return "Hello World";
	}

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public int getUnitId() {
        return this.config.modbusUnitId();
    }

    @Override
    public String getModbusBridgeId() {
        return this.config.modbus_id();
    }

    @Override
    public void addCharger(PytesDcCharger charger) {
        this.charger = charger;
    }

    @Override
    public void removeCharger(PytesDcCharger charger) {
        if (this.charger == charger) {
            this.charger = null;
        }
    }
	
	
}
