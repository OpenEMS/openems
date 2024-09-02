import { NgModule } from "@angular/core";
import { DigitalOutput } from "./DigitalOutput/digitalOutput.module";

@NgModule({
    imports: [
        DigitalOutput,
    ],
    exports: [
        DigitalOutput,
    ],
})
export class ControllerIo { }
