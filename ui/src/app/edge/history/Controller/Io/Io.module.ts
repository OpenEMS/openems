import { NgModule } from "@angular/core";
import { FixDigitalOutputHistory } from "src/app/edge/live/Controller/Io/FixDigitalOutput/history/fix-digital-output-history.module";

@NgModule({
    imports: [
        FixDigitalOutputHistory,
    ],
    exports: [
        FixDigitalOutputHistory,
    ],
})
export class ControllerIo { }
