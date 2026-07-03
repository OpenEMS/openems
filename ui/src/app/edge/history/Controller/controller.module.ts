import { NgModule } from "@angular/core";
import { ChannelThreshold } from "../../live/Controller/Channelthreshold/history/channelThreshold.module";
import { ControllerEssGridOptimizedCharge } from "../../live/Controller/Ess/GridOptimizedCharge/history/gridOptimizeCharge.module";
import { ControllerHeat } from "../../live/Controller/Heat/history/heat-history";
import { FixDigitalOutputHistory } from "../../live/Controller/Io/FixDigitalOutput/history/fix-digital-output-history.module";
import { ControllerModbusTcpApi } from "../../live/Controller/ModbusTcpApi/history/modbusTcpApi.module";
import { EnerixControl } from "./EnerixControl/enerixControl.module";
import { ControllerEss } from "./Ess/ess.module";
import { ControllerIo } from "./Io/Io.module";

@NgModule({
    imports: [
        ControllerEss,
        ControllerIo,
        EnerixControl,
        ControllerModbusTcpApi,
        FixDigitalOutputHistory,
        ControllerEssGridOptimizedCharge,
        ControllerHeat,
        ChannelThreshold,
    ],
    exports: [
        ControllerEss,
        ControllerIo,
        EnerixControl,
        ControllerModbusTcpApi,
        FixDigitalOutputHistory,
        ControllerEssGridOptimizedCharge,
        ControllerHeat,
        ChannelThreshold,
    ],
})
export class Controller {}
