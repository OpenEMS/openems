import { NgModule } from "@angular/core";
import { ChannelThreshold } from "../../live/Controller/Channelthreshold/history/channelThreshold.module";
import { ControllerEnerixControlControlHistory } from "../../live/Controller/EnerixControl/history/enerixControl.module";
import { ControllerEssGridOptimizedCharge } from "../../live/Controller/Ess/GridOptimizedCharge/history/gridOptimizeCharge.module";
import { ControllerHeat } from "../../live/Controller/Heat/history/heat-history";
import { FixDigitalOutputHistory } from "../../live/Controller/Io/FixDigitalOutput/history/fix-digital-output-history.module";
import { ControllerModbusTcpApi } from "../../live/Controller/ModbusTcpApi/history/modbusTcpApi.module";
import { ControllerEss } from "./Ess/ess.module";
import { ControllerIo } from "./Io/Io.module";

@NgModule({
    imports: [
        ControllerEss,
        ControllerIo,
        ChannelThreshold,
        ControllerEnerixControlControlHistory,
        ControllerModbusTcpApi,
        FixDigitalOutputHistory,
        ControllerEssGridOptimizedCharge,
        ControllerHeat,
        ChannelThreshold,
    ],
    exports: [
        ControllerEss,
        ControllerIo,
        ChannelThreshold,
        ControllerEnerixControlControlHistory,
        ControllerModbusTcpApi,
        FixDigitalOutputHistory,
        ControllerEssGridOptimizedCharge,
        ControllerHeat,
        ChannelThreshold,
    ],
})
export class Controller {}
