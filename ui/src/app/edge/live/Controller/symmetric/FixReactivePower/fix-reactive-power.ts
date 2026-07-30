import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { ControllerEssFixReactivePowerComponent } from "./new-navigation/new-navigation";

@NgModule({
    imports: [BrowserModule, SharedModule, ControllerEssFixReactivePowerComponent],
    exports: [ControllerEssFixReactivePowerComponent],
})
export class ControllerEssFixActivePower {}
