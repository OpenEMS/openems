import { NgModule } from "@angular/core";
import { ChannelThreshold } from "./ChannelThreshold/channelThreshold.module";
import { ControllerEss } from "./Ess/ess.module";
import { GridOptimizeCharge } from "./Ess/GridoptimizedCharge/gridOptimizeCharge.module";
import { TimeOfUseTariff } from "./Ess/TimeOfUseTariff/timeOfUseTariff.module";
import { ControllerIo } from "./Io/Io.module";
import { ModbusTcpApi } from "./ModbusTcpApi/modbusTcpApi.module";

@NgModule({
  imports: [
    ControllerEss,
    ControllerIo,
    ChannelThreshold,
    TimeOfUseTariff,
    ModbusTcpApi,
    GridOptimizeCharge,
  ],
  exports: [
    ControllerEss,
    ControllerIo,
    ChannelThreshold,
    TimeOfUseTariff,
    ModbusTcpApi,
    GridOptimizeCharge,
  ],
})
export class Controller { }
