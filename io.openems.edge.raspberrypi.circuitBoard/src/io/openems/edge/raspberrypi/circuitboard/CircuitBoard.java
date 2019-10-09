package io.openems.edge.raspberrypi.circuitboard;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;
import io.openems.edge.raspberrypi.circuitboard.api.boardtypes.TemperatureBoard;
import io.openems.edge.raspberrypi.sensors.Sensor;
import io.openems.edge.raspberrypi.spi.SpiInitial;
import jdk.nashorn.internal.ir.annotations.Reference;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Designate( ocd= Config.class, factory=true)
@Component(name="CircuitBoard",immediate=true,
		configurationPolicy = ConfigurationPolicy.REQUIRE)
//has the regression values --> give to Sensor
public class CircuitBoard extends AbstractOpenemsComponent implements OpenemsComponent {

	@Reference
	private SpiInitial spiInitial;

	private String circuitBoardId;
	private String type;
	private String versionId;

	//Get ids from SpiInitial / Maybe extra class?
	//Only needed
	List<Sensor> sensors= new ArrayList<>();
	private short maxCapacity;
	private List<Integer> mcpListViaId=new ArrayList<>();
	//each CircuitBoard has its own Enum BoardType
	public CircuitBoard(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Activate
	public void activate(Config config) throws ConfigurationException {
	this.circuitBoardId=config.boardId();
	this.versionId=config.versionNumber();
	this.type=config.boardType();
	//Does Everything
	instantiateCorrectBoard(config);
	//make function with return bool etc
	spiInitial.getCircuitBoards().add(this);
	//Open SpiChannel via Adc List
		for (Adc adcWantToActivate:spiInitial.getAdcList()
			 ) {
			int counter = 0;
			// if(adcWantToActivate.getId())
		}
		//Add to SpiManager
	}

	private void instantiateCorrectBoard(Config config) throws ConfigurationException{
		switch(config.boardType()){
			case "Temperature": createTemperatureBoard(config.versionNumber());
			break;
		}
	}

	private void createTemperatureBoard(String versionNumber) throws ConfigurationException {
		switch (versionNumber){
			case "1":
				this.maxCapacity = TemperatureBoard.TEMPERATURE_BOARD_V_1.getMaxSize();
				for (Adc mcpWantToCreate : TemperatureBoard.TEMPERATURE_BOARD_V_1.getMcpContainer()) {
					createMcp(mcpWantToCreate);
				}
		}
	}

	private void createMcp(Adc mcpWantToCreate) throws ConfigurationException {
		//1. get SpiChannel
		int spiChannelWantToBeUsed;
		int mcpId;
		if(spiInitial.getFreeSpiChannels().size()==0)
		{
			spiChannelWantToBeUsed=spiInitial.getSpiManager().size()+1;
		}
		else{
			spiChannelWantToBeUsed=spiInitial.getFreeSpiChannels().get(0);
			spiInitial.getFreeSpiChannels().remove(0);
			if(spiInitial.getFreeSpiChannels().size()>0) {
				Collections.sort(spiInitial.getFreeSpiChannels());
			}
		}
		//2. get Mcp Id
		mcpId = getMcpId();


		mcpWantToCreate.initialize(mcpId, spiChannelWantToBeUsed, this.circuitBoardId);
		spiInitial.addAdcList(mcpWantToCreate);
		this.mcpListViaId.add(mcpId);
		//TODO mcpId.openSpiChannel();
		spiInitial.getSpiManager().put(mcpWantToCreate.getId(), spiChannelWantToBeUsed);


	}

	private int getMcpId() {

		if(spiInitial.getFreeAdcIds().size()==0){
			return spiInitial.getAdcList().size()+1;
		}
		 int toReturn = spiInitial.getFreeAdcIds().get(0);
		spiInitial.getFreeAdcIds().remove(0);
		Collections.sort(spiInitial.getFreeAdcIds());
		return toReturn;
	}

	public short getMaxCapacity() {
		return maxCapacity;
	}



	@Deactivate
	public void deactivate() {
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public void addToSensors(Sensor sensor) {
		if (sensor.getCircuitBoardId().equals(this.type)) {
		this.getSensors().add(sensor);
		}
	}



	public String getCircuitBoardId() {
		return circuitBoardId;
	}

	public String getType() {
		return type;
	}

	public List<Integer> getMcpListViaId() {
		return mcpListViaId;
	}

	public String getVersionId() {
		return versionId;
	}
}
