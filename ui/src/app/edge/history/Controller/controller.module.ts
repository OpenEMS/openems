import { NgModule } from "@angular/core";
import { ChannelThreshold } from "./ChannelThreshold/channelThreshold.module";
import { TimeOfUseTariff } from "./Ess/TimeOfUseTariff/timeOfUseTariff.module";

@NgModule({
  imports: [
    ChannelThreshold,
    TimeOfUseTariff,
  ],
  exports: [
    ChannelThreshold,
    TimeOfUseTariff,
  ],
})
export class Controller { }
