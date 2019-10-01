package io.openems.edge.raspberrypi.sensor;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.sensor.api.Adc.Adc;
import io.openems.edge.raspberrypi.sensor.api.Adc.Pins.Pin;
import io.openems.edge.raspberrypi.sensor.api.Board;
import io.openems.edge.raspberrypi.spi.SpiInitial;
import io.openems.edge.raspberrypi.spi.SpiInitialImpl;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Designate( ocd= Config.class, factory=true)
@Component(name="Sensor", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public abstract class Sensor extends AbstractOpenemsComponent implements OpenemsComponent {
    @Reference
    protected SpiInitial spiInitial;
    @Reference
    protected ConfigurationAdmin cm;

    private String sensorId;
//just temporary for future Implementations, will be more concrete
private List<Integer> adcId=new ArrayList<>();
private List<Enum> boardId=new ArrayList<>();
private List<String> adcType=new ArrayList<>();
/*
String className = "Class1";
Object xyz = Class.forName(className).newInstance();
-->Needed with adcType

 */
private Map<Integer, Pin> pinUsage = new HashMap<>();

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
        this.sensorId = config.sensorId();
	    //arrange Config --> add Items correctly
	    arrangeConfig(config);

        //6.Map the Pins to ChipId
        for (Integer adc: this.adcId
        ) {
            arrangePinToChipMapping();
        }

        //Create new Adc if Id not found
        //If Adc exists --> Check if Pins are free
        arrangeAdcAndPinInitialization(config.pinUsage());


        //Check if ADC is in List --> Continue to Check if Pins are already used
        //ADC not in List --> Create new




    }

    private void arrangeAdcAndPinInitialization() {
        // 7. Check if Adc in Adc List (foreach Adc == valueof adcList.getId)
        for (Integer adc: this.adcId
        ) {
            if(!checkIfAdcExists(adc)){
                //Create New Adc with components: forName --> MCP3208 for example,

                try {
                    Class.forName(adcType.get(adc)).getConstructor(Enum.class, Integer.class).newInstance(this.boardId.get(this.adcId.indexOf(adc)), adc);
                } catch (ClassNotFoundException | NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                //set Used for every Pin

            }else{
                //Placeholder
                checkPinUse(adc, 0 );
            }
        }
    }


    private void arrangeConfig(Config config){
    String arranger;
    //1. Get adcIds + write to adcIdList


    // 3. Get Board ID's

    //4. Get them in Pieces --> Add to BoardList
    //5. Get MCP Types --> AdcTypes
    //
  arrangeConfigToList("adcId", config.adcId());
  arrangeConfigToList("boardId", config.boardId());
  arrangeConfigToList("adcType", config.adcType());


}


    private void arrangeConfigToList(String identifier, String identifierConfig ) {

	    //TODO make it more beautiful...no prio atm
        String arranger = identifierConfig;

	    switch(identifier){

            case "adcId":
                if(arranger.contains(";")) {
                    for (String adcID : arranger.split(";")) {
                        adcId.add(Integer.parseInt(adcID));
                        System.out.println("added " + adcID + "to List");
                    }
                }
                else{
                    adcId.add(Integer.parseInt(arranger));
                }
                break;

            case "boardId":
                if(arranger.contains(";")) {
                    for (String boardId : arranger.split(";")) {
                        this.boardId.add(Board.valueOf(boardId));
                        System.out.println("added " + boardId + "to List");
                    }
                }
                else{
                    this.boardId.add(Board.valueOf(arranger));
                }
                break;
            case "adcType":

                if(arranger.contains(";")){

                    for(String adcType : arranger.split(";")){
                        this.adcType.add(adcType);
                    }
                }else{this.adcType.add(arranger);}
                break;


        }

    }
    private boolean checkIfAdcExists(int adc){

        for (Adc checkifExist: spiInitial.getAdcList()
             ) {
           return checkifExist.getId()==adc;
        }

	    return false;
    }
private boolean checkPinUse(int pin, int id){return false;}

public void arrangePinToChipMapping(String pinUsages){
	    int AdcListPosition = 0;
	    //More than one PinGrouping
	    if(pinUsages.contains(";")) {
            for (String pinCollection : pinUsages.split(";")
            ) {
                //Function to Split Pin even further

            }

        }else{
	        //Function to Split Pin even further (same function as above
        }
	    //Read Config String
    //Split String for each ; ---> Each Substring of ; is part of one chipId
    //Split String for each , --> Values for the mapping of ChipId

	    
};

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
