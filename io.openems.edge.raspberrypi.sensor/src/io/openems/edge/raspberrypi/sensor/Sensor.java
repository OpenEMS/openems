package io.openems.edge.raspberrypi.sensor;

import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.sensor.api.Adc.Adc;
import io.openems.edge.raspberrypi.sensor.api.Adc.Pins.Pin;
import io.openems.edge.raspberrypi.sensor.api.Board;
<<<<<<< HEAD
import io.openems.edge.raspberrypi.sensor.sensortype.SensorType;
import io.openems.edge.raspberrypi.spi.SpiInitial;
=======
import io.openems.edge.raspberrypi.spi.SpiInitial;
import io.openems.edge.raspberrypi.spi.SpiInitialImpl;
>>>>>>> SPI
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.lang.reflect.InvocationTargetException;
<<<<<<< HEAD
import java.util.*;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Sensor", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public abstract class Sensor extends AbstractOpenemsComponent implements OpenemsComponent {
    @Reference
    private SpiInitial spiInitial;
=======
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Designate( ocd= Config.class, factory=true)
@Component(name="Sensor", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public abstract class Sensor extends AbstractOpenemsComponent implements OpenemsComponent {
    @Reference
    protected SpiInitial spiInitial;
>>>>>>> SPI
    @Reference
    protected ConfigurationAdmin cm;

    private String sensorId;
<<<<<<< HEAD

    private List<Integer> adcId = new ArrayList<>();
    private List<Enum> boardId = new ArrayList<>();
    private List<String> adcType = new ArrayList<>();
    //First Integer is ChipID, Second is PinPosition
    private Map<Integer, List<Integer>> pinUsage = new HashMap<>();
    private final SensorWorker worker = new SensorWorker();
    //TODO create ChannelIds
    public Sensor(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                  io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }


    public String getSensorId() {
        return sensorId;
    }

    public Map<Integer, List<Integer>> getPinUsage() {
        return pinUsage;
    }

    @Activate
    void activate(Config config) throws ConfigurationException {
        this.sensorId = config.sensorId();
        //arrange Config --> add to Lists
        arrangeConfig(config);
        //Map the Pins to AdcId
        arrangePinToAdcMapping(config.pinUsage());
        //See if you can use adc with Pins
        for (Integer adcWantTobeUsed : this.adcId
        ) {
            for (Adc alreadyExist : spiInitial.getAdcList()
            ) {
                if (alreadyExist.getId() == adcWantTobeUsed) {
                    arrangePinInitialization(adcWantTobeUsed);

                } else {
                    //check if enough config param. were given
                    if(!adcType.contains(adcType.get(adcId.indexOf(adcWantTobeUsed)))) {
                        throw new ConfigurationException("Not enough adcTypes given", "User didn't config properly");
                    }
                    if(!boardId.contains(boardId.get(adcId.indexOf(adcWantTobeUsed)))){
                        throw new ConfigurationException("Not enough Boards given", "User didn't config properly");
                    }
                    createNewMcp(adcType.get(adcId.indexOf(adcWantTobeUsed)), adcWantTobeUsed);
                    //Allocate Pininit
                    arrangePinInitialization(adcWantTobeUsed);
                }
            }

        }

        //Important for SensorTypes
        spiInitial.getSensorList().add(this);
        spiInitial.getSensorManager().put(this.id(), null);
        //TODO is worker actually needed?
        this.worker.activate(config.sensorId());

    }

    private void createNewMcp(String adcWantTobeUsed, int adc) throws ConfigurationException {
        int SpiChannelForNewMcp=allocateSpiChannel();

        try {
            Class.forName(adcWantTobeUsed).getConstructor(Enum.class, Integer.class, Integer.class).newInstance(this.boardId.get(this.adcId.indexOf(adc)), adc, SpiChannelForNewMcp);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new ConfigurationException("","Cannot create ADC with name: " + adcWantTobeUsed +
                    "Remember to write: Mcp followed by correct Number (Mcp3208");

        }

    }

    private int allocateSpiChannel() {
        if(spiInitial.getFreeSpiChannels().size()==0){
            return spiInitial.getSpiManager().size();
        }
        int returnValue = spiInitial.getFreeSpiChannels().get(0);
        spiInitial.getFreeSpiChannels().remove(0);
        if(spiInitial.getFreeSpiChannels().size()>0) {
            Collections.sort(spiInitial.getFreeSpiChannels());
        }
        return returnValue;
    }

    private void arrangePinInitialization(int existingAdc) throws ConfigurationException {
        //Get Correct Adc via adcId
        Adc adc = getCorrectAdc(existingAdc);


        //for each Entry in Adc PinList, Check if Pin in Map already used
        for (Pin pin : adc.getPins()
        ) {
            //Get correct PinPosition to compare
            Pin pinPosition = adc.getPins().get(pin.getPosition());

            if (pin.isUsed() && pin.getPosition()==pinPosition.getPosition()) {

                throw new ConfigurationException("The Pin at Position: " +
                                                    pin.getPosition() + "at ADC: " + adc +
                                                        " is already used.", "User typed wrong Pins");
            }

        }
        //Succesful? good now you can allocate all

        for (Pin pin: adc.getPins()
             ) {
            if (pinUsage.containsValue(pin)) {
                pin.setUsed(true);
                System.out.println("Pin " + pin + " succesfully allocated");
            }
        }

        //throw new PinAlreadyUsedException("You Used:"+existingAdc+Pinposition+"but it's already used by:")


        //Check if already used?throw PinAlreadyUsedException: SetUsed True
        //

    }


    private Adc getCorrectAdc(int existingAdc) {

        for (Adc adc : spiInitial.getAdcList()
        ) {
            if (adc.getId() == existingAdc) {
                return adc;
            }
        }
        //TODO NullpointerException...eventhough it shouldn't occur
        return null;
    }


    private void arrangeConfig(Config config) {

        arrangeConfigToList("adcId", config.adcId());
        arrangeConfigToList("boardId", config.boardId());
        arrangeConfigToList("adcType", config.adcType());


    }


    private void arrangeConfigToList(String identifier, String identifierConfig) {

        //TODO make it more beautiful...no prio atm
        String arranger = identifierConfig;

        switch (identifier) {

            case "adcId":
                if (arranger.contains(";")) {
=======
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
>>>>>>> SPI
                    for (String adcID : arranger.split(";")) {
                        adcId.add(Integer.parseInt(adcID));
                        System.out.println("added " + adcID + "to List");
                    }
<<<<<<< HEAD
                } else {
=======
                }
                else{
>>>>>>> SPI
                    adcId.add(Integer.parseInt(arranger));
                }
                break;

            case "boardId":
<<<<<<< HEAD
                if (arranger.contains(";")) {
=======
                if(arranger.contains(";")) {
>>>>>>> SPI
                    for (String boardId : arranger.split(";")) {
                        this.boardId.add(Board.valueOf(boardId));
                        System.out.println("added " + boardId + "to List");
                    }
<<<<<<< HEAD
                } else {
=======
                }
                else{
>>>>>>> SPI
                    this.boardId.add(Board.valueOf(arranger));
                }
                break;
            case "adcType":

<<<<<<< HEAD
                if (arranger.contains(";")) {

                    for (String adcType : arranger.split(";")) {
                        this.adcType.add(adcType);
                    }
                } else {
                    this.adcType.add(arranger);
                }
=======
                if(arranger.contains(";")){

                    for(String adcType : arranger.split(";")){
                        this.adcType.add(adcType);
                    }
                }else{this.adcType.add(arranger);}
>>>>>>> SPI
                break;


        }

    }
<<<<<<< HEAD



    private void arrangePinToAdcMapping(String pinUsages) {

        int adcListPosition = 0;
        //More than one PinGrouping
        if (pinUsages.contains(";")) {
            for (String pinCollection : pinUsages.split(";")
            ) {
                splitSubstringForAdcMapping(pinCollection, adcListPosition++);
            }
        } else {
            splitSubstringForAdcMapping(pinUsages, 0);
        }

    }

    private void splitSubstringForAdcMapping(String pinCollection, int adcListPosition) {
        List<Integer> pinsToPut = new ArrayList<>();
        char [] pinPosition = pinCollection.toCharArray();
        for (char actualPin : pinPosition) {

            pinsToPut.add(Character.getNumericValue(actualPin));
        }
        this.pinUsage.put(adcListPosition, pinsToPut);
    }



    @Deactivate
    public void deactivate() {

        //TODO Deactivate SensorTypes with this SensorID
        //TODO Deactivate this
        //TODO Remove from spiInitial list

        removeSensorTypes();


        for (Integer allocatedAdc: this.adcId
             ) {

            adcAndPinRemove(allocatedAdc);
        }


        super.deactivate();
        this.worker.deactivate();
    }

    private void removeSensorTypes() {

        for (SensorType willBeRemoved: spiInitial.getSensorTypeList()
             ) {
            if(willBeRemoved.getFatherId()==this.sensorId)
            {
                willBeRemoved.deactivate();
            }
        }
        spiInitial.getSensorManager().remove(this);


    }

    private void adcAndPinRemove(int allocatedAdc) {
        for (Adc adcShutdown: spiInitial.getAdcList()
        ) {
            if(adcShutdown.getId()==allocatedAdc) {
                for (Pin pin: adcShutdown.getPins()
                 ){
                    if(this.pinUsage.containsValue(pin)){
                        pin.setUsedBy(null);
                        pin.setUsed(false);
                    }

                }
            }
        }
    }

    @Override
    public String debugLog() {
        return "This will be a List for All Chip and Pin Usage, maybe with Sensor Types from List";
    }
    //TODO worker actually needed?
    private class SensorWorker extends AbstractCycleWorker{

        @Override
        public void activate(String name) {
            super.activate(name);
        }

        @Override
        public void deactivate() {
            super.deactivate();
        }

        @Override
        protected void forever() {

        }

    }

=======
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
>>>>>>> SPI
}
