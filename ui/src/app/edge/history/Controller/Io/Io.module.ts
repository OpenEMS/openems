import { NgModule } from "@angular/core";
import { DigitalOutput } from "./DigitalOutput/digitalOutput.module";
import { HeatingElement } from "./heatingelement/heatingelement.module";

@NgModule({
    imports: [
        DigitalOutput,
        HeatingElement,
    ],
    exports: [
        DigitalOutput,
        HeatingElement,
    ],
})
export class ControllerIo { }
