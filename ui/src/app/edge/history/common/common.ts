import { NgModule } from "@angular/core";
import { CommonAutarchyHistory } from "../../live/common/autarchy/history/autarchy-history";
import { CommonConsumptionHistory } from "../../live/common/consumption/history/consumption-history";
import { CommonGridHistory } from "../../live/common/grid/history/grid-history";
import { CommonSelfConsumptionHistory } from "../../live/common/selfconsumption/history/common-selfconsumption-history";
import { CommonEnergyMonitor } from "./energy/energy";
import { Common_Production } from "./production/production";

@NgModule({
    imports: [
        CommonAutarchyHistory,
        CommonConsumptionHistory,
        CommonGridHistory,
        CommonSelfConsumptionHistory,
        CommonEnergyMonitor,
        Common_Production,
    ],
    exports: [
        CommonAutarchyHistory,
        CommonConsumptionHistory,
        CommonGridHistory,
        CommonSelfConsumptionHistory,
        CommonEnergyMonitor,
        Common_Production,
    ],
})
export class Common { }
