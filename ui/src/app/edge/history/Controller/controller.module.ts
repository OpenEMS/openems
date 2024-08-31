import { NgModule } from "@angular/core";
import { ControllerEss } from "./Ess/ess.module";
import { ControllerIo } from "./Io/Io.module";
import { ChannelThreshold } from "./ChannelThreshold/channelThreshold.module";
import { GridOptimizeCharge } from "./Ess/GridoptimizedCharge/gridOptimizeCharge.module";
import { TimeOfUseTariff } from "./Ess/TimeOfUseTariff/timeOfUseTariff.module";

@NgModule({
  imports: [
    ControllerEss,
    ControllerIo,
    ChannelThreshold,
    TimeOfUseTariff,
    GridOptimizeCharge,
  ],
  exports: [
    ControllerEss,
    ControllerIo,
    ChannelThreshold,
    TimeOfUseTariff,
    GridOptimizeCharge,
  ],
})
export class Controller { }
