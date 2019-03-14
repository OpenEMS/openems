package io.openems.edge.ess.refu88k;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
//import io.openems.edge.common.channel.doc.Doc;
//import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EssREFUstore88K ess) {
		// Define the channels. Using streams + switch enables Eclipse IDE to tell us if
		// we are missing an Enum value.
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(ess, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SOC:
					case ACTIVE_POWER:
					case REACTIVE_POWER:
						return new IntegerReadChannel(ess, channelId);
					case MAX_APPARENT_POWER:	
						return new IntegerReadChannel(ess, channelId, EssREFUstore88K.MAX_APPARENT_POWER);
					case GRID_MODE:
						return new IntegerReadChannel(ess, channelId, GridMode.ON_GRID);
					case ACTIVE_CHARGE_ENERGY:
					case ACTIVE_DISCHARGE_ENERGY:
						return new LongReadChannel(ess, channelId);

					}
					return null;
				}), Arrays.stream(ManagedSymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER:
					case DEBUG_SET_REACTIVE_POWER:
					case ALLOWED_CHARGE_POWER:
					case ALLOWED_DISCHARGE_POWER:
						return new IntegerReadChannel(ess, channelId);
					case SET_ACTIVE_POWER_EQUALS:
					case SET_REACTIVE_POWER_EQUALS:
					case SET_ACTIVE_POWER_LESS_OR_EQUALS:
					case SET_ACTIVE_POWER_GREATER_OR_EQUALS:
					case SET_REACTIVE_POWER_LESS_OR_EQUALS:
					case SET_REACTIVE_POWER_GREATER_OR_EQUALS:
						return new IntegerWriteChannel(ess, channelId);
					}
					return null;
				}), Arrays.stream(EssREFUstore88K.ChannelId.values()).map(channelId -> {
					switch (channelId) {
								
					/*
					 * Model 121 (Inverter Controls Basic Settings)
					 */
					case WMax:
					case VRef:
					case VRefOfs:
					case WMax_SF:
					case VRef_SF:
					case VRefOfs_SF:	
					/*
					 * Model 123 (Immediate Inverter Controls)
					 */
					case CONN:
					case WMaxLimPct:
					case WMaxLim_Ena:
					case OutPFSet:
					case OutPFSet_Ena:
					case VArWMaxPct:
					case VArPct_Ena:
	
					/*
					 * Model 64040 (Request REFU Parameter ID)
					 */
					case ReadWriteParamId:
					case ReadWriteParamIndex:	
					/*
					 * Model 64041 (Answer REFU Parameter Value)
					 */
					case ReadWriteParamValue_U32:
					case ReadWriteParamValue_S32:
					case ReadWriteParamValue_F32:
					case ReadWriteParamValue_U16:
					case ReadWriteParamValue_S16:
					case ReadWriteParamValue_U8:
					case ReadWriteParamValue_S8:
						
					/*
					 * Model 64041 (Answer REFU Parameter Value)
					 */
					case PCSSetOperation:						
									
						return new IntegerWriteChannel(ess, channelId);

						
						
					/*
					 * Model 1 (Common)
					 */
					case Id_1:
					case L_1:
					case Mn:
					case Md:
					case Opt:
					case Vr:
					case SN:
					case DA: // RW
					case Pad_1:
					/*
					 * Model 103 (Inverter Three Phase)
					 */
					case Id_103:
					case L_103:
					case A:
					case AphA:
					case AphB:
					case AphC:
					case A_SF:
					case PPVphAB:
					case PPVphBC:
					case PPVphCA:
					case PhVphA:
					case PhVphB:
					case PhVphC:
					case V_SF:
					case W:
					case W_SF:
					case Hz:
					case Hz_SF:
					case VA:
					case VA_SF:
					case VAr:
					case VAr_SF:
					case WH:
					case WH_SF:
					case DCA:
					case DCA_SF:
					case DCV:
					case DCV_SF:
					case DCW:
					case DCW_SF:
					case TmpCab:
					case TmpSnk:
					case Tmp_SF:
					case St:
					case StVnd:
					case Evt1:
					case Evt2:
					case EvtVnd1:
					case EvtVnd2:
					case EvtVnd3:
					case EvtVnd4:
					/*
					 * Model 120 (Inverter Controls Nameplate Ratings)
					 */
					case Id_120:
					case L_120:
					case DERTyp:
					case WRtg:
					case WRtg_SF:
					case VARtg:
					case VARtg_SF:
					case VArRtgQ1:
					case VArRtgQ2:
					case VArRtgQ3:
					case VArRtgQ4:
					case VArRtg_SF:
					case ARtg:
					case ARtg_SF:
					case PFRtgQ1:
					case PFRtgQ2:
					case PFRtgQ3:
					case PFRtgQ4:
					case PFRtg_SF:
					case Pad_120:
					/*
					 * Model 121 (Inverter Controls Basic Settings)
					 */
					case Id_121:
					case L_121:	
					/*
					 * Model 123 (Immediate Inverter Controls)
					 */
					case Id_123:
					case L_123:	
					case WMaxLimPct_SF:
					case OutPFSet_SF:
					case VArPct_SF:	
					/*
					 * Model 64040 (Request REFU Parameter ID)
					 */
					case Id_64040:
					case L_64040:
						
					/*
					 * Model 64041 (Answer REFU Parameter Value)
					 */
					case Id_64041:
					case L_64041:
				
						return new IntegerReadChannel(ess, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}