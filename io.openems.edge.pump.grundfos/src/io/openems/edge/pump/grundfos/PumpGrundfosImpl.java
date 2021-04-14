package io.openems.edge.pump.grundfos;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.genibus.api.Genibus;
import io.openems.edge.bridge.genibus.api.PumpDevice;
import io.openems.edge.bridge.genibus.api.task.*;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.pump.grundfos.api.PumpGrundfosChannels;
import io.openems.edge.pump.grundfos.api.PumpType;
import io.openems.edge.pump.grundfos.api.WarnBits;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


@Designate(ocd = Config.class, factory = true)
@Component(name = "PumpGrundfos",
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE, //
        property = { //
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
        })
public class PumpGrundfosImpl extends AbstractOpenemsComponent implements OpenemsComponent, PumpGrundfosChannels, EventHandler {



    private AtomicReference<Genibus> genibusId = new AtomicReference<Genibus>(null);

    private Genibus genibus;

    /*
    @Reference(policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    Genibus genibus;
    */

    @Reference
    ComponentManager cpm;

    @Reference
    protected ConfigurationAdmin cm;

    private PumpType pumpType;
    private WarnBits warnBits;
    private PumpDevice pumpDevice;
    private boolean pumpWink;
    private boolean broadcast = false;
    private boolean changeAddress;
    private double newAddress;
    private boolean isMagna3;

    private boolean mpSetup;
    private boolean mpEnd;
    private boolean mpMaster;
    private int mpMasterAddr;
    private TpModeSetting tpMode;

    private final Logger log = LoggerFactory.getLogger(PumpGrundfosImpl.class);

    public PumpGrundfosImpl() {
        super(OpenemsComponent.ChannelId.values(), PumpGrundfosChannels.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());

        if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "Genibus", config.genibusBridgeId()) == false) {
            genibus = this.genibusId.get();
        }

        /*
        // Allocate components.
        if (cpm.getComponent(config.genibusBridgeId()) instanceof Genibus) {
            genibus = cpm.getComponent(config.genibusBridgeId());
        } else {
            throw new ConfigurationException(config.genibusBridgeId(), "The GENIbus bridge " + config.genibusBridgeId()
                    + " is not a (configured) GENIbus bridge.");
        }
        */

        isMagna3 = config.isMagna3();
        //allocatePumpType(config.pumpType());
        allocatePumpType("Magna3");
        pumpWink = config.pumpWink();
        this.broadcast = config.broadcast();
        changeAddress = config.changeAddress();
        newAddress = config.newAddress();

        mpSetup = config.mpSetup();
        mpEnd = config.mpEnd();
        mpMaster = config.mpMaster();
        mpMasterAddr = config.mpMasterAddress();
        tpMode = config.tpMode();
        if (broadcast) {
            createTaskList(super.id(), 254);
        } else {
            createTaskList(super.id(), config.pumpAddress());
        }
        pumpFlashLed();
    }

    private void allocatePumpType(String pumpType) {
        switch (pumpType) {
            case "Magna3":
                this.pumpType = PumpType.MAGNA_3;
                this.warnBits = WarnBits.MAGNA_3;
                break;
        }
    }

    @Deactivate
    public void deactivate() {
        Genibus genibus = this.genibusId.getAndSet(null);
        if (genibus != null) {
            genibus.removeDevice(super.id());
        }
        super.deactivate();
    }

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setGenibus(Genibus genibus) {
        this.genibusId.set(genibus);
    }

    protected void unsetGenibus(Genibus genibus) {
        this.genibusId.compareAndSet(genibus, null);
        if (genibus != null) {
            genibus.removeDevice(super.id());
        }
    }

    private void pumpFlashLed() throws OpenemsError.OpenemsNamedException {
        if (pumpWink) {
            this.setWinkOn().setNextWriteValue(true);
        } else {
            this.setWinkOff().setNextWriteValue(true);
        }
    }

    /** Creates a PumpDevice object containing all the tasks the GENIbus should send to this device. The PumpDevice is
     * then added to the GENIbus bridge.
     *
     * Tasks automatically decide if they are GET, SET or INFO. (If you don't know what that means, read the GENIbus specs.)
     * Right now only headclasses 0, 2, 3, 4, 5 and 7 are supported. Assuming a task is priority high:
     * - INFO is done when needed before any GET or SET.
     * - GET is done every cycle for all "Measured Data" tasks (= headclass 2), "Protocol Data" tasks (= headclass 0)
     *   and "ASCII" tasks (= headclass 7).
     * - SET is done every cycle for all "Command" tasks (= headclass 3), "Configuration Parameter" tasks (= headclass 4)
     *   and "Reference Value" tasks (= headclass 5) when there is a value in the "nextWrite" of the channel.
     *   The "nextWrite" of the channel is reset to "null" once the SET is executed. This means a SET is done just once.
     *   For repeated execution of SET, you need to repeatedly write in "nextWrite" of the channel.
     * - GET for headclass 4 and 5 (3 has no GET) is done once at the start, and then only after a SET to update the
     *   value. The result of the GET is written in the "nextValue" of the channel.
     * If the connection to a device is lost (pump switched off, serial connection unplugged), the controller will
     * attempt to reestablish the connection. If that succeeds, the device is treated as if it is a new device, meaning
     * all INFO is requested again, all once tasks done again etc.
     *
     * Suggestions for priority settings.
     * - Headclass 0 and 7:     once.
     * - Headclass 2:           high or low.
     * - Headclass 3, 4 and 5:  high.
     *
     * Tasks with more than 8 bit are handled as one task using "PumpReadTask16bitOrMore()" and "PumpWriteTask16bitOrMore()".
     * The address to put is the one of the "hi" value. The "low" values are always on the consecutive addresses. You also
     * need to specify how many bytes the task has (16 bit = 2 bytes, 24 bit = 3 bytes, 32 bit = 4 bytes). The byte
     * number is equivalent to the number of addresses a task has (two addresses = one hi, one low; means 16 bit = 2 bytes).
     * You can also use "PumpReadTask16bitOrMore()" and "PumpWriteTask16bitOrMore()" for 8 bit tasks by setting the byte
     * number to 1. "PumpReadTask8bit()" and "PumpWriteTask8bit()" function in that way, they map to the "16bitOrMore"
     * tasks.
     *
     * Data of a task is automatically converted according to the INFO of the task, but also according to OpenEMS
     * conventions. A pressure reading with unit "m" will be converted to "bar" in the OpenEMS channel. Temperature
     * readings will be converted to dC° in the channel. The channel unit is set accordingly.
     * For write tasks that are not boolean (headclass 4 and 5), the unit of INFO is used for the write as well.
     * Example: ref_rem (5, 1)
     * The unit of INFO is %, the range is 0% to 100%. The channel than has values between 0 and 1.0, and for sending a
     * SET with value 100%, write in the "nextWrite" of the channel "1.0".
     *
     * The tasks also allow for an optional "channel multiplier" as the last argument. This is a fixed value that is
     * used as a multiplier when reading a GET and as a divisor when writing a SET.
     *
     * @param deviceId
     * @param pumpAddress
     */
    private void createTaskList(String deviceId, int pumpAddress) {
        // Broadcast mode is just to find the address of a unit. Not suitable for sending commands.
        if (broadcast || changeAddress) {
            pumpDevice = new PumpDevice(deviceId, pumpAddress, 4,
                    new PumpReadTask8bit(2, 0, getBufferLength(), "Standard", Priority.ONCE),
                    new PumpReadTask8bit(3, 0, getUnitBusMode(), "Standard", Priority.ONCE),

                    new PumpReadTask8bit(148, 2, getUnitFamily(), "Standard", Priority.ONCE),
                    new PumpReadTask8bit(149, 2, getUnitType(), "Standard", Priority.ONCE),
                    new PumpReadTask8bit(150, 2, getUnitVersion(), "Standard", Priority.ONCE),

                    new PumpWriteTask8bit(46, 4, setUnitAddr(), "Standard", Priority.HIGH),
                    new PumpWriteTask8bit(47, 4, setGroupAddr(), "Standard", Priority.ONCE)
            );
            genibus.addDevice(pumpDevice);
            return;
        }

        if (mpSetup) {
            pumpDevice = new PumpDevice(deviceId, pumpAddress, 4,
                    new PumpCommandsTask(92, 3, setMpStartMultipump()),
                    new PumpCommandsTask(93, 3, setMpEndMultipump()),
                    new PumpCommandsTask(40, 3, setMpMaster()),
                    new PumpCommandsTask(87, 3, setMpStartSearch()),
                    new PumpCommandsTask(88, 3, setMpJoinReqAccepted()),

                    new PumpReadTask8bit(1, 2, getMultipumpMembers(), "Standard", Priority.HIGH),

                    new PumpWriteTask8bit(45, 4, setMpMasterAddr(), "Standard", Priority.HIGH),
                    new PumpWriteTask8bit(241, 4, setTpMode(), "Standard", Priority.HIGH)
            );
            genibus.addDevice(pumpDevice);
            return;
        }

        // The variable "lowPrioTasksPerCycle" lets you tune how fast the low priority tasks are executed. A higher
        // number means faster execution, up to the same execution speed as high priority tasks.
        // The controller will execute all high and low tasks once per cycle if there is enough time. A reduced execution
        // speed of low priority tasks happens only when there is not enough time.
        // There is also priority once, which as the name implies will be executed just once.
        //
        // What does "lowPrioTasksPerCycle" actually do?
        // Commands are sent to the pump device from a task queue. Each cycle, all the high tasks are added to the queue,
        // plus the amount "lowPrioTasksPerCycle" of low tasks. If the queue is empty before the cycle is finished, as
        // many low tasks as can still fit in this cycle will be executed as well. When all tasks have been executed
        // once this cycle, the controller will idle.
        // If there is not enough time, the execution rate of low tasks compared to high tasks depends on the total
        // amount of low tasks. The fastest execution rate (same as priority high) is reached when "lowPrioTasksPerCycle"
        // equals the total number of low tasks (value is capped at that number).
        // So if there are 10 low tasks and lowPrioTasksPerCycle=10, the low tasks behave like high tasks.
        // If in the same situation lowPrioTasksPerCycle=5, a priority low task is executed at half the rate of a
        // priority high task.
        pumpDevice = new PumpDevice(deviceId, pumpAddress, 4,


                // Commands.
                // If true is sent to to conflicting channels at the same time (e.g. start and stop), the pump
                // device will act on the command that was sent first. The command list is executed from top to bottom
                // in the order they are listed here.
                new PumpCommandsTask(this.pumpType.getRemote(),
                        this.pumpType.getRemoteHeadClass(), setRemote()),
                new PumpCommandsTask(this.pumpType.getStart(),
                        this.pumpType.getStartHeadClass(), this.setStart()),
                new PumpCommandsTask(this.pumpType.getStop(),
                        this.pumpType.getStopHeadClass(), this.setStop()),
                new PumpCommandsTask(this.pumpType.getMinMotorCurve(),
                        this.pumpType.getMinMotorCurveHeadClass(), setMinMotorCurve()),
                new PumpCommandsTask(this.pumpType.getMaxMotorCurve(),
                        this.pumpType.getMaxMotorCurveHeadClass(), setMaxMotorCurve()),
                new PumpCommandsTask(this.pumpType.getConstFrequency(),
                        this.pumpType.getConstFrequencyHeadClass(), setConstFrequency()),
                new PumpCommandsTask(this.pumpType.getConstPressure(),
                        this.pumpType.getConstPressureHeadClass(), setConstPressure()),
                new PumpCommandsTask(this.pumpType.getAutoAdapt(),
                        this.pumpType.getAutoAdaptHeadClass(), setAutoAdapt()),
                new PumpCommandsTask(121, 3, setWinkOn()),
                new PumpCommandsTask(122, 3, setWinkOff()),

                // Read tasks priority once
                new PumpReadTask8bit(2, 0, getBufferLength(), "Standard", Priority.ONCE),
                new PumpReadTask8bit(3, 0, getUnitBusMode(), "Standard", Priority.ONCE),
                new PumpReadTask8bit(148, 2, getUnitFamily(), "Standard", Priority.ONCE),
                new PumpReadTask8bit(149, 2, getUnitType(), "Standard", Priority.ONCE),
                new PumpReadTask8bit(150, 2, getUnitVersion(), "Standard", Priority.ONCE),

                // Read tasks priority high
                new PumpReadTask8bit(48, 2, getRefAct(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(49, 2, getRefNorm(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(90, 2, getControlSourceBits(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getPlo(), this.pumpType.getPloHeadClass(), getPowerConsumption(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getH(), this.pumpType.gethHeadClass(), getCurrentPressure(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getQ(), this.pumpType.getqHeadClass(), getCurrentPumpFlow(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.gettW(), this.pumpType.gettWHeadClass(), getPumpedWaterMediumTemperature(), "Standard", Priority.HIGH),
                // PumpReadTask has an optional channel multiplier. That is a double that is multiplied with the readout
                // value just before it is put in the channel. Here is an example of how to use this feature:
                // Apparently the unit returned by INFO is wrong. Unit type = 30 = 2*Hz, but the value is returned in Hz.
                // Could also be that the error is in the documentation and unit 30 is Hz and not Hz*2.
                new PumpReadTask8bit(this.pumpType.getfAct(), this.pumpType.getfActHeadClass(), getMotorFrequency(), "Standard", Priority.HIGH, 0.5),
                new PumpReadTask8bit(this.pumpType.getrMin(), this.pumpType.getrMinHeadClass(), getRmin(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getrMax(), this.pumpType.getrMaxHeadClass(), getRmax(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getControlMode(), this.pumpType.getControlModeHeadClass(), getActualControlModeBits(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(81, 2, getActMode1Bits(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getWarnCode(), this.pumpType.getWarnCodeHeadClass(), getWarnCode(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getAlarmCode(), this.pumpType.getAlarmCodeHeadClass(), getAlarmCode(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getWarnBits1(), this.pumpType.getWarnBits1HeadClass(), getWarnBits_1(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getWarnBits2(), this.pumpType.getWarnBits2HeadClass(), getWarnBits_2(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getWarnBits3(), this.pumpType.getWarnBits3HeadClass(), getWarnBits_3(), "Standard", Priority.HIGH),
                new PumpReadTask8bit(this.pumpType.getWarnBits4(), this.pumpType.getWarnBits4HeadClass(), getWarnBits_4(), "Standard", Priority.HIGH),

                // Read tasks priority low
                new PumpReadTask8bit(this.pumpType.gethDiff(), this.pumpType.gethDiffHeadClass(), getDiffPressureHead(), "Standard", Priority.LOW),
                new PumpReadTask8bit(this.pumpType.gettE(), this.pumpType.gettEheadClass(), getElectronicsTemperature(), "Standard", Priority.LOW),
                new PumpReadTask8bit(this.pumpType.getiMo(), this.pumpType.getImoHeadClass(), getCurrentMotor(), "Standard", Priority.LOW),
                new PumpReadTask8bit(2, 2, getTwinpumpStatus(), "Standard", Priority.LOW),

                new PumpReadTask8bit(this.pumpType.getAlarmCodePump(), this.pumpType.getAlarmCodePumpHeadClass(), getAlarmCodePump(), "Standard", Priority.LOW),
                new PumpReadTask8bit(163, 2, getAlarmLog1(), "Standard", Priority.LOW),
                new PumpReadTask8bit(164, 2, getAlarmLog2(), "Standard", Priority.LOW),
                new PumpReadTask8bit(165, 2, getAlarmLog3(), "Standard", Priority.LOW),
                new PumpReadTask8bit(166, 2, getAlarmLog4(), "Standard", Priority.LOW),
                new PumpReadTask8bit(167, 2, getAlarmLog5(), "Standard", Priority.LOW),

                // Config parameters tasks.
                // Class 4 tasks should always be priority high. They have special code to decide if they are sent or not.
                // Since the values in them are static unless changed by the user, they are read once at the start and
                // then only after a write. If a class 4 task is never written to, it essentially behaves like priority
                // once. It should be priority high so any writes are executed immediately.
                new PumpWriteTask8bit(this.pumpType.gethConstRefMax(), this.pumpType.gethConstRefMaxHeadClass(),
                        setConstRefMaxH(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(this.pumpType.gethConstRefMin(), this.pumpType.gethConstRefMinHeadClass(),
                        setConstRefMinH(), "Standard", Priority.HIGH),
                // The channel multiplier is also available for write tasks. For a GET it is a multiplier, for a SET it
                // is a divisor. Apparently all frequencies in the MAGNA3 are off by a factor of 2.
                new PumpWriteTask8bit(30, 4, setFupper(), "Standard", Priority.HIGH, 0.5),
                new PumpWriteTask8bit(34, 4, setFmin(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(35, 4, setFmax(), "Standard", Priority.HIGH),
                // Apparently all frequencies in the MAGNA3 are off by a factor of 2.
                new PumpWriteTask8bit(31, 4, setFnom(), "Standard", Priority.HIGH, 0.5),
                new PumpWriteTask16bitOrMore(2, this.pumpType.gethMaxHi(), this.pumpType.gethMaxHiHeadClass(),
                        setMaxPressure(), "Standard", Priority.HIGH),
                new PumpWriteTask16bitOrMore(2, this.pumpType.getqMaxHi(), this.pumpType.getqMaxHiHeadClass(),
                        setPumpMaxFlow(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(254, 4, setHrange(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(this.pumpType.getDeltaH(), this.pumpType.getDeltaHheadClass(), setPressureDelta(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(47, 4, setGroupAddr(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(241, 4, setTpMode(), "Standard", Priority.HIGH),

                // Sensor configuration
                new PumpWriteTask8bit(229, 4, setSensor1Func(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(226, 4, setSensor1Applic(), "Standard", Priority.HIGH),
                new PumpWriteTask8bit(208, 4, setSensor1Unit(), "Standard", Priority.HIGH),
                new PumpWriteTask16bitOrMore(2, 209, 4, setSensor1Min(), "Standard", Priority.HIGH),
                new PumpWriteTask16bitOrMore(2, 211, 4, setSensor1Max(), "Standard", Priority.HIGH),

                //new PumpReadTask8bit(127, 2, getSensorGsp(), "Standard", Priority.LOW),
                //new PumpWriteTask8bit(238, 4, setSensorGspFunc(), "Standard", Priority.LOW),

                // Reference values tasks
                new PumpWriteTask8bit(this.pumpType.getRefRem(), this.pumpType.getRefRemHeadClass(),
                        setRefRem(), "Standard", Priority.HIGH),

                // Strings
                new PumpReadTaskASCII(8, 7, getProductNumber(), "Standard", Priority.ONCE),
                new PumpReadTaskASCII(9, 7, getSerialNumber(), "Standard", Priority.ONCE),

                // Multipump commands
                new PumpCommandsTask(92, 3, setMpStartMultipump()),
                new PumpCommandsTask(93, 3, setMpEndMultipump()),
                new PumpCommandsTask(40, 3, setMpMaster()),
                new PumpCommandsTask(87, 3, setMpStartSearch()),
                new PumpCommandsTask(88, 3, setMpJoinReqAccepted()),

                new PumpReadTask8bit(1, 2, getMultipumpMembers(), "Standard", Priority.HIGH),

                new PumpWriteTask8bit(45, 4, setMpMasterAddr(), "Standard", Priority.HIGH)
                // setTpMode() is already in.
        );
        genibus.addDevice(pumpDevice);
    }

    @Override
    public PumpDevice getPumpDevice() {
        return pumpDevice;
    }

    @Override
    public void handleEvent(Event event) {
        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
                this.updateChannels();
                break;
        }
    }

    // Fill channels with data that are not directly read from the Genibus.
    private void updateChannels() {

        // Get connection status from pump, put it in the channel.
        isConnectionOk().setNextValue(pumpDevice.isConnectionOk());

        // Parse ControlSource value to a string.
        if (getControlSourceBits().value().isDefined()) {
            int controlSourceBits = (int)Math.round(getControlSourceBits().value().get());
            int priorityBits = controlSourceBits & 0b1111;
            int activeSourceBits = controlSourceBits >> 4;
            String source;
            switch (activeSourceBits) {
                case 1:
                    source = "Panel";
                    break;
                case 2:
                    source = "Network (e.g. GENIbus)";
                    break;
                case 3:
                    source = "Handheld device (e.g. GENIlink/GENIair)";
                    break;
                case 4:
                    source = "External input (DI, Limit exceeded, AI Stop)";
                    break;
                case 5:
                    source = "Stop button";
                    break;
                default:
                    source = "unknown";
            }
            getControlSource().setNextValue("Command source: " + source + ", priority: " + priorityBits);


            String mode = "unknown";
            if (isMagna3) {
                // The following code was tested to work with a Magna3 pump, but did not Work with an MGE pump.
                // The MGE has different priority values. At const. press. the Magna3 has priority 10 while the MGE has
                // priority 6. Also, getActualControlModeBits() does not work on the MGE.

                // Parse ActualControlMode value to a string.
                if (getActualControlModeBits().value().isDefined()) {
                    int controlModeValue = (int)Math.round(getActualControlModeBits().value().get());
                    switch (priorityBits) {
                        case 7:
                            mode = "Stopp";
                            break;
                        case 8:
                            mode = "Constant frequency - Max";
                            break;
                        case 9:
                            mode = "Constant frequency - Min";
                            break;
                        default:
                            switch (controlModeValue) {
                                case 0:
                                    mode = "Constant pressure";
                                    break;
                                case 1:
                                    mode = "Proportional pressure";
                                    break;
                                case 2:
                                    mode = "Constant frequency";
                                    break;
                                case 5:
                                    mode = "AutoAdapt";
                                    break;
                                case 6:
                                    mode = "Constant temperature";
                                    break;
                                case 7:
                                    mode = "Closed loop sensor control";
                                    break;
                                case 8:
                                    mode = "Constant flow";
                                    break;
                                case 9:
                                    mode = "Constant level";
                                    break;
                                case 10:
                                    mode = "FlowAdapt";
                                    break;
                                case 11:
                                    mode = "Constant differential pressure";
                                    break;
                                case 12:
                                    mode = "Constant differential temperature";
                                    break;
                            }
                    }
                }
            } else {

                // Parse ActualControlMode value to a string.
                if (getActMode1Bits().value().isDefined()) {
                    int controlModeBits = (int)Math.round(getActMode1Bits().value().get());
                    int operatingModes = controlModeBits & 0b111;
                    int controlModes = (controlModeBits >> 3) & 0b111;
                    boolean testMode = (controlModeBits >> 7) > 0;
                    switch (operatingModes) {
                        case 0:
                            //mode = "Start";   // Don't need that, since there should never be just "start" alone.
                            break;
                        case 1:
                            mode = "Stop";
                            break;
                        case 2:
                            mode = "Constant frequency - Min";
                            break;
                        case 3:
                            mode = "Constant frequency - Max";
                            break;
                        case 7:
                            mode = "Hand mode";
                            break;
                    }
                    switch (controlModes) {
                        case 0:
                            mode = "Constant pressure";
                            break;
                        case 1:
                            mode = "Proportional pressure";
                            break;
                        case 2:
                            mode = "Constant frequency";
                            break;
                        case 5:
                            mode = "AutoAdapt or FlowAdapt";
                            break;
                        case 6:
                            mode = "Other";
                            break;
                    }
                    if (testMode) {
                        mode = "Test";
                    }
                }
            }
            getActualControlMode().setNextValue(mode);
        }



        // Parse unit family, type and version
        StringBuilder allInfo = new StringBuilder();
        if (getUnitFamily().value().isDefined()) {
            allInfo.append("Unit family: ");
            int unitFamily = (int)Math.round(getUnitFamily().value().get());
            switch (unitFamily) {
                case 1:
                    allInfo.append("UPE/MAGNA, ");
                    break;
                case 2:
                    allInfo.append("MGE, ");
                    break;
                case 38:
                    allInfo.append("MAGNA Multi-pump, ");
                    break;
                case 39:
                    allInfo.append("MGE Multi-pump, ");
                    break;
                default:
                    allInfo.append(unitFamily).append(", ");
                    break;
            }
        }
        if (getUnitType().value().isDefined()) {
            allInfo.append("Unit type: ");
            int unitType = (int)Math.round(getUnitType().value().get());
            switch (unitType) {
                case 10:
                    allInfo.append("MAGNA3, ");
                    break;
                case 7:
                    allInfo.append("MGE model H/I, ");
                    break;
                default:
                    allInfo.append(unitType).append(", ");
                    break;
            }
        }
        if (getUnitVersion().value().isDefined()) {
            allInfo.append("Unit version: ");
            int unitVersion = (int)Math.round(getUnitVersion().value().get());
            switch (unitVersion) {
                case 1:
                    allInfo.append("Naked MGE, ");
                    break;
                case 2:
                    allInfo.append("Multi stage without sensor (CRE), ");
                    break;
                case 3:
                    allInfo.append("Multi stage with sensor (CRE), ");
                    break;
                case 4:
                    allInfo.append("Single stage Series 1000 (LME), ");
                    break;
                case 5:
                    allInfo.append("Single stage Series 2000 (LME), ");
                    break;
                case 6:
                    allInfo.append("Single stage Collect Series 1000 (MGE motor with MAGNA3 hydraulic), ");
                    break;
                case 7:
                    allInfo.append("Single stage Collect Series 2000 (MGE motor with MAGNA3 hydraulic), ");
                    break;
                case 8:
                    allInfo.append("Home booster, ");
                    break;
                default:
                    allInfo.append(unitVersion).append(", ");
                    break;
            }
        }
        if (allInfo.length() > 2) {
            allInfo.delete(allInfo.length() - 2, allInfo.length());
            allInfo.append(".");
            getUnitInfo().setNextValue(allInfo);
        }

        // Parse twinpump status value to a string.
        if (getTwinpumpStatus().value().isDefined()) {
            int twinpumpStatusValue = (int)Math.round(getTwinpumpStatus().value().get());
            String twinpumpStatusString;
            switch (twinpumpStatusValue) {
                case 0:
                    twinpumpStatusString = "Single pump. Not part of a multi pump.";
                    break;
                case 1:
                    twinpumpStatusString = "Twin-pump master. Contact to twin pump slave OK.";
                    break;
                case 2:
                    twinpumpStatusString = "Twin-pump master. No contact to twin pump slave.";
                    break;
                case 3:
                    twinpumpStatusString = "Twin-pump slave. Contact to twin pump master OK.";
                    break;
                case 4:
                    twinpumpStatusString = "Twin-pump slave. No contact to twin pump master.";
                    break;
                case 5:
                    twinpumpStatusString = "Self appointed twin-pump master. No contact to twin pump master.";
                    break;
                default:
                    twinpumpStatusString = "unknown";
            }
            getTwinpumpStatusString().setNextValue(twinpumpStatusString);
        }

        // Parse twinpump/multipump mode value to a string.
        if (setTpMode().value().isDefined()) {
            int twinpumpModeValue = (int)Math.round(setTpMode().value().get());
            String twinpumpModeString;
            switch (twinpumpModeValue) {
                case 0:
                    twinpumpModeString = "None, not part of a multi pump system.";
                    break;
                case 1:
                    twinpumpModeString = "Time alternating mode.";
                    break;
                case 2:
                    twinpumpModeString = "Load (power) alternating mode.";
                    break;
                case 3:
                    twinpumpModeString = "Cascade control mode.";
                    break;
                case 4:
                    twinpumpModeString = "Backup mode.";
                    break;
                default:
                    twinpumpModeString = "unknown";
            }
            getTpModeString().setNextValue(twinpumpModeString);
        }

        // Parse warn messages and put them all in one channel.
        StringBuilder allErrors = new StringBuilder();
        List<String> errorValue;
        if (getWarnBits_1().value().isDefined()) {
            int data = (int)Math.round(getWarnBits_1().value().get());
            errorValue = this.warnBits.getErrorBits1();
            for (int x = 0; x < 8; x++) {
                if ((data & (1 << x)) == (1 << x)) {
                    allErrors.append(errorValue.get(x));
                }
            }
        }
        if (getWarnBits_2().value().isDefined()) {
            int data = (int)Math.round(getWarnBits_2().value().get());
            errorValue = this.warnBits.getErrorBits2();
            for (int x = 0; x < 8; x++) {
                if ((data & (1 << x)) == (1 << x)) {
                    allErrors.append(errorValue.get(x));
                }
            }
        }
        if (getWarnBits_3().value().isDefined()) {
            int data = (int)Math.round(getWarnBits_3().value().get());
            errorValue = this.warnBits.getErrorBits3();
            for (int x = 0; x < 8; x++) {
                if ((data & (1 << x)) == (1 << x)) {
                    allErrors.append(errorValue.get(x));
                }
            }
        }
        if (getWarnBits_4().value().isDefined()) {
            int data = (int)Math.round(getWarnBits_4().value().get());
            errorValue = this.warnBits.getErrorBits4();
            for (int x = 0; x < 8; x++) {
                if ((data & (1 << x)) == (1 << x)) {
                    allErrors.append(errorValue.get(x));
                }
            }
        }
        getWarnMessage().setNextValue(allErrors);

        if (broadcast) {
            boolean signalReceived = isConnectionOk().value().isDefined() && isConnectionOk().value().get();
            //this.logInfo(this.log, "--GENIbus broadcast--");
            if (signalReceived == false) {
                //this.logInfo(this.log, "No signal received so far.");
            } else {
                String genibusAddress = "null";
                if (setUnitAddr().value().isDefined()) {
                    genibusAddress = "" + Math.round(setUnitAddr().value().get());
                }
                String groupAddress = "null";
                if (setGroupAddr().value().isDefined()) {
                    groupAddress = "" + Math.round(setGroupAddr().value().get());
                }
                String bufferLength = "null";
                if (getBufferLength().value().isDefined()) {
                    bufferLength = "" + Math.round(getBufferLength().value().get());
                }
                String busMode = "null";
                if (getUnitBusMode().value().isDefined()) {
                    busMode = "" + Math.round(getUnitBusMode().value().get());
                }

                //this.logInfo(this.log, "Pump found - " + getUnitInfo().value().get());
                //this.logInfo(this.log, "GENIbus address: " + genibusAddress);
                //this.logInfo(this.log, "Group address: " + groupAddress);
                //this.logInfo(this.log, "Buffer length: " + bufferLength);
                //this.logInfo(this.log, "Bus mode: " + busMode);
            }
        } else if (changeAddress) {
            if (newAddress > 31 && newAddress < 232) {
                try {
                    setUnitAddr().setNextWriteValue(newAddress);
                    //this.logInfo(this.log, "Pump address changed. New address = " + (Math.round(newAddress)) + ".");
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.logError(this.log, "Address change failed!");
                    e.printStackTrace();
                }
            } else {
                this.logError(this.log, "Value for new address = " + (Math.round(newAddress)) + " is not in the valid range (32 - 231). "
                        + "Not executing address change!");
            }
            changeAddress = false;
        } else if (mpSetup) {
            if (mpEnd) {
                try {
                    setMpEndMultipump().setNextWriteValue(true);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.logError(this.log, "Multipump setup failed!");
                    e.printStackTrace();
                }
            } else {
                if (mpMaster) {
                    try {
                        setMpMaster().setNextWriteValue(true);
                        setMpStartMultipump().setNextWriteValue(true);
                        setMpStartSearch().setNextWriteValue(true);
                        setMpJoinReqAccepted().setNextWriteValue(true);
                        setTpMode().setNextWriteValue((double)tpMode.getValue());
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.logError(this.log, "Multipump setup failed!");
                        e.printStackTrace();
                    }
                } else {

                    try {
                        setMpStartMultipump().setNextWriteValue(true);
                        setMpMasterAddr().setNextWriteValue((double)mpMasterAddr);
                        setMpStartSearch().setNextWriteValue(true);
                        setMpJoinReqAccepted().setNextWriteValue(true);
                        setTpMode().setNextWriteValue((double)tpMode.getValue());
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.logError(this.log, "Multipump setup failed!");
                        e.printStackTrace();
                    }
                }
            }
            int pumpCounter = 0;
            if (getMultipumpMembers().value().isDefined()) {
                int multipumpMemberBits = (int)Math.round(getMultipumpMembers().value().get());
                for (int i = 0; i < 8; i++) {
                    if (((multipumpMemberBits >> i) & 0b1) == 0b1) {
                        pumpCounter++;
                    }
                }
            }

            this.logInfo(this.log, "-- Multipump Setup   " +
                    "--");
            this.logInfo(this.log, "Multipump members: " + pumpCounter);
            this.logInfo(this.log, "Multipump mode: " + getTpModeString().value().get());
        }

    }

}
