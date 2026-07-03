import { NgModule } from "@angular/core";
import { ControllerEssGridOptimizedCharge } from "../../live/Controller/Ess/GridOptimizedCharge/history/gridOptimizeCharge.module";
import { ControllerHeat } from "../../live/Controller/Heat/history/heat-history";
import { FixDigitalOutputHistory } from "../../live/Controller/Io/FixDigitalOutput/history/fix-digital-output-history.module";
import { EnerixControl } from "./EnerixControl/enerixControl.module";
import { ControllerEss } from "./Ess/ess.module";
import { ControllerIo } from "./Io/Io.module";
import { ModbusTcpApi } from "./ModbusTcpApi/modbusTcpApi.module";

@NgModule({
    imports: [
        ControllerEss,
        ControllerIo,
        EnerixControl,
        ModbusTcpApi,
        FixDigitalOutputHistory,
        ControllerEssGridOptimizedCharge,
        ControllerHeat,
    ],
    exports: [
        ControllerEss,
        ControllerIo,
        EnerixControl,
        ModbusTcpApi,
        FixDigitalOutputHistory,
        ControllerEssGridOptimizedCharge,
        ControllerHeat,
    ],
})
export class Controller { }
