import { NgModule } from "@angular/core";
import { CommonAutarchyHistory } from "../../live/common/autarchy/history/autarchy-history";
import { CommonConsumptionHistory } from "../../live/common/consumption/history/consumption-history";
import { CommonGridHistory } from "../../live/common/grid/history/grid-history";
import { CommonProductionHistory } from "../../live/common/production/history/production-history";
import { CommonSelfConsumptionHistory } from "../../live/common/selfconsumption/history/common-selfconsumption-history";
import { CommonEnergyMonitor } from "./energy/energy";

@NgModule({
    imports: [
        CommonAutarchyHistory,
        CommonConsumptionHistory,
        CommonGridHistory,
        CommonProductionHistory,
        CommonSelfConsumptionHistory,
        CommonEnergyMonitor,
    ],
    exports: [
        CommonAutarchyHistory,
        CommonConsumptionHistory,
        CommonGridHistory,
        CommonProductionHistory,
        CommonSelfConsumptionHistory,
        CommonEnergyMonitor,
    ],
})
export class Common { }
