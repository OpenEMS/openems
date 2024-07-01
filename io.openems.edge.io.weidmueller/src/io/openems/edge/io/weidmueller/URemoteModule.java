package io.openems.edge.io.weidmueller;

import java.util.Optional;
import java.util.stream.Stream;

public enum URemoteModule {

	/* Digital input modules */
	UR20_4DI_P(0x00091F84, 1, 0),

	// UR20_4DI_P_3W(0x001B1F84), //
	// UR20_8DI_P_2W(0x00131FC1), //
	// UR20_8DI_P_3W(0x000A1FC1), //
	// UR20_8DI_P_3W_HD(0x00031FC1), //
	// UR20_16DI_P(0x00049FC2), //
	// UR20_16DI_P_PLC_INT(0x00059FC2), //
	// UR20_2DI_P_TS(0x0F014700), //
	// UR20_4DI_P_TS(0x0F024700), //
	// UR20_4DI_N(0x00011F84), //
	// UR20_8DI_N_3W(0x00021FC1), //
	// UR20_16DI_N(0x000C9FC2), //
	// UR20_16DI_N_PLC_INT(0x000D9FC2), //
	// UR20_4DI_2W_230V_AC(0x00169F84), //
	// UR20_8DI_ISO_2W(0x001C1FC1), //

	/* Digital output modules */
	// UR20_4DO_P(0x01012FA0), //
	// UR20_4DO_P_2A(0x01052FA0), //
	// UR20_4DO_PN_2A(0x01152FC8), //
	UR20_8DO_P(0x01022FC8, 0, 1), //
	// UR20_8DO_P_2W_HD(0x01192FC8), //
	// UR20_16DO_P(0x0103AFD0), //
	// UR20_16DO_P_PLC_INT(0x0104AFD0), //
	// UR20_4DO_N(0x010A2FA0), //
	// UR20_4DO_N_2A(0x010B2FA0), //
	// UR20_8DO_N(0x010C2FC8), //
	// UR20_16DO_N(0x010DAFD0), //
	// UR20_16DO_N_PLC_INT(0x010EAFD0), //
	// UR20_4DO_ISO_4A(0x011C2FA0), //
	// UR20_4RO_SSR_255(0x01072FA0), //
	// UR20_4RO_CO_255(0x01062FA0), //

	/* Digital input and output module */
	// UR20_8DIO_P_3W_DIAG(0x00223F49), //

	/* Digital pulse width modulation output modules */
	// UR20_2PWM_PN_0_5A(0x09084880), //
	// UR20_2PWM_PN_2A(0x09094880), //

	/* Stepper motor module */
	// UR20_1SM_50W_6DI2DO_P(0x09C34F6D), //

	/* Analogue input modules */
	// UR20_2AI_UI_16(0x042215C3), //
	// UR20_2AI_UI_16_DIAG(0x04231543), //
	// UR20_4AI_UI_16(0x040115C4), //
	// UR20_4AI_UI_16_DIAG(0x04021544), //
	// UR20_4AI_UI_DIF_16_DIAG(0x041E1544), //
	// UR20_4AI_UI_DIF_32_DIAG(0x041F1545), //
	// UR20_4AI_UI_ISO_16_DIAG(0x04211544), //
	// UR20_4AI_UI_16_HD(0x041315C4), //
	// UR20_4AI_UI_16_DIAG_HD(0x04141544), //
	// UR20_4AI_UI_12(0x041115C4), //
	// UR20_8AI_I_16_HD(0x040415C5), //
	// UR20_8AI_I_16_DIAG_HD(0x04051545), //
	// UR20_8AI_I_PLC_INT(0x040915C5), //
	// UR20_4AI_RTD_DIAG(0x04061544), //
	// UR20_4AI_RTD_HP_DIAG(0x04081544), //
	// UR20_4AI_TC_DIAG(0x04071544), //
	// UR20_4AI_R_HS_16_DIAG(0x041C1544), //
	// UR20_4AI_I_HART_16_DIAG(0x0E423F6D), //
	// UR20_8AI_RTD_DIAG_2W(0x04201545), //
	// UR20_2AI_SG_24_DIAG(0x041B356D), //
	// UR20_3EM_230V_AC(0x0418356D), //

	/* Analogue output modules */
	// UR20_2AO_UI_16(0x050925D8), //
	// UR20_2AO_UI_16_DIAG(0x05082558), //
	// UR20_2AO_UI_ISO_16_DIAG(0x05072558), //
	// UR20_4AO_UI_16(0x050225E0), //
	// UR20_4AO_UI_16_M(0x050625E0), //
	// UR20_4AO_UI_16_DIAG(0x05012560), //
	// UR20_4AO_UI_16_M_DIAG(0x05052560), //
	// UR20_4AO_UI_16_HD(0x050425E0), //
	// UR20_4AO_UI_16_DIAG_HD(0x05032560), //

	/* Digital counter modules */
	// UR20_1CNT_100_1DO(0x08C13800), //
	// UR20_2CNT_100(0x08C33800), //
	// UR20_1CNT_500(0x08C43801), //
	// UR20_2FCNT_100(0x088128EE), //

	/* Communication modules */
	// UR20_1SSI(0x09C17880), //
	// UR20_1COM_232_485_422(0x0E413FED), //
	// UR20_1COM_232_485_422_V2(0x0E44086D), //
	// UR20_1COM_SAI_PRO(0x0BC1E800), //
	// UR20_4COM_IO_LINK(0x0E81276D), //
	// UR20_4COM_IO_LINK_V2(0x0EA1276D), //

	/* Safe I/O modules */
	// UR20_4DI_4DO_PN_FSOE(0x001A7E40), //
	// UR20_4DI_4DO_PN_FSOE_V2(0x001F7E40), //
	// UR20_8DI_PN_FSOE(0x00176E40), //
	// UR20_8DI_PN_FSOE_V2(0x00206E40), //
	// UR20_4DI_4DO_PN_FSPS(0x00113E40), //
	// UR20_4DI_4DO_PN_FSPS_V2(0x001D3E40), //
	// UR20_8DI_PN_FSPS(0x00121E40), //
	// UR20_8DI_PN_FSPS_V2(0x001E1E40), //

	/* Safe feed-in modules */
	// UR20_PF_O_1DI_SIL(0x18019F43), //
	// UR20_PF_O_2DI_SIL(0x18039F43), //
	// UR20_PF_O_2DI_DELAY_SIL(0x18029F43), //

	/* Subbus modules SAI Active Universal Pro */
	// SAI_AU_M8_SB_8DI(0x0A011F41), //
	// SAI_AU_M12_SB_8DI(0x0A021F41), //
	// SAI_AU_M8_SB_8DIO(0x0A033F49), //
	// SAI_AU_M12_SB_8DIO(0x0A043F49), //
	// SAI_AU_M8_SB_8DO_2A(0x0B012F48), //
	// SAI_AU_M12_SB_8DO_2A(0x0B022F48), //
	// SAI_AU_M12_SB_4AI(0x0A411544), //
	// SAI_AU_M12_SB_4AO(0x0B412560), //
	// SAI_AU_M12_SB_4Thermo(0x0A431544), //
	// SAI_AU_M12_SB_4PT100(0x0A421544), //
	// SAI_AU_M12_SB_2Counter(0x0B813844), //
	;

	public final long moduleId;
	public final long noOfInputBytes; // page 67 of manual
	public final long noOfOutputBytes;

	private URemoteModule(long moduleId, int noOfInputBytes, int noOfOutputBytes) {
		this.moduleId = moduleId;
		this.noOfInputBytes = noOfInputBytes;
		this.noOfOutputBytes = noOfOutputBytes;
	}

	/**
	 * Gets a {@link URemoteModule} by its Module-ID.
	 * 
	 * @param moduleId the Module-ID
	 * @return an optional {@link URemoteModule}
	 */
	public static Optional<URemoteModule> getByModuleId(long moduleId) {
		return Stream.of(URemoteModule.values()) //
				.filter(m -> m.moduleId == moduleId) //
				.findFirst();
	}

}
