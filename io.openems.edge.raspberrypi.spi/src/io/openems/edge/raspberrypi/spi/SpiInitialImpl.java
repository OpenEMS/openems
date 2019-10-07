package io.openems.edge.raspberrypi.spi;

<<<<<<< HEAD
import com.pi4j.wiringpi.Spi;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.raspberrypi.sensor.Sensor;
import io.openems.edge.raspberrypi.sensor.api.Adc.Adc;
import io.openems.edge.raspberrypi.sensor.sensortype.SensorType;
import io.openems.edge.raspberrypi.spi.api.BridgeSpi;
=======
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.sensor.api.Adc.Adc;
import io.openems.edge.raspberrypi.sensor.sensortype.SensorType;
import io.openems.edge.raspberrypi.spi.api.BridgeSpi;
import io.openems.edge.raspberrypi.spi.subchannel.SpiSensor;
>>>>>>> SPI
import io.openems.edge.raspberrypi.spi.task.Task;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
<<<<<<< HEAD
import java.util.concurrent.ConcurrentHashMap;
=======
>>>>>>> SPI


@Designate(ocd = Config.class, factory = true)
@Component(name = "SpiInitial")
@Config
public class SpiInitialImpl extends AbstractOpenemsComponent implements SpiInitial, BridgeSpi, EventHandler, OpenemsComponent {

<<<<<<< HEAD
    private List<Adc> adcList = new ArrayList<>();
    private List<Sensor> sensorList = new ArrayList<>();
    private List<SensorType> sensorTypes = new ArrayList<>();
    private String name;
    //sensorManager --> father and child
    private Map <String, List<String>> sensorManager = new HashMap<>();
    //adcManager --> adc and Sensortypes
    private Map<String, Map<Integer, List<Integer>>> adcManager= new HashMap<>();
    //SpiManager --> SpiChannel and Adc
    private Map <Integer, String> SpiManager = new HashMap<>();
    private List<Integer> freeSpiChannels=new ArrayList<>();
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final SpiWorker worker = new SpiWorker();
=======
    private Map<SpiSensor, SensorType> spiList = new HashMap<>();

    private List<Adc> adcList = new ArrayList<>();
    private Map<SensorType, Adc> adcPart = new HashMap<>();
    private String name;
>>>>>>> SPI

    protected SpiInitialImpl(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    @Activate
<<<<<<< HEAD

=======
    //TODO Create SPI List and ADC List, used by every device
>>>>>>> SPI
    //TODO SPI Wiring Pi Setup; Do SPI Worker --> for every Channel
    //TODO handle Event
    public void activate(Config config) {
        this.name = config.id();
<<<<<<< HEAD
        super.activate(getComponentContext(), config.service_pid(), config.id(), config.enabled());
        if(this.isEnabled()){
            this.worker.activate(config.id());
        }
        Spi.wiringPiSPISetup(0, config.frequency());

=======
>>>>>>> SPI
    }

    @Deactivate
    public void deactivate() {
<<<<<<< HEAD

        for (Sensor sensor: sensorList
             ) {
            sensor.deactivate();
        }
        this.worker.deactivate();
        super.deactivate();
    }


    @Override
    public void addTask(String sourceId, Task task) {
    this.tasks.put(sourceId, task);
=======
        //TODO Close every SPI SubChannel and with it, it's connected Devices
        //TODO Worker Deactivate
        super.deactivate();
    }

    //TODO Write Tasks
    @Override
    public void addTask(String sourceId, Task task) {

>>>>>>> SPI
    }

    @Override
    public void removeTask(String sourceId) {

    }

<<<<<<< HEAD
    private class SpiWorker extends AbstractCycleWorker{

        @Override
        public void activate(String name){
            super.activate(name);
        }
        @Override
        public void deactivate(){
            super.deactivate();
        }

        @Override
        protected void forever() throws Throwable {
            for(Task task : tasks.values()){
                byte[]data= task.getRequest();
                //TODO SPI WIRINGPI DATARW(task.getChannel.getChannel, data); SpiChannel where to look -->From ADC
                task.setResponse(data);
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        switch(event.getTopic()){
            case EdgeEventConstants
                    .TOPIC_CYCLE_EXECUTE_WRITE:
                this.worker.triggerNextRun();
            break;
        }
=======
    @Override
    public void handleEvent(Event event) {
>>>>>>> SPI

    }

    //useful for checks if SpiSensor-->Channel is already used or if adc already exists
<<<<<<< HEAD

=======
    public Map<SpiSensor, SensorType> getSpiList() {
        return spiList;
    }
>>>>>>> SPI

    public List<Adc> getAdcList() {
        return adcList;
    }

<<<<<<< HEAD

    @Override
    public boolean addAdcList(Adc adc) {
        return this.adcList.add(adc);
    }


    @Override
    public Map <String, List<String>>getSensorManager() {
        return sensorManager;
    }

    /*@Override
    public boolean addSensorManager() {
        return false;
    }
*/
    @Override
    public Map<String, Map<Integer, List<Integer>>> getAdcManager() {
        return adcManager;
    }


    @Override
    public Map<Integer, String> getSpiManager() {
        return SpiManager;
    }

    @Override
    public List<Sensor> getSensorList() {
        return sensorList;
    }
    @Override
    public List<SensorType> getSensorTypeList() {
        return sensorTypes;
    }

    @Override
    public boolean addToSensorManager(String child, String father){
        List<String> existingSensors = new ArrayList<>();
        for (Sensor fatherSensor: this.sensorList
             ) {
            if(fatherSensor.getSensorId().equals(father)){
                existingSensors=this.getSensorManager().get(father);
                return existingSensors.add(child);

            }
        }

        return false;
    }
    @Override
    public List<Integer> getFreeSpiChannels(){

        return this.freeSpiChannels;
    }


=======
    @Override
    public void addSpiList(SpiSensor spiSensor, SensorType sensorType) {
        this.spiList.put(spiSensor, sensorType);

    }

    @Override
    public boolean addAdcList(Adc adc) {
        return this.adcList.add(adc);
    }

    @Override
    public Map<SensorType, Adc> getAdcPart() {
        return adcPart;
    }

    @Override
    public boolean addAdcPart(SensorType sensorType, Adc adc) {

        return this.adcPart.put(sensorType, adc) == null;
    }
>>>>>>> SPI
}
