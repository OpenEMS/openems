import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ControllerEssGridOptimizedCharge } from "./history/gridOptimizeCharge.module";
import { ModalComponent } from "./modal/modal";
import { PredictionChartComponent } from "./modal/predictionChart";
import { ControllerEssGridOptimizedChargeHomeComponent } from "./new-navigation/new-navigation";
import { ControllerEssGridOptimizedChargeSettingsComponent } from "./settings/settings";
import { NewNavigationPredictionChartComponent } from "./shared/prediction-chart";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ControllerEssGridOptimizedCharge,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
        PredictionChartComponent,
        ControllerEssGridOptimizedChargeHomeComponent,
        ControllerEssGridOptimizedChargeSettingsComponent,
        NewNavigationPredictionChartComponent,
    ],
    exports: [
        FlatComponent,
        ControllerEssGridOptimizedCharge,
        ControllerEssGridOptimizedChargeHomeComponent,
        ControllerEssGridOptimizedChargeSettingsComponent,
    ],
})
export class ControllerEssGridOptimizedChargeModule { }
