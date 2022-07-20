import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { SharedModule } from 'src/app/shared/shared.module';
import { Flat } from './flat/flat';
import { Modal } from './modal/modal';
import { PredictionChartComponent } from './modal/predictionChart';

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    entryComponents: [
        Flat,
        Modal,
        PredictionChartComponent
    ],
    declarations: [
        Flat,
        Modal,
        PredictionChartComponent
    ],
    exports: [
        Flat
    ]
})
export class Controller_Ess_GridOptimizedCharge { }
