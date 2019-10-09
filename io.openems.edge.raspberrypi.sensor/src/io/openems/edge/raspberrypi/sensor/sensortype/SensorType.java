package io.openems.edge.raspberrypi.sensor.sensortype;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.raspberrypi.sensor.Sensor;
import io.openems.edge.raspberrypi.sensor.api.Adc.Adc;
import io.openems.edge.raspberrypi.spi.SpiInitialImpl;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Designate(ocd = Config.class, factory = true)
@Component(name = "SensorType")

public abstract class SensorType extends AbstractOpenemsComponent {
    @Reference
    protected SpiInitialImpl spiInitial;


    private String typeId;
    private String fatherId;
    //TODO Implement OpenemsChannels; String ChannelId will be Indicator what u want to measure
    private String ChannelId; //Will be the Indicator for what it ll be used--> Like Temperature for temperature Channel
    //Which ADC uses Which Pin
    private Map<Integer, List<Integer>> pinUsage = new HashMap<>();

    public SensorType(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[][] furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }


    @Activate
    void activate(Config config) throws ConfigurationException {

        this.typeId = config.typeId();
        this.fatherId = config.fatherId();

        mapAdcAndPinUsage(config.adcId(), config.pinPositions());

        boolean fatherIdentified = false;
        for (Sensor father : spiInitial.getSensorList()
        ) {
            if (fatherId.equals(father.getSensorId())) {
                fatherIdentified = true;
                childAndFatherValidation(father);
                break;
            }
        }
        if (!fatherIdentified) {
            throw new ConfigurationException("No FatherSensor found, " +
                    "Check for Correct Spelling/See if you have instantiated Father first");
        }

        allocateUsedBy();


        if (spiInitial.addToSensorManager(this.typeId, this.fatherId)) {
            spiInitial.getSensorTypeList().add(this);
        }


    }

    @Deactivate
    public void deactivate() {
        removeUsedBy();
        spiInitial.getSensorManager().get(fatherId).remove(this.typeId);
        spiInitial.getSensorTypeList().remove(this);
    }


    //TODO Check entryChild etc
    private void removeUsedBy() {

        for (Map.Entry<Integer, List<Integer>> entryChild : pinUsage.entrySet()) {

            for (Adc adcParent : spiInitial.getAdcList()
            ) {
                if (adcParent.getId() == entryChild.getKey()) {
                    for (int childPin : pinUsage.get(entryChild)
                    ) {
                        if (adcParent.getPins().get(entryChild.getKey()).getPosition() == childPin) {
                            adcParent.getPins().get(entryChild.getKey()).setUsedBy("");
                        }
                    }
                }

            }
        }

    }


    public String getTypeId() {
        return typeId;
    }

    public String getFatherId() {
        return fatherId;
    }

    public String getChannelId() {
        return ChannelId;
    }

    public Map<Integer, List<Integer>> getPinUsage() {
        return pinUsage;
    }

    //TODO Check for correct iteration
    public void allocateUsedBy() {
        for (Map.Entry<Integer, List<Integer>> entryChild : pinUsage.entrySet()) {

            for (Adc adcParent : spiInitial.getAdcList()
            ) {
                if (adcParent.getId() == entryChild.getKey()) {
                    for (int childPin : pinUsage.get(entryChild)
                    ) {
                        if (adcParent.getPins().get(entryChild.getKey()).getPosition() == childPin) {
                            adcParent.getPins().get(entryChild.getKey()).setUsedBy(this.typeId);
                        }
                    }
                }

            }
        }

    }

    //TODO Check correct iteration map
    public void childAndFatherValidation(Sensor father) throws ConfigurationException {

        /*
        1. check if each ADC Id of Child is in Father Map
        */
        for (Integer keyChild : pinUsage.keySet()
        ) {
            if (!father.getPinUsage().containsKey(keyChild)) {
                throw new ConfigurationException("ADC withing Parent not found");
            }

            /*2. Check if each of allocated ADC PIN Child is part of Father Map
             * */
            //TODO Check correct Value
            for (Map.Entry<Integer, List<Integer>> entryChild : pinUsage.entrySet()
            ) {
                if (!father.getPinUsage().containsValue(entryChild)) {
                    throw new ConfigurationException("Pin within Parent not found");
                }
            }
            List<Integer> childPinList = pinUsage.get(keyChild);
            //Each Pin not allowed to be used by another child
            for (int childPin : childPinList) {
                if (!spiInitial.getAdcList().get(keyChild).getPins().get(childPin).getUsedBy().equals("")) {
                    throw new ConfigurationException("Pin " + childPin + "can't be allocated, already used by: " + spiInitial.getAdcList().get(keyChild).getPins().get(childPin).getUsedBy());
                }
            }

        }


    }

    public void mapAdcAndPinUsage(String adc, String pins) throws ConfigurationException {
        List<List<Integer>> pinList = new ArrayList<>();
        if (pins.contains(";")) {
            for (String pinSection : pins.split(";")
            ) {
                List<Integer> pinSelection = new ArrayList<>();

                for (char pin : pinSection.toCharArray()
                ) {
                    pinSelection.add(Character.getNumericValue(pin));

                }
                pinList.add(pinSelection);
            }
        } else {
            List<Integer> pinSelection = new ArrayList<>();
            for (char pin : pins.toCharArray()
            ) {
                pinSelection.add(Character.getNumericValue(pin));
            }
            pinList.add(pinSelection);
        }
        if (adc.contains(";")) {
            int indexCounter = 0;
            for (String adcSection : adc.split(";")
            ) {
                if (indexCounter > pinList.size()) {
                    throw new ConfigurationException("Too many Adcs or too few Pins were assigned");
                }

                pinUsage.put(Integer.parseInt(adcSection), pinList.get(indexCounter++));
            }
        } else {
            if (pinList.size() > 1) {
                throw new ConfigurationException("Too few Adcs or too many Pins were assigned");
            }
            pinUsage.put(Integer.parseInt(adc), pinList.get(0));
        }

    }


}
