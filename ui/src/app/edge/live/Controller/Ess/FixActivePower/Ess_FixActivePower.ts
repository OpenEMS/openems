import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";
import { ControllerEssFixActivePowerHomeComponent } from "./new-navigation/new-navigation";
import { ControllerEssFixActivePowerSettingsComponent } from "./settings/settings";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ControllerEssFixActivePowerHomeComponent,
        ControllerEssFixActivePowerSettingsComponent,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
    ],
    exports: [
        FlatComponent,
        ControllerEssFixActivePowerHomeComponent,
        ControllerEssFixActivePowerSettingsComponent,
    ],
})
export class ControllerEssFixActivePower { }
