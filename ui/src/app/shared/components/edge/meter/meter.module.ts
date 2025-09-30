import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { PipeModule } from "src/app/shared/pipe/PIPE.MODULE";

import { HistoryDataErrorModule } from "../../history-data-error/history-data-ERROR.MODULE";
import { ModalModule } from "../../modal/MODAL.MODULE";
import { ElectricityMeterComponent } from "./electricity/MODAL.COMPONENT";
import { EssChargerComponent } from "./esscharger/MODAL.COMPONENT";

@NgModule({
    imports: [
        BrowserModule,
        IonicModule,
        PipeModule,
        TranslateModule,
        BaseChartDirective,
        CommonModule,
        NGX_SPINNER_MODULE.FOR_ROOT({
            type: "ball-clip-rotate-multiple",
        }),
        HistoryDataErrorModule,
        ModalModule,
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
