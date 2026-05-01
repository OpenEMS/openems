import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ControllerHeatingElementHistory } from "./history/heatingelement.module";
import { ModalComponent } from "./modal/modal";
import { ControllerIoHeatingElementHomeComponent } from "./new-navigation/new-navigation";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ControllerHeatingElementHistory,
        ControllerIoHeatingElementHomeComponent,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
    ],
    exports: [
        FlatComponent,
        ControllerIoHeatingElementHomeComponent,
    ],
})
export class ControllerIoHeatingElement { }
