package io.openems.edge.bridge.lucidcontrol;

import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.lucidcontrol.api.LucidControlBridge;
import io.openems.edge.bridge.lucidcontrol.api.LucidControlBridgeTask;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.io.BufferedReader;
import java.io.IOException;

import java.io.InputStreamReader;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The LucidControlBridge. Modules are adding their Path and maxVoltage to this Bridge.
 * Devices add their tasks that will be handled by this Bridge.
 * Note: This Bridge only works if you install the Software from LucidControl here:
 *
 * @see <a href="https://www.lucid-control.com/downloads/">https://www.lucid-control.com/downloads/</a> <br>
 * The Bridge will open up the shell and reads/writes from/to the lucidcontrol devices.
 * Further Note: This Bridge only works with the Linux OS NOT Windows.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.Lucid.Control",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class LucidControlBridgeImpl extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler, LucidControlBridge {


    //String Key : Module Id, String Value  = Address
    private final Map<String, String> pathMap = new ConcurrentHashMap<>();
    //String Key: Module Id; Integer Value : Voltage of Module
    private final Map<String, String> voltageMap = new ConcurrentHashMap<>();

    Map<String, LucidControlBridgeTask> tasks = new ConcurrentHashMap<>();

    private final LucidControlWorker worker = new LucidControlWorker();

    private String lucidIoPath;

    public LucidControlBridgeImpl() {
        super(OpenemsComponent.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.lucidIoPath = config.lucidIoPath();
        if (config.enabled()) {
            this.worker.activate(super.id());
        }
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
        this.worker.deactivate();
        //shouldn't be necessary but just to make sure
        this.voltageMap.keySet().forEach(this::removeModule);
    }

    @Modified
    void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.lucidIoPath = config.lucidIoPath();
        if (config.enabled()) {
            this.worker.activate(super.id());
        }
    }

    /**
     * Adds the Path of the LucidControl Module.
     *
     * @param id   id of the module, usually from config.id()
     * @param path path of the Module, usually from config.path()
     */

    @Override
    public void addPath(String id, String path) {
        this.pathMap.put(id, path);
    }

    /**
     * Adds the max Voltage the LucidControl Module provides.
     *
     * @param id      id of the module, usually from config.id()
     * @param voltage max voltage of module, usually from config.voltage()
     */

    @Override
    public void addVoltage(String id, String voltage) {
        this.voltageMap.put(id, voltage);
    }

    /**
     * Removes the Module and it's connected Devices.
     *
     * @param id key to identify the connected Devices, usually from LucidControlModule via super.id()
     */
    @Override
    public void removeModule(String id) {
        this.tasks.values().removeIf(task -> task.getModuleId().equals(id));
        this.pathMap.remove(id);
        this.voltageMap.remove(id);
    }

    /**
     * Removes the LucidControlBridge task identified via id.
     *
     * @param id the unique id of the task, provided by LucidControl Device(usually from super.id()).
     */
    @Override
    public void removeTask(String id) {
        this.tasks.remove(id);
    }

    /**
     * Adds the LucidControlBridgeTask to this tasks.
     *
     * @param id    unique id provided by the LucidControlDevice
     * @param lucid Task created by the LucidControl Device
     */
    @Override
    public void addLucidControlTask(String id, LucidControlBridgeTask lucid) {
        this.tasks.put(id, lucid);
    }

    @Override
    public String getPath(String moduleId) {
        return this.pathMap.get(moduleId);
    }

    @Override
    public String getVoltage(String moduleId) {
        return this.voltageMap.get(moduleId);
    }

    private class LucidControlWorker extends AbstractCycleWorker {
        @Override
        public void activate(String id) {
            super.activate(id);
        }

        @Override
        public void deactivate() {
            super.deactivate();
        }

        /**
         * Provides the command for using the linux shell.
         * ATTENTION! No Windows support yet!
         * Output of shell provides a Voltage.
         * Always "Chip"+Number of Chip + ":" followed by Number e.g.
         * Chip0: -0.2548
         * That's why you can split the String at:
         * The Number Value is parsed to a double value and given to task, to calculate the Pressure value.
         */
        @Override
        protected void forever() {

            LucidControlBridgeImpl.this.tasks.values().forEach(task -> {
                if (task.isRead() || task.isWriteDefined()) {

                    String[] command = {"bash", "-c", LucidControlBridgeImpl.this.lucidIoPath + " -d" + task.getPath() + task.getRequest()};

                    String value = execCmd(command, task.isRead());
                    if (task.isRead()) {
                        if (value.contains(":")) {
                            if (value.contains("\t") && value.contains("\n")) {
                                value = value.replace("\t", "");
                                value = value.replace("\n", "");
                            }
                            String[] parts = value.split(":");
                            value = parts[1];
                        }
                        task.setResponse(Double.parseDouble(value));
                    }
                }
            });
        }


    }

    /**
     * Execute the command and returns the output.
     *
     * @param params the Command, containing bash exec and the line what should be written in such line
     * @param isRead a boolean to determine if the cmdline output should be read.
     * @return the output of the commandline e.g. Chip0:-1.5264
     */
    private static String execCmd(String[] params, boolean isRead) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(params);
            Process process = processBuilder.start();

            if (isRead) {
                StringBuilder output = new StringBuilder();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line;
                int counter = 0;
                while (counter < 5) {
                    line = reader.readLine();
                    if (line != null) {
                        output.append(line).append("\n");
                        break;
                    }
                    counter++;
                }
                return output.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Didn't Work";

        }
        return "Not Readable";
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            this.worker.triggerNextRun();
        }
    }

}
