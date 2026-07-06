import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ControllerEnerixControlControlHistory } from "./history/enerixControl.module";
import { ModalComponent } from "./modal/modal";
import { ControllerEnerixControlHomeComponent } from "./new-navigation/new-navigation";
import { ControllerEnerixControlSettingsComponent } from "./settings/settings";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ControllerEnerixControlControlHistory,
        ControllerEnerixControlHomeComponent,
        ControllerEnerixControlSettingsComponent,
    ],
    declarations: [FlatComponent, ModalComponent],
    exports: [
        FlatComponent,
        ControllerEnerixControlHomeComponent,
        ControllerEnerixControlSettingsComponent,
        ControllerEnerixControlControlHistory,
    ],
})
export class Controller_EnerixControl {}
