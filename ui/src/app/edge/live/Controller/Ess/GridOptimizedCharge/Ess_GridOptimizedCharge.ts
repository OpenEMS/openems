import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";
import { PredictionChartComponent } from "./modal/predictionChart";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
        PredictionChartComponent,
    ],
    exports: [
        FlatComponent,
    ],
})
export class Controller_Ess_GridOptimizedCharge { }
