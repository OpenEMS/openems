import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/SHARED.MODULE";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
    ],
    exports: [
        FlatComponent,
        ModalComponent,
    ],
})
export class ControllerIoFixDigitalOutput { }
