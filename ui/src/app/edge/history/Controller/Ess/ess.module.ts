import { NgModule } from "@angular/core";
import { TimeOfUseTariff } from "./TimeOfUseTariff/TIME_OF_USE_TARIFF.MODULE";

@NgModule({
    imports: [
        TimeOfUseTariff,
    ],
    exports: [
        TimeOfUseTariff,
    ],
})
export class ControllerEss { }
