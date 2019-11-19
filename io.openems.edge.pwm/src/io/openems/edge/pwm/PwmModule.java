package io.openems.edge.pwm;

import com.pi4j.gpio.extension.pca.PCA9685GpioProvider;
import com.pi4j.gpio.extension.pca.PCA9685Pin;
import com.pi4j.io.gpio.*;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import io.openems.edge.bridgei2c.I2cBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;
import java.math.BigDecimal;


@Designate(ocd = Config.class, factory = true)
@Component(name = "PwmModule")
public class PwmModule extends AbstractOpenemsComponent implements OpenemsComponent {

    @Reference
    protected I2cBridge refI2cBridge;


    private BigDecimal frequency;
    private BigDecimal frequencyCorrectionFactor;
    private I2CBus i2CBus;
    private Pin[] pin;
    private GpioProviderBase provider;
    private GpioPinPwmOutput[] myOutputs;
    //    private int offset;
//    private int pulseDuration;
    private int onPosition;
    private int offPosition;
    private static final int SERVO_DURATION_MIN = 900;
    private static final int SERVO_DURATION_NEUTRAL = 1500;
    private static final int SERVO_DURATION_MAX = 2100;


    public PwmModule() {
        super(OpenemsComponent.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());

        allocateBus(config.bus_address());
        this.frequency = new BigDecimal(config.max_frequency());
        float correction = Float.parseFloat(config.actual_frequency()) / Float.parseFloat(config.max_frequency());
        this.frequencyCorrectionFactor = new BigDecimal(Float.toString(correction));
//        this.pulseDuration = config.pwm_pulseDuration();
        allocateGpioProvider(config);
    }

//    private float calcfrequency(int step_micro) {
//        return 4096 * (step_micro * (float) Math.pow(10, -6));
//    }

    @Deactivate
    public void deactivate() {
        refI2cBridge.removeGpioDevice(super.id());
        super.deactivate();
    }


    private void allocateGpioProvider(Config config) {
        try {
            switch (config.version()) {
                case "1":

                    provider = new PCA9685GpioProvider(this.i2CBus, Integer.parseInt(config.pwm_address()),
                            this.frequency, this.frequencyCorrectionFactor);
                    this.myOutputs = provisionPwmOutputs((PCA9685GpioProvider) provider);
                    ((PCA9685GpioProvider) provider).reset();
//                    for (int i = 0; i < 8; i++) {
                    this.pin = PCA9685Pin.ALL;
//                        this.onPosition = checkForOverflow(offset * i);
//                        this.offPosition = checkForOverflow(pulseDuration * (i + 1));
//                        ((PCA9685GpioProvider) provider).setPwm(pin[i], onPosition, offPosition);
//                        providerSettings("PCA9685");
                    this.refI2cBridge.addGpioDevice(super.id(), provider);
//                    }
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

//    private void providerSettings(String pca) {
//        switch (pca) {
//            case "PCA9685":
//                //Set full ON
//                ((PCA9685GpioProvider) provider).setAlwaysOn(PCA9685Pin.PWM_10);
//                //Set full Off
//                ((PCA9685GpioProvider) provider).setAlwaysOff(PCA9685Pin.PWM_11);
//                //Set 0.9ms pulse (R/C Servo minimum position)
//                provider.setPwm(PCA9685Pin.PWM_12, SERVO_DURATION_MIN);
//                // Set 1.5ms pulse (R/C Servo neutral position)
//                provider.setPwm(PCA9685Pin.PWM_13, SERVO_DURATION_NEUTRAL);
//                // Set 2.1ms pulse (R/C Servo maximum position)
//                provider.setPwm(PCA9685Pin.PWM_14, SERVO_DURATION_MAX);
//                for (GpioPinPwmOutput output : myOutputs) {
//                    int[] onOffValues = ((PCA9685GpioProvider) provider).getPwmOnOffValues(output.getPin());
//                    System.out.println(output.getPin().getName() + " (" + output.getName() + "): ON value [" + onOffValues[0] + "], OFF value [" + onOffValues[1] + "]");
//                }
//                break;
//        }
//    }

    private void allocateBus(int config) {
        try {

            switch (config) {

                case 0:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_0);
                    break;
                case 1:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_1);
                    break;
                case 2:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_2);
                    break;
                case 3:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_3);
                    break;
                case 4:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_4);
                    break;
                case 5:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_5);
                    break;
                case 6:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_6);
                    break;
                case 7:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_7);
                    break;
                case 8:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_8);
                    break;
                case 9:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_9);
                    break;
                case 10:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_10);
                    break;
                case 11:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_11);
                    break;
                case 12:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_12);
                    break;
                case 13:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_13);
                    break;
                case 14:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_14);
                    break;
                case 15:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_15);
                    break;
                case 16:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_16);
                    break;
                case 17:
                    this.i2CBus = I2CFactory.getInstance(I2CBus.BUS_17);
                    break;

            }

        } catch (IOException | I2CFactory.UnsupportedBusNumberException e) {
            e.printStackTrace();
        }

    }

    private static GpioPinPwmOutput[] provisionPwmOutputs(final PCA9685GpioProvider gpioProvider) {
        GpioController gpio = GpioFactory.getInstance();
        GpioPinPwmOutput myOutputs[] = {
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_00, "Pulse 00"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_01, "Pulse 01"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_02, "Pulse 02"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_03, "Pulse 03"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_04, "Pulse 04"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_05, "Pulse 05"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_06, "Pulse 06"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_07, "Pulse 07"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_08, "Pulse 08"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_09, "Pulse 09"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_10, "Always ON"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_11, "Always OFF"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_12, "Servo pulse MIN"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_13, "Servo pulse NEUTRAL"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_14, "Servo pulse MAX"),
                gpio.provisionPwmOutputPin(gpioProvider, PCA9685Pin.PWM_15, "not used")};
        return myOutputs;
    }

    private static int checkForOverflow(int position) {
        int result = position;
        if (position > PCA9685GpioProvider.PWM_STEPS - 1) {
            result = position - PCA9685GpioProvider.PWM_STEPS - 1;
        }
        return result;
    }
}
