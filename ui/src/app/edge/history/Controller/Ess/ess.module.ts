import { NgModule } from "@angular/core";
import { ControllerEssTimeOfUseTariffHistory } from "src/app/edge/live/Controller/Ess/TimeOfUseTariff/history/timeOfUseTariff.module";

@NgModule({
    imports: [
        ControllerEssTimeOfUseTariffHistory,
    ],
    exports: [
        ControllerEssTimeOfUseTariffHistory,
    ],
})
export class ControllerEss { }
