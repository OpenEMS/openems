package io.openems.edge.heater.gasboiler.viessmann.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface GasBoilerData extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /*
         * Informations will be got by ModBus.
         * That's why the Percentage Values got 2 Channels. 1 To Receive and Write the Correct Value and 1 for
         * human readable und writable values.
         *
         * */


        /**
         * Power Mode
         * 0   = OFF
         * 1   = ON:   Goto DEVICE_POWER_LEVEL_SETPOINT
         * 255 = AUTO: Goto DEVICE_OPERATION_MODE
         */

        DEVICE_POWER_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),


        /**
         * Power Level in Percent (Note: Modbus uses per mil)
         * 0       = Off
         * 1..50   = Run at 50%
         * 50..100 = Run at Specified Level
         */

        DEVICE_POWER_LEVEL_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),


        /**
         * Device Power Level in Percent (Modbus works with per mil)
         */

        DEVICE_POWER_LEVEL(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),


        /**
         * Device Operation Mode
         * 0   = HVAC_AUTO:       Regulate according to internal and external inputs (GoTo DEVICE_FLOW_TEMPERATURE_SETPOINT,HC1-3_OPERATION_MODE)
         * 1   = HVAC_HEAT:       Regulate according to internal and external inputs (GoTo DEVICE_FLOW_TEMPERATURE_SETPOINT,HC1-3_OPERATION_MODE)
         * 255 = HVAC_NUL:        Regulate according to internal and external inputs (GoTo DEVICE_FLOW_TEMPERATURE_SETPOINT,HC1-3_OPERATION_MODE)
         * 6   = HVAC_OFF:        Off
         * 7   = HVAC_TEST:       Run on minimal power (respecting boiler min/max temperature)
         * 111 = HVAC_LOW_FIRE:   Run on minimal power (respecting boiler min/max temperature)
         * 8   = HVAC_EMERG_HEAT: Run on full power (respecting boiler max temperature is reached)
         * 112 = HVAC_HIGH_FIRE:  Run on full power (respecting boiler max temperature is reached)
         */

        DEVICE_OPERATION_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),


        /**
         * Boiler State
         * 0 = off
         * 1 = Burner Tier 1 on
         * 2 = Burner Tier 2 on
         * 3 = Burner Tier 1+2 on
         */

        BOILER_STATE(Doc.of(OpenemsType.INTEGER)),


        /**
         * Setpoint for Device Flow Temperature in dezidegree Celsius (Note: Modbus uses centidegree Celcius)
         */

        DEVICE_FLOW_TEMPERATURE_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),


        /**
         * Flow temperature
         */

        DEVICE_FLOW_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),


        /**
         * Return temperature
         */

        DEVICE_RETURN_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),


        /**
         * Operation Mode for Heating Circuit 1. Only off will be use to ensure exclusive use of DEVICE_FLOW_TEMPERATURE_SETPOINT
         * 6 = HVAC_OFF: Off
         */

        HC1_OPERATION_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),


        /**
         * Operation Mode for Heating Circuit 1. Only off will be use to ensure exclusive use of DEVICE_FLOW_TEMPERATURE_SETPOINT
         * 6 = HVAC_OFF: Off
         */

        HC2_OPERATION_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),


        /**
         * Operation Mode for Heating Circuit 1. Only off will be use to ensure exclusive use of DEVICE_FLOW_TEMPERATURE_SETPOINT
         * 6 = HVAC_OFF: Off
         */

        HC3_OPERATION_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),


        /**
         * Operating hours Burner Tier 1
         */

        OPERATING_HOURS_TIER1(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),


        /**
         * Operating hours Burner Tier 2
         */

        OPERATING_HOURS_TIER2(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),


        /**
         * Number of Boiler starts
         */

        BOILER_STARTS(Doc.of(OpenemsType.INTEGER)),


        /**
         * Collective disturbance
         * true = disturbance
         */

        DISTURBANCE(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_1(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_2(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_3(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_4(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_5(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_6(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_7(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_8(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_9(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_10(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_11(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_12(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_13(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_14(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_15(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_16(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_17(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_18(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_19(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_20(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_21(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_22(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_23(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_24(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_25(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_26(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_27(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_28(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_29(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_30(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_31(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_32(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_33(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_34(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_35(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_36(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_37(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_38(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_39(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_40(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_41(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_42(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_43(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_44(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_45(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_46(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_47(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_48(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_49(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_50(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_51(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_52(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_53(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_54(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_55(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_56(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_57(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_58(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_59(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_60(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_61(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_62(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_63(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_64(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_65(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_66(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_67(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_68(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_69(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_70(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_71(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_72(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_73(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_74(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_75(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_76(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_77(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_78(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_79(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_80(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_81(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_82(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_83(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_84(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_85(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_86(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_87(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_88(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_89(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_90(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_91(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_92(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_93(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_94(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_95(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_96(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_97(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_98(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_99(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_100(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_101(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_102(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_103(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_104(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_105(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_106(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_107(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_108(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_109(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_110(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_111(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_112(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_113(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_114(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_115(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_116(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_117(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_118(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_119(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_120(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_121(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_122(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_123(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_124(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_125(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_126(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_127(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_128(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_129(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_130(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_131(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_132(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_133(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_134(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_135(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_136(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_137(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_138(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_139(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_140(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_141(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_142(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_143(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_144(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_145(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_146(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_147(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_148(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_149(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_150(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_151(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_152(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_153(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_154(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_155(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_156(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_157(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_158(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_159(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_160(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_161(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_162(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_163(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_164(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_165(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_166(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_167(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_168(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_169(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_170(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_171(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_172(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_173(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_174(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_175(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_176(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_177(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_178(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_179(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_180(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_181(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_182(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_183(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_184(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_185(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_186(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_187(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_188(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_189(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_190(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_191(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_192(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_193(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_194(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_195(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_196(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_197(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_198(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_199(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_200(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_201(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_202(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_203(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_204(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_205(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_206(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_207(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_208(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_209(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_210(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_211(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_212(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_213(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_214(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_215(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_216(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_217(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_218(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_219(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_220(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_221(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_222(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_223(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_224(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_225(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_226(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_227(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_228(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_229(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_230(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_231(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_232(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_233(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_234(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_235(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_236(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_237(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_238(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_239(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_240(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_241(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_242(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_243(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_244(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_245(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_246(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_247(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_248(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_249(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_250(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_251(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_252(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_253(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_254(Doc.of(OpenemsType.BOOLEAN)),
        ERROR_BIT_255(Doc.of(OpenemsType.BOOLEAN)),


        /**
         * All occuring Errors as String.
         */
        ERROR_CHANNEL(Doc.of(OpenemsType.STRING)),
        HAS_ERROR(Doc.of(OpenemsType.BOOLEAN));

//        /**
//         * Output 1 represented by Boolean.
//         * <li>Type: Boolean</li>
//         * 0 = Off
//         * 1 = On
//         */
//
//        OUTPUT_AM_1_1(Doc.of(OpenemsType.BOOLEAN)),
//        /**
//         * Output 2 represented by Boolean.
//         * <li>Type: Boolean</li>
//         * 0 = Off
//         * 1 = On
//         */
//        OUTPUT_AM_1_2(Doc.of(OpenemsType.BOOLEAN)),
//        /**
//         * Output 20 represented by Boolean.
//         * <li>Type: Boolean</li>
//         * 0 = Off
//         * 1 = On
//         */
//        OUTPUT_20(Doc.of(OpenemsType.BOOLEAN)),
//        /**
//         * Output 2 represented by Boolean.
//         * <li>Type: Boolean</li>
//         * 0 = Off
//         * 1 = On
//         */
//        OUTPUT_29(Doc.of(OpenemsType.BOOLEAN)),
//        /**
//         * Ambient Temperature in °C.
//         * <li>Type: Integer</li>
//         * <li>Unit: Degree Celsius</li>
//         */
//        AMBIENT_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        /**
//         * Output EA 1 represented by Boolean.
//         * <li>Type: Boolean</li>
//         * 0 = Off
//         * 1 = On
//         */
//        OUTPUT_EA_1(Doc.of(OpenemsType.BOOLEAN)),
//        /**
//         * Input EA 1-3 represented by Boolean.
//         * <li>Type: Boolean</li>
//         * 0 = Off
//         * 1 = ON
//         */
//        INPUT_EA_1(Doc.of(OpenemsType.BOOLEAN)),
//        INPUT_EA_2(Doc.of(OpenemsType.BOOLEAN)),
//        INPUT_EA_3(Doc.of(OpenemsType.BOOLEAN)),
//
//        /**
//         * SetPoint Range. 0_10V to 0_120°C.
//         * <li>Type: Integer</li>
//         * <li>Unit: Degree Celsius</li>
//         */
//        SETPOINT_EA_1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        /**
//         * Pumprotationvalue. 0-100% control signal == 0-10V
//         * 1 = 0.5%
//         */
//        OUTPUT_SIGNAL_PM_1(Doc.of(OpenemsType.INTEGER)),
//        /**
//         * Shows the Actual Value in Correct %.
//         */
//        OUTPUT_SIGNAL_PM_1_PERCENT(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
//        /**
//         * GRID_ELECTRICITY_PUMP. Represented by Boolean (On Off)
//         */
//        GRID_VOLTAGE_BEHAVIOUR_PM_1(Doc.of(OpenemsType.BOOLEAN)),
//        /**
//         * Potential free Electrical Contact of the Pump. Represented by Boolean (On Off)
//         */
//        FLOATING_ELECTRICAL_CONTACT_PM_1(Doc.of(OpenemsType.BOOLEAN)),
//        /**
//         * <p>Volume Flow Set Point of Pump Pm1.
//         * 1 == 0.5%
//         * </p>
//         */
//        VOLUME_FLOW_SET_POINT_PM_1(Doc.of(OpenemsType.INTEGER)),
//        /**
//         * Actual Percentage Value of Pump.
//         */
//
//        VOLUME_FLOW_SET_POINT_PM_1_PERCENT(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
//
//        DISTURBANCE_INPUT_PM_1(Doc.of(OpenemsType.BOOLEAN)),
//        /**
//         * Temperature Sensors 1-4 of Pump.
//         * <li>Unit: Degree Celsius</li>
//         */
//        TEMPERATURESENSOR_PM_1_1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        TEMPERATURESENSOR_PM_1_2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        TEMPERATURESENSOR_PM_1_3(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        TEMPERATURESENSOR_PM_1_4(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//
//        REWIND_TEMPERATURE_17A(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        REWIND_TEMPERATURE_17B(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        /**
//         * Additional Temperature Sensor, Appearing in the Datasheet of Vitogate 300.
//         */
//        SENSOR_9(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        /**
//         * The Signal becomes True if the Heating cycle or the Waterusage of Device
//         * sends a Temperature demand to the heat generation.
//         */
//        TRIBUTARY_PUMP(Doc.of(OpenemsType.BOOLEAN)),
//        /**
//         * OperatingMode A1 M1.
//         * 0: Off (Monitoried by Freezeprotection)
//         * 1: Only Heating Water (Running by autotimer programms, Freezeprotection)
//         * 2: Heating + Heating Water (Heating of room and above mentioned bulletpoints.
//         */
//        OPERATING_MODE_A1_M1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
//        /**
//         * Setpoint of Boiler Temperature 0-127°C.
//         */
//        BOILER_SET_POINT_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)
//                .accessMode(AccessMode.READ_WRITE)),
//        /**
//         * Exhaustion Temperature of the Combustion Engine.
//         */
//        COMBUSTION_ENGINE_EXHAUST_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        /**
//         * Combustion_Engine On Off represented by Boolean.
//         * 0 = Off
//         * 1 = On
//         */
//        COMBUSTION_ENGINE_ON_OFF(Doc.of(OpenemsType.BOOLEAN)),
//
//        COMBUSTION_ENGINE_OPERATING_HOURS_TIER_1(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),
//        COMBUSTION_ENGINE_OPERATING_HOURS_TIER_2(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),
//        /**
//         * Combustion engine Efficiency Actual Value.
//         * 1 == 0.5%
//         */
//        COMBUSTION_ENGINE_EFFICIENCY_ACTUAL_VALUE(Doc.of(OpenemsType.INTEGER)),
//        /**
//         * Human readable % Value.
//         */
//        COMBUSTION_ENGINE_EFFICIENCY_ACTUAL_VALUE_PERCENT(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
//        COMBUSTION_ENGINE_START_COUNTER(Doc.of(OpenemsType.INTEGER)),
//        /**
//         * Operation Modes.
//         * 0: Combustion engine off
//         * 1: Combustion engine Tier 1 on
//         * 2: Combustion Engine Tier 2 on
//         * 3: Combustion Engine Tier 1+2 on
//         */
//        COMBUSTION_ENGINE_OPERATING_MODE(Doc.of(OpenemsType.INTEGER)),
//
//        COMBUSTION_ENGINE_BOILER_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        /**
//         * Tells if the Sensor has an Error.
//         * 0: Everythings OK
//         * 1: Error
//         */
//        TEMPERATURE_SENSOR_1_PM_1_STATUS(Doc.of(OpenemsType.BOOLEAN)),
//        TEMPERATURE_SENSOR_2_PM_1_STATUS(Doc.of(OpenemsType.BOOLEAN)),
//        TEMPERATURE_SENSOR_3_PM_1_STATUS(Doc.of(OpenemsType.BOOLEAN)),
//        TEMPERATURE_SENSOR_4_PM_1_STATUS(Doc.of(OpenemsType.BOOLEAN)),
//
//        /**
//         * Expanded Diagnose Operating Hour Data.
//         */
//        OPERATING_HOURS_COMBUSTION_ENGINE_TIER_1_EXPANDED(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)
//                .accessMode(AccessMode.READ_WRITE)),
//        /**
//         * Operation modes of the heatingboiler.
//         * 0: HVAC_AUTO
//         * 1: HVAC_HEAT
//         * 2: HVAC_MRNG_WRMUP
//         * 3: HVAC_COOL
//         * 4: HVAC_NIGHT_PURGE
//         * 5: HVAC_PRE_COOL
//         * 6: HVAC_OFF
//         * 7: HVAC_TEST
//         * 8: HVAC_EMERG_HEAT
//         * 9: HVAC_FAN_ONLY
//         * 110: HVAC_SLAVE_ACTIVE
//         * 111: HVAC_LOW_FIRE
//         * 112: HVAC_HIGH_FIRE
//         * 255: HVAC_NUL
//         */
//        HEAT_BOILER_OPERATION_MODE(Doc.of(OpenemsType.INTEGER)),
//
//        HEAT_BOILER_TEMPERATURE_SET_POINT_EFFECTIVE(Doc.of(OpenemsType.INTEGER)),
//        /**
//         * Status represented by boolean.
//         * 0 = Off
//         * 1 = On
//         */
//        HEAT_BOILER_PERFORMANCE_STATUS(Doc.of(OpenemsType.BOOLEAN)),
//        /**
//         * Performance set point.
//         * 0 = Off
//         * 1 = On
//         * 255 = Auto
//         */
//        HEAT_BOILER_PERFORMANCE_SET_POINT_STATUS(Doc.of(OpenemsType.INTEGER)
//                .accessMode(AccessMode.READ_WRITE)),
//        /**
//         * Values 1 == 0.5%.
//         */
//        HEAT_BOILER_PERFORMANCE_SET_POINT_VALUE(Doc.of(OpenemsType.INTEGER)
//                .accessMode(AccessMode.READ_WRITE)),
//        /**
//         * For Actual Percentage Value(Easier to read/write.
//         */
//        HEAT_BOILER_PERFORMANCE_SET_POINT_VALUE_PERCENT(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)
//                .accessMode(AccessMode.READ_WRITE)),
//        /**
//         * Heat Boiler Temperature Set point Value between 0-127.
//         */
//        HEAT_BOILER_TEMPERATURE_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)
//                .accessMode(AccessMode.READ_WRITE)),
//        /**
//         * Actual measured Temperature.
//         */
//        HEAT_BOILER_TEMPERATURE_ACTUAL(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        /**
//         * Modulation Value between 0-100%.
//         * 1 == 0.5%
//         */
//        HEAT_BOILER_MODULATION_VALUE(Doc.of(OpenemsType.INTEGER)),
//        /**
//         * Humandreadable and writable % Values.
//         */
//        HEAT_BOILER_MODULATION_VALUE_PERCENT(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
//        /**
//         * Operation mode of warm water.
//         * 0: HVAC_AUTO
//         * 1: HVAC_HEAT
//         * 3: HVAC_COOL
//         * 4: HVAC_NIGHT_PURGE
//         * 5: HVAC_PRE_COOL
//         * 6: HVAC_OFF
//         * 255: HVAC_NUL
//         */
//        WARM_WATER_OPERATION_MODE(Doc.of(OpenemsType.INTEGER)
//                .accessMode(AccessMode.READ_WRITE)),
//        /**
//         * Reads the effective warm water set point temperature in °C.
//         */
//        WARM_WATER_EFFECTIVE_SET_POINT_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//
//        /**
//         * Sets the warm water Temperature between 0-90°C.
//         */
//        FUNCTIONING_WARM_WATER_SET_POINT_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)
//                .accessMode(AccessMode.READ_WRITE)),
//        /**
//         * Boiler Set Point Performance.
//         * 1 == 0.5%
//         */
//        BOILER_SET_POINT_PERFORMANCE_EFFECTIVE(Doc.of(OpenemsType.INTEGER)),
//        /**
//         * Human readable percentageValue.
//         */
//        BOILER_SET_POINT_PERFORMANCE_EFFECTIVE_PERCENT(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
//        /**
//         * Boiler Set Point temperature 0-127°C.
//         * Considers Boiler max temp. Boiler protection and freeze protection.
//         * <li>Unit: Degree Celsius</li>
//         */
//        BOILER_SET_POINT_TEMPERATURE_EFFECTIVE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        /**
//         * Max Reached Temperature of Boiler. Values between 0-500°C
//         * <li>Unit: Degree Celsius</li>
//         */
//        BOILER_MAX_REACHED_EXHAUST_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//
//        /**
//         * Status of the Warm Water storage pump.
//         * 0: Off
//         * 1: On
//         */
//        WARM_WATER_STORAGE_CHARGE_PUMP(Doc.of(OpenemsType.BOOLEAN)),
//        WARM_WATER_STORAGE_TEMPERATURE_5_A(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        WARM_WATER_STORAGE_TEMPERATURE_5_B(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
//        /**
//         * Tells the prep status of the Warm water.
//         * 0: Load inactive
//         * 1: Engine Start
//         * 2: Engine start Pump
//         * 3: Load active
//         * 4: Trail
//         */
//        WARM_WATER_PREPARATION(Doc.of(OpenemsType.INTEGER)),
//        /**
//         * Setpoint of Warmwater; Values between 10-95.
//         * <li>Unit: Degree Celsius</li>
//         * <p>
//         * Attention: Max. allowed potable water temperature.
//         * 10-60, with coding 56: 1 it's possible to set temp to 10-90.
//         * </p>
//         */
//        WARM_WATER_TEMPERATURE_SET_POINT(Doc.of(OpenemsType.INTEGER)
//                .accessMode(AccessMode.READ_WRITE)),
//        WARM_WATER_TEMPERATURE_SET_POINT_EFFECTIVE(Doc.of(OpenemsType.INTEGER)),
//        /**
//         * Ciruculation pump state.
//         * 0 = Off
//         * 1 = On
//         */
//        WARM_WATER_CIRCULATION_PUMP(Doc.of(OpenemsType.BOOLEAN));
//

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }


    default WriteChannel<Integer> getDevicePowerMode() {
        return this.channel(ChannelId.DEVICE_POWER_MODE);
    }

    default WriteChannel<Integer> getDevicePowerLevelSetpoint() {
        return this.channel(ChannelId.DEVICE_POWER_LEVEL_SETPOINT);
    }

    default WriteChannel<Integer> getDeviceOperationMode() {
        return this.channel(ChannelId.DEVICE_OPERATION_MODE);
    }

    default WriteChannel<Integer> getDeviceFlowTemperatureSetpoint() {
        return this.channel(ChannelId.DEVICE_FLOW_TEMPERATURE_SETPOINT);
    }

    default WriteChannel<Integer> getHc1OperationMode() {
        return this.channel(ChannelId.HC1_OPERATION_MODE);
    }

    default WriteChannel<Integer> getHc2OperationMode() {
        return this.channel(ChannelId.HC2_OPERATION_MODE);
    }

    default WriteChannel<Integer> getHc3OperationMode() {
        return this.channel(ChannelId.HC3_OPERATION_MODE);
    }


    default Channel<Integer> getDeviceFlowTemperature() {
        return this.channel(ChannelId.DEVICE_FLOW_TEMPERATURE);
    }

    default Channel<Integer> getDeviceReturnTemperature() {
        return this.channel(ChannelId.DEVICE_RETURN_TEMPERATURE);
    }

    default Channel<Integer> getOperatingHoursTier1() {
        return this.channel(ChannelId.OPERATING_HOURS_TIER1);
    }

    default Channel<Integer> getOperatingHoursTier2() {
        return this.channel(ChannelId.OPERATING_HOURS_TIER2);
    }

    default Channel<Integer> getBoilerStarts() {
        return this.channel(ChannelId.BOILER_STARTS);
    }

    default Channel<Integer> getDevicePowerLevel() {
        return this.channel(ChannelId.DEVICE_POWER_LEVEL);
    }

    default Channel<Integer> getBoilerState() {
        return this.channel(ChannelId.BOILER_STATE);
    }


    default Channel<Boolean> getDisturbance() {
        return this.channel(ChannelId.DISTURBANCE);
    }

    default Channel<Boolean> getError(int errNo) {
        return this.channel(ChannelId.valueOf("ERROR_BIT_" + errNo));
    }

    ;

    default Channel<String> getErrorChannel() {
        return this.channel(ChannelId.ERROR_CHANNEL);
    }

    default Channel<Boolean> hasErrorChannel() {
        return this.channel(ChannelId.HAS_ERROR);
    }


//     * Following Channels get the Value of member Channels and returns them as an actual Percentage Value
//     * */
//    default Channel<Float> getOutPutSignalPm1_Percent() {
//        return this.channel(ChannelId.OUTPUT_SIGNAL_PM_1_PERCENT);
//    }
//
//    default Channel<Float> getVolumeFlowSetPointPm1Percent() {
//        return this.channel(ChannelId.VOLUME_FLOW_SET_POINT_PM_1_PERCENT);
//    }
//
//    default Channel<Float> getCombustionEngineEfficiencyActualValuePercent() {
//        return this.channel(ChannelId.COMBUSTION_ENGINE_EFFICIENCY_ACTUAL_VALUE_PERCENT);
//    }
//
//    default WriteChannel<Float> getHeatBoilerPerformanceSetPointValuePercent() {
//        return this.channel(ChannelId.HEAT_BOILER_PERFORMANCE_SET_POINT_VALUE_PERCENT);
//    }
//
//    default Channel<Float> getHeatBoilerModulationValuePercent() {
//        return this.channel(ChannelId.HEAT_BOILER_MODULATION_VALUE_PERCENT);
//    }
//
//    default Channel<Float> getBoilerSetPointPerformanceEffectivePercent() {
//        return this.channel(ChannelId.BOILER_SET_POINT_PERFORMANCE_EFFECTIVE_PERCENT);
//    }

}

