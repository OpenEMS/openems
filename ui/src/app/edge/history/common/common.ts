import { NgModule } from "@angular/core";
import { Common_Autarchy } from "./autarchy/Autarchy";
import { Common_Consumption } from "./consumption/Consumption";
import { CommonEnergyMonitor } from "./energy/energy";
import { Common_Grid } from "./grid/grid";
import { Common_Production } from "./production/production";
import { Common_Selfconsumption } from "./selfconsumption/SelfConsumption";

@NgModule({
  imports: [
    Common_Autarchy,
    Common_Consumption,
    CommonEnergyMonitor,
    Common_Grid,
    Common_Production,
    Common_Selfconsumption,
  ],
  exports: [
    Common_Autarchy,
    Common_Consumption,
    CommonEnergyMonitor,
    Common_Grid,
    Common_Production,
    Common_Selfconsumption,
  ],
})
export class Common { }
