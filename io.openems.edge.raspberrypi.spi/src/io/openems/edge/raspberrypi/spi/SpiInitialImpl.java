package io.openems.edge.raspberrypi.spi;

import com.pi4j.wiringpi.Spi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

import io.openems.edge.raspberrypi.sensor.Sensor;
import io.openems.edge.raspberrypi.sensor.api.Adc.Adc;
import io.openems.edge.raspberrypi.sensor.sensortype.SensorType;
import io.openems.edge.raspberrypi.spi.api.BridgeSpi;
import io.openems.edge.raspberrypi.spi.task.Task;


@Designate(ocd = Config.class, factory = true)
@Component(name = "SpiInitial",
            immediate=true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
            property= EventConstants.EVENT_TOPIC+"="+EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class SpiInitialImpl extends AbstractOpenemsComponent implements SpiInitial, BridgeSpi, EventHandler, OpenemsComponent {

    private List<Adc> adcList = new ArrayList<>();
    private List<Sensor> sensorList = new ArrayList<>();
    private List<SensorType> sensorTypes = new ArrayList<>();

    //sensorManager --> father and child
    private Map <String, List<String>> sensorManager = new HashMap<>();
    //adcManager --> adc and Sensortypes
    private Map<String, Map<Integer, List<Integer>>> adcManager= new HashMap<>();
    //SpiManager --> SpiChannel and Adc
    private Map <Integer, String> SpiManager = new HashMap<>();
    private List<Integer> freeSpiChannels=new ArrayList<>();
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final SpiWorker worker = new SpiWorker();

    protected SpiInitialImpl(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    @Activate

    //TODO SPI Wiring Pi Setup; Do SPI Worker --> for every Channel
    public void activate(Config config) {
        super.activate(getComponentContext(), config.service_pid(), config.id(), config.enabled());
        if(this.isEnabled()){
            this.worker.activate(config.id());
        }
        //
        Spi.wiringPiSPISetup(0, config.frequency());

    }

    @Deactivate
    public void deactivate() {

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
    }

    @Override
    public void removeTask(String sourceId) {

    }

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
               int uebergabe = task.getSpiChannel(); //<---INT SpiSubChannel!
                //TODO SPI WIRINGPI DATARW(task.getChannel.getChannel, data); SpiChannel where to look -->From ADC
                task.setResponse(data);
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        if(event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE))
        {
            this.worker.triggerNextRun();
        }


    }

    //useful for checks if SpiSensor-->Channel is already used or if adc already exists


    public List<Adc> getAdcList() {
        return adcList;
    }


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


}
