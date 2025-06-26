import { NgModule } from "@angular/core";
import { TimeOfUseTariff } from "./TimeOfUseTariff/timeOfUseTariff.module";

@NgModule({
    imports: [
        TimeOfUseTariff,
    ],
    exports: [
        TimeOfUseTariff,
    ],
})
export class ControllerEss { }
