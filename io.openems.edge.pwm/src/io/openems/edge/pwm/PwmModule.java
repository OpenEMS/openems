package io.openems.edge.pwm;


import com.pi4j.io.gpio.*;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import io.openems.edge.bridgei2c.I2cBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.pwmModule.api.IpcaGpioProvider;
import io.openems.edge.pwmModule.api.Pca9685GpioProvider;
import io.openems.edge.pwmModule.api.PcaGpioProvider;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;
import java.math.BigDecimal;


@Designate(ocd = Config.class, factory = true)
@Component(name = "PwmModule",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class PwmModule extends AbstractOpenemsComponent implements OpenemsComponent {

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    public I2cBridge refI2cBridge;

    private BigDecimal frequency;
    private BigDecimal frequencyCorrectionFactor;
    private I2CBus i2CBus;
    private Pin[] pin;
    private PcaGpioProvider provider;
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
                    provider = new Pca9685GpioProvider(this.i2CBus, config.pwm_address(),
                            this.frequency, this.frequencyCorrectionFactor);
                    //                    for (int i = 0; i < 8; i++) {
                    //                        this.onPosition = checkForOverflow(offset * i);
                    //                        this.offPosition = checkForOverflow(pulseDuration * (i + 1));
                    //                        ((PCA9685GpioProvider) provider).setPwm(pin[i], onPosition, offPosition);
                    //                        providerSettings("PCA9685");
                    this.refI2cBridge.addGpioDevice(super.id(), (IpcaGpioProvider) provider);
                    //                    }
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

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


    private int checkForOverflow(int position) {
        int result = -66;
        if (this.provider instanceof Pca9685GpioProvider) {
            result = position;
            if (position > Pca9685GpioProvider.PWM_STEPS - 1) {
                result = position - Pca9685GpioProvider.PWM_STEPS - 1;
            }
        }
        return result;
    }
}
