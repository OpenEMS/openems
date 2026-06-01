import { NgModule } from "@angular/core";
import { ChannelThreshold } from "../../live/Controller/Channelthreshold/history/channelThreshold.module";
import { ControllerEssGridOptimizedCharge } from "../../live/Controller/Ess/GridOptimizedCharge/history/gridOptimizeCharge.module";
import { ControllerHeat } from "../../live/Controller/Heat/history/heat-history";
import { EnerixControl } from "./EnerixControl/enerixControl.module";
import { ControllerEss } from "./Ess/ess.module";
import { ControllerIo } from "./Io/Io.module";
import { ModbusTcpApi } from "./ModbusTcpApi/modbusTcpApi.module";

@NgModule({
    imports: [
        ControllerEss,
        ControllerIo,
        ChannelThreshold,
        EnerixControl,
        ModbusTcpApi,
        ControllerEssGridOptimizedCharge,
        ControllerHeat,
    ],
    exports: [
        ControllerEss,
        ControllerIo,
        ChannelThreshold,
        EnerixControl,
        ModbusTcpApi,
        ControllerEssGridOptimizedCharge,
        ControllerHeat,
    ],
})
export class Controller { }
