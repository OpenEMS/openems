package io.openems.edge.raspberrypi.sensor;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Designate( ocd= Config.class, factory=true)
@Component(name="Sensor", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Sensor extends AbstractOpenemsComponent implements OpenemsComponent {
private String SensorID;
//just temporary for future Implementations, will be more concrete
private List<Integer> ChipID=new ArrayList<>();
private List<Enum> BoardID=new ArrayList<>();
private List<Enum> ADCTypes=new ArrayList<>();
private List<Integer> PinUsage=new ArrayList<>();
//
	@Reference
	protected ConfigurationAdmin cm;

	public Sensor (io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
									io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}


	@Activate
	//TODO Config inputs to Arraylist
	//TODO foreach loop adding to adclist
	//TODO Create New Adc if not in adclist
	//TODO Set PinUse with SensorID
	//TODO Create SensorTypeList --> for deactivation
	void activate(Config config) {
		this.SensorID = config.SensorID();
	}

	//TODO foreach loop remove out of adclist
	//TODO PinUse false
	@Deactivate
	protected void deactivate() {
		//TODO Create SensorTyp Liste
		//TODO Deactivate SensorTypes with this SensorID
		//TODO Deachtivate this
		super.deactivate();
	}
	@Override
	public String debugLog(){return "This will be a List for All Chip and Pin Usage, maybe with Sensor Types from List";}
}
