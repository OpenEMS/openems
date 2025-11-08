import { NgModule } from "@angular/core";
import { CommonAutarchyHistory } from "../../live/common/autarchy/history/autarchy-history";
import { CommonGridHistory } from "../../live/common/grid/history/grid-history";
import { CommonSelfConsumptionHistory } from "../../live/common/selfconsumption/history/common-selfconsumption-history";
import { Common_Consumption } from "./consumption/Consumption";
import { CommonEnergyMonitor } from "./energy/energy";
import { Common_Production } from "./production/production";

@NgModule({
  imports: [
    CommonAutarchyHistory,
    CommonGridHistory,
    CommonSelfConsumptionHistory,
    Common_Consumption,
    CommonEnergyMonitor,
    Common_Production,
  ],
  exports: [
    CommonAutarchyHistory,
    CommonGridHistory,
    CommonSelfConsumptionHistory,
    Common_Consumption,
    CommonEnergyMonitor,
    Common_Production,
  ],
})
export class Common { }
