package io.openems.edge.heater.gasboiler.viessmann;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC2ReadInputsTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.heater.Heater;
import io.openems.edge.heater.HeaterState;
import io.openems.edge.heater.gasboiler.viessmann.api.GasBoiler;
import io.openems.edge.heater.gasboiler.viessmann.api.GasBoilerData;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Designate(ocd = Config.class, factory = true)
@Component(name = "GasBoiler",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class GasBoilerImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, GasBoilerData, GasBoiler, Heater, EventHandler {

    //in kW
    private int thermalOutput = 66;

    GasBoilerType gasBoilerType;
    private boolean isEnabled;
    private int cycleCounter = 0;

    private String[] errorList = ErrorList.STANDARD_ERRORS.getErrorList();

    private final Logger log = LoggerFactory.getLogger(GasBoilerImpl.class);

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Reference
    protected ConfigurationAdmin cm;

    public GasBoilerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                GasBoilerData.ChannelId.values(),
                Heater.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        allocateGasBoilerType(config.gasBoilerType());
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus", config.modbusBridgeId());
        if (config.maxThermicalOutput() != 0) {
            this.thermalOutput = config.maxThermicalOutput();
        }
        // Deactivating controllers for heating circuits because we will not need them
        this.getHc1OperationMode().setNextWriteValue(6);
        this.getHc2OperationMode().setNextWriteValue(6);
        this.getHc3OperationMode().setNextWriteValue(6);
    }

    private void allocateGasBoilerType(String gasBoilerType) {

        switch (gasBoilerType) {
            case "Placeholder":
            case "VITOTRONIC_100":
            default:
                this.gasBoilerType = GasBoilerType.VITOTRONIC_100;
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                new FC2ReadInputsTask(200, Priority.HIGH,
                        m(GasBoilerData.ChannelId.DISTURBANCE, new CoilElement(200)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_1, new CoilElement(201)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_2, new CoilElement(202)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_3, new CoilElement(203)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_4, new CoilElement(204)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_5, new CoilElement(205)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_6, new CoilElement(206)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_7, new CoilElement(207)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_8, new CoilElement(208)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_9, new CoilElement(209)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_10, new CoilElement(210)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_11, new CoilElement(211)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_12, new CoilElement(212)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_13, new CoilElement(213)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_14, new CoilElement(214)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_15, new CoilElement(215)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_16, new CoilElement(216)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_17, new CoilElement(217)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_18, new CoilElement(218)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_19, new CoilElement(219)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_20, new CoilElement(220)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_21, new CoilElement(221)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_22, new CoilElement(222)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_23, new CoilElement(223)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_24, new CoilElement(224)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_25, new CoilElement(225)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_26, new CoilElement(226)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_27, new CoilElement(227)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_28, new CoilElement(228)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_29, new CoilElement(229)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_30, new CoilElement(230)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_31, new CoilElement(231)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_32, new CoilElement(232)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_33, new CoilElement(233)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_34, new CoilElement(234)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_35, new CoilElement(235)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_36, new CoilElement(236)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_37, new CoilElement(237)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_38, new CoilElement(238)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_39, new CoilElement(239)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_40, new CoilElement(240)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_41, new CoilElement(241)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_42, new CoilElement(242)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_43, new CoilElement(243)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_44, new CoilElement(244)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_45, new CoilElement(245)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_46, new CoilElement(246)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_47, new CoilElement(247)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_48, new CoilElement(248)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_49, new CoilElement(249)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_50, new CoilElement(250)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_51, new CoilElement(251)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_52, new CoilElement(252)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_53, new CoilElement(253)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_54, new CoilElement(254)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_55, new CoilElement(255)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_56, new CoilElement(256)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_57, new CoilElement(257)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_58, new CoilElement(258)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_59, new CoilElement(259)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_60, new CoilElement(260)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_61, new CoilElement(261)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_62, new CoilElement(262)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_63, new CoilElement(263)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_64, new CoilElement(264)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_65, new CoilElement(265)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_66, new CoilElement(266)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_67, new CoilElement(267)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_68, new CoilElement(268)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_69, new CoilElement(269)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_70, new CoilElement(270)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_71, new CoilElement(271)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_72, new CoilElement(272)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_73, new CoilElement(273)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_74, new CoilElement(274)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_75, new CoilElement(275)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_76, new CoilElement(276)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_77, new CoilElement(277)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_78, new CoilElement(278)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_79, new CoilElement(279)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_80, new CoilElement(280)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_81, new CoilElement(281)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_82, new CoilElement(282)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_83, new CoilElement(283)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_84, new CoilElement(284)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_85, new CoilElement(285)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_86, new CoilElement(286)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_87, new CoilElement(287)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_88, new CoilElement(288)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_89, new CoilElement(289)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_90, new CoilElement(290)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_91, new CoilElement(291)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_92, new CoilElement(292)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_93, new CoilElement(293)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_94, new CoilElement(294)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_95, new CoilElement(295)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_96, new CoilElement(296)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_97, new CoilElement(297)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_98, new CoilElement(298)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_99, new CoilElement(299)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_100, new CoilElement(300)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_101, new CoilElement(301)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_102, new CoilElement(302)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_103, new CoilElement(303)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_104, new CoilElement(304)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_105, new CoilElement(305)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_106, new CoilElement(306)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_107, new CoilElement(307)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_108, new CoilElement(308)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_109, new CoilElement(309)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_110, new CoilElement(310)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_111, new CoilElement(311)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_112, new CoilElement(312)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_113, new CoilElement(313)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_114, new CoilElement(314)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_115, new CoilElement(315)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_116, new CoilElement(316)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_117, new CoilElement(317)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_118, new CoilElement(318)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_119, new CoilElement(319)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_120, new CoilElement(320)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_121, new CoilElement(321)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_122, new CoilElement(322)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_123, new CoilElement(323)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_124, new CoilElement(324)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_125, new CoilElement(325)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_126, new CoilElement(326)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_127, new CoilElement(327)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_128, new CoilElement(328)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_129, new CoilElement(329)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_130, new CoilElement(330)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_131, new CoilElement(331)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_132, new CoilElement(332)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_133, new CoilElement(333)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_134, new CoilElement(334)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_135, new CoilElement(335)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_136, new CoilElement(336)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_137, new CoilElement(337)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_138, new CoilElement(338)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_139, new CoilElement(339)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_140, new CoilElement(340)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_141, new CoilElement(341)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_142, new CoilElement(342)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_143, new CoilElement(343)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_144, new CoilElement(344)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_145, new CoilElement(345)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_146, new CoilElement(346)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_147, new CoilElement(347)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_148, new CoilElement(348)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_149, new CoilElement(349)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_150, new CoilElement(350)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_151, new CoilElement(351)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_152, new CoilElement(352)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_153, new CoilElement(353)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_154, new CoilElement(354)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_155, new CoilElement(355)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_156, new CoilElement(356)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_157, new CoilElement(357)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_158, new CoilElement(358)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_159, new CoilElement(359)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_160, new CoilElement(360)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_161, new CoilElement(361)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_162, new CoilElement(362)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_163, new CoilElement(363)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_164, new CoilElement(364)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_165, new CoilElement(365)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_166, new CoilElement(366)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_167, new CoilElement(367)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_168, new CoilElement(368)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_169, new CoilElement(369)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_170, new CoilElement(370)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_171, new CoilElement(371)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_172, new CoilElement(372)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_173, new CoilElement(373)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_174, new CoilElement(374)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_175, new CoilElement(375)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_176, new CoilElement(376)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_177, new CoilElement(377)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_178, new CoilElement(378)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_179, new CoilElement(379)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_180, new CoilElement(380)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_181, new CoilElement(381)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_182, new CoilElement(382)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_183, new CoilElement(383)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_184, new CoilElement(384)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_185, new CoilElement(385)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_186, new CoilElement(386)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_187, new CoilElement(387)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_188, new CoilElement(388)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_189, new CoilElement(389)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_190, new CoilElement(390)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_191, new CoilElement(391)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_192, new CoilElement(392)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_193, new CoilElement(393)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_194, new CoilElement(394)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_195, new CoilElement(395)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_196, new CoilElement(396)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_197, new CoilElement(397)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_198, new CoilElement(398)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_199, new CoilElement(399)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_200, new CoilElement(400)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_201, new CoilElement(401)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_202, new CoilElement(402)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_203, new CoilElement(403)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_204, new CoilElement(404)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_205, new CoilElement(405)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_206, new CoilElement(406)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_207, new CoilElement(407)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_208, new CoilElement(408)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_209, new CoilElement(409)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_210, new CoilElement(410)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_211, new CoilElement(411)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_212, new CoilElement(412)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_213, new CoilElement(413)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_214, new CoilElement(414)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_215, new CoilElement(415)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_216, new CoilElement(416)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_217, new CoilElement(417)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_218, new CoilElement(418)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_219, new CoilElement(419)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_220, new CoilElement(420)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_221, new CoilElement(421)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_222, new CoilElement(422)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_223, new CoilElement(423)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_224, new CoilElement(424)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_225, new CoilElement(425)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_226, new CoilElement(426)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_227, new CoilElement(427)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_228, new CoilElement(428)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_229, new CoilElement(429)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_230, new CoilElement(430)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_231, new CoilElement(431)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_232, new CoilElement(432)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_233, new CoilElement(433)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_234, new CoilElement(434)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_235, new CoilElement(435)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_236, new CoilElement(436)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_237, new CoilElement(437)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_238, new CoilElement(438)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_239, new CoilElement(439)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_240, new CoilElement(440)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_241, new CoilElement(441)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_242, new CoilElement(442)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_243, new CoilElement(443)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_244, new CoilElement(444)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_245, new CoilElement(445)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_246, new CoilElement(446)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_247, new CoilElement(447)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_248, new CoilElement(448)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_249, new CoilElement(449)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_250, new CoilElement(450)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_251, new CoilElement(451)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_252, new CoilElement(452)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_253, new CoilElement(453)),
                        m(GasBoilerData.ChannelId.ERROR_BIT_255, new CoilElement(455))
                ),
                // Write to holding registers
                new FC6WriteRegisterTask(5,
                        m(GasBoilerData.ChannelId.DEVICE_OPERATION_MODE, new UnsignedWordElement(5))),
                new FC6WriteRegisterTask(10,
                        m(GasBoilerData.ChannelId.DEVICE_POWER_MODE, new UnsignedWordElement(10))),
                new FC6WriteRegisterTask(11,
                        m(GasBoilerData.ChannelId.DEVICE_POWER_LEVEL_SETPOINT, new UnsignedWordElement(11), ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
                new FC6WriteRegisterTask(13,
                        m(GasBoilerData.ChannelId.DEVICE_FLOW_TEMPERATURE_SETPOINT, new UnsignedWordElement(13), ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
                new FC6WriteRegisterTask(17,
                        m(GasBoilerData.ChannelId.HC1_OPERATION_MODE, new UnsignedWordElement(17))),
                new FC6WriteRegisterTask(20,
                        m(GasBoilerData.ChannelId.HC2_OPERATION_MODE, new UnsignedWordElement(20))),
                new FC6WriteRegisterTask(23,
                        m(GasBoilerData.ChannelId.HC3_OPERATION_MODE, new UnsignedWordElement(23))),

                // reread written holding registers
                new FC3ReadRegistersTask(5, Priority.LOW,
                        m(GasBoilerData.ChannelId.DEVICE_OPERATION_MODE, new UnsignedWordElement(5)),
                        new DummyRegisterElement(6, 9),
                        m(GasBoilerData.ChannelId.DEVICE_POWER_MODE, new UnsignedWordElement(10)),
                        m(GasBoilerData.ChannelId.DEVICE_POWER_LEVEL_SETPOINT, new UnsignedWordElement(11), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(12, 12),
                        m(GasBoilerData.ChannelId.DEVICE_FLOW_TEMPERATURE_SETPOINT, new UnsignedWordElement(13), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(14, 16),
                        m(GasBoilerData.ChannelId.HC1_OPERATION_MODE, new UnsignedWordElement(17)),
                        new DummyRegisterElement(18, 19),
                        m(GasBoilerData.ChannelId.HC2_OPERATION_MODE, new UnsignedWordElement(20)),
                        new DummyRegisterElement(21, 22),
                        m(GasBoilerData.ChannelId.HC3_OPERATION_MODE, new UnsignedWordElement(23))
                ),

                // read input registers
                new FC4ReadInputRegistersTask(6, Priority.LOW,
                        m(GasBoilerData.ChannelId.DEVICE_POWER_LEVEL, new UnsignedWordElement(6), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(7, 7),
                        m(GasBoilerData.ChannelId.DEVICE_FLOW_TEMPERATURE, new UnsignedWordElement(8), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(9, 30),
                        m(GasBoilerData.ChannelId.DEVICE_RETURN_TEMPERATURE, new UnsignedWordElement(31)),
                        new DummyRegisterElement(32, 35),
                        m(GasBoilerData.ChannelId.OPERATING_HOURS_TIER1, new UnsignedWordElement(36)),
                        m(GasBoilerData.ChannelId.OPERATING_HOURS_TIER2, new UnsignedWordElement(37)),
                        m(GasBoilerData.ChannelId.BOILER_STARTS, new UnsignedWordElement(38)),
                        m(GasBoilerData.ChannelId.BOILER_STATE, new UnsignedWordElement(39))
                )
        );
    }

    @Override
    public boolean setPointPowerPercentAvailable() {
        return false;
    }

    @Override
    public boolean setPointPowerAvailable() {
        return false;
    }

    @Override
    public boolean setPointTemperatureAvailable() {
        return false;
    }

    @Override
    public int calculateProvidedPower(int demand, float bufferValue) throws OpenemsError.OpenemsNamedException {
        int providedPower = Math.round(((demand * bufferValue) * 100) / this.thermalOutput);
        // correcting for negative and values over 100
        if (providedPower < 0) {
            providedPower = 0;
        }

        if (providedPower > 100) {
            providedPower = 100;
        }

        if (providedPower > 0) {
            getDevicePowerMode().setNextWriteValue(1);
        }

        getDevicePowerLevelSetpoint().setNextWriteValue(providedPower);
        return (providedPower * thermalOutput) / 100;
    }

    @Override
    public int getMaximumThermalOutput() {
        return this.thermalOutput;
    }

    @Override
    public void setOffline() throws OpenemsError.OpenemsNamedException {
        if (this.getDisturbance().value().isDefined() && this.getDisturbance().value().get()) {
            log.warn("Error in Gasboiler : " + super.id());
            this.setState(HeaterState.ERROR.name());
        }
        this.getDevicePowerMode().setNextWriteValue(0);
    }


    @Override
    public boolean hasError() {
        if (this.hasErrorChannel().value().isDefined()) {
            return this.hasErrorChannel().value().get();
        } else {
            return false;
        }
    }

    @Override
    public void requestMaximumPower() {
        if (isEnabled) {
            try {
                this.getDevicePowerLevelSetpoint().setNextWriteValue(100);
                this.setState(HeaterState.RUNNING.name());
            } catch (OpenemsError.OpenemsNamedException e) {
                log.warn("Couldn't write in Channel " + e.getMessage());
            }
        }
    }

    @Override
    public void setIdle() {
        if (isEnabled) {
            try {
                this.getDevicePowerLevelSetpoint().setNextWriteValue(0);
                this.setState(HeaterState.AWAIT.name());
            } catch (OpenemsError.OpenemsNamedException e) {
                log.warn("Couldn't write in Channel " + e.getMessage());
            }

        }
    }


/*    @Override
    public String debugLog() {
        String out = "";
        System.out.println("--------------" + super.id() + "--------------");
        List<Channel<?>> all = new ArrayList<>();
        Arrays.stream(GasBoilerData.ChannelId.values()).forEach(consumer -> {
            all.add(this.channel(consumer));
        });
        all.forEach(consumer -> System.out.println(consumer.channelId().id() + " value: " + (consumer.value().isDefined() ? consumer.value().get() : "UNDEFINED ") + (consumer.channelDoc().getUnit().getSymbol())));
        //TODO: Error/Warning status etc
        System.out.println("----------------------------------");
        return "ok";
    }
*/

    private List<String> generateErrorList() {

        List<String> errorList = new ArrayList<>();

        for (int i = 0; i < 255; i++) {
            if (this.getError(i + 1).getNextValue().isDefined()) {
                if (this.getError(i + 1).getNextValue().get()) {
                    errorList.add(this.errorList[i]);
                }
            }
        }

        return errorList;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            channelmapping();
            List<String> errorList = generateErrorList();
            this.getErrorChannel().setNextValue(errorList.toString());
            if (errorList.isEmpty()) {
                this.hasErrorChannel().setNextValue(false);
            } else {
                this.hasErrorChannel().setNextValue(true);
            }

        }
    }

    protected void channelmapping() {
        // Decide state of enabledSignal.
        // The method isEnabledSignal() does get and reset. Calling it will clear the value (for that cycle). So you
        // need to store the value in a local variable.
        Optional<Boolean> enabledSignal = isEnabledSignal();
        if (enabledSignal.isPresent()) {
            isEnabled = enabledSignal.get();
            cycleCounter = 0;
        } else {
            // No value in the Optional.
            // Wait 5 cycles. If isEnabledSignal() has not been filled with a value again, switch to false.
            if (isEnabled) {
                cycleCounter++;
                if (cycleCounter > 5) {
                    isEnabled = false;
                }
            }
        }
    }
}
