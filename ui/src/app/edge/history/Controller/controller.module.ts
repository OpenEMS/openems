import { NgModule } from "@angular/core";
import { ChannelThreshold } from "./ChannelThreshold/channelThreshold.module";

@NgModule({
  imports: [
    ChannelThreshold,
  ],
  exports: [
    ChannelThreshold,
  ],
})
export class Controller { }
