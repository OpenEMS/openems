import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { SharedModule } from "src/app/shared/shared.module";
import { ControllerHeatComponent } from "./flat/flat";
import { ControllerHeat } from "./history/heat-history";
import { ControllerHeatModalComponent } from "./modal/modal";
import { ControllerHeatHomeComponent } from "./new-navigation/new-navigation";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        CommonModule,
        IonicModule,
        TranslateModule,
        ControllerHeat,
    ],
    declarations: [
        ControllerHeatComponent,
        ControllerHeatModalComponent,
        ControllerHeatHomeComponent,
    ],
    exports: [
        ControllerHeatComponent,
        ControllerHeat,
    ],
})
export class ControllerHeatModule { }
