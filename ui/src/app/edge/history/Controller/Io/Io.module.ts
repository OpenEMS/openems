import { NgModule } from "@angular/core";
import { DigitalOutput } from "./DigitalOutput/DIGITAL_OUTPUT.MODULE";
import { HeatingElement } from "./heatingelement/HEATINGELEMENT.MODULE";

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
