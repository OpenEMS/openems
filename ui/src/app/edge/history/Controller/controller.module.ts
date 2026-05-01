import { NgModule } from "@angular/core";
import { ControllerEssGridOptimizedCharge } from "../../live/Controller/Ess/GridOptimizedCharge/history/gridOptimizeCharge.module";
import { ControllerHeat } from "../../live/Controller/Heat/history/heat-history";
import { ChannelThreshold } from "./ChannelThreshold/channelThreshold.module";
import { EnerixControl } from "./EnerixControl/enerixControl.module";
import { ControllerEss } from "./Ess/ess.module";
import { TimeOfUseTariff } from "./Ess/TimeOfUseTariff/timeOfUseTariff.module";
import { ControllerIo } from "./Io/Io.module";
import { ModbusTcpApi } from "./ModbusTcpApi/modbusTcpApi.module";

@NgModule({
    imports: [
        ControllerEss,
        ControllerIo,
        ChannelThreshold,
        EnerixControl,
        TimeOfUseTariff,
        ModbusTcpApi,
        ControllerEssGridOptimizedCharge,
        ControllerHeat,
    ],
    exports: [
        ControllerEss,
        ControllerIo,
        ChannelThreshold,
        EnerixControl,
        TimeOfUseTariff,
        ModbusTcpApi,
        ControllerEssGridOptimizedCharge,
        ControllerHeat,
    ],
})
export class Controller { }
