import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { NgChartsModule } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { PipeModule } from "src/app/shared/pipe/pipe";

import { HistoryDataErrorModule } from "../../history-data-error/history-data-error.module";
import { ElectricityMeterComponent } from "./electricity/modal.component";
import { EssChargerComponent } from "./esscharger/modal.component";

@NgModule({
    imports: [
        BrowserModule,
        IonicModule,
        PipeModule,
        TranslateModule,
        NgChartsModule,
        CommonModule,
        NgxSpinnerModule.forRoot({
            type: 'ball-clip-rotate-multiple',
        }),
        HistoryDataErrorModule,
    ],
    declarations: [
        ElectricityMeterComponent,
        EssChargerComponent,
    ],
    exports: [
        ElectricityMeterComponent,
        EssChargerComponent,
    ],
})
export class MeterModule { }
