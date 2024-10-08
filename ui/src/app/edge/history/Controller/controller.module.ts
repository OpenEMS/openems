import { NgModule } from "@angular/core";
import { ControllerEss } from "./Ess/ess.module";
import { ControllerIo } from "./Io/Io.module";
import { ChannelThreshold } from "./ChannelThreshold/channelThreshold.module";
import { GridOptimizeCharge } from "./Ess/GridoptimizedCharge/gridOptimizeCharge.module";
import { TimeOfUseTariff } from "./Ess/TimeOfUseTariff/timeOfUseTariff.module";
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
