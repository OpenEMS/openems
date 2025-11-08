import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { PipeComponentsModule, PipeModule } from "src/app/shared/pipe/pipe.module";

import { HistoryDataErrorModule } from "../../history-data-error/history-data-error.module";
import { ModalComponentsModule, ModalModule } from "../../modal/modal.module";
import { ElectricityMeterComponent } from "./electricity/modal.component";
import { EssChargerComponent } from "./esscharger/modal.component";

@NgModule({
    imports: [
        CommonUiModule,
        PipeComponentsModule,
        BaseChartDirective,
        NgxSpinnerModule.forRoot({
            type: "ball-clip-rotate-multiple",
        }),
        HistoryDataErrorModule,
        ModalComponentsModule,
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
export class MeterComponentsModule { }

@NgModule({
    imports: [
        BrowserModule,
        MeterComponentsModule,
        PipeModule,
        ModalModule,
    ],
    exports: [
        MeterComponentsModule,
    ],
})
export class MeterModule { }
