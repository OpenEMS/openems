import { NgModule } from "@angular/core";

import { ChannelThreshold } from "./ChannelThreshold/channelThreshold.module";
import { GridOptimizeCharge } from "./Ess/GridoptimizedCharge/gridOptimizeCharge.module";
import { TimeOfUseTariff } from "./Ess/TimeOfUseTariff/timeOfUseTariff.module";

@NgModule({
  imports: [
    ChannelThreshold,
    TimeOfUseTariff,
    GridOptimizeCharge,
  ],
  exports: [
    ChannelThreshold,
    TimeOfUseTariff,
    GridOptimizeCharge,
  ],
})
export class Controller { }
