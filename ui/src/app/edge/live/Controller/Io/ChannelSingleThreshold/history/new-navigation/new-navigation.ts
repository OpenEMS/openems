import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { SingleChartComponent } from "../../../../Channelthreshold/history/chart/singlechart.component";

@Component({
    selector: "oe-controller-channelthreshold-overview",
    templateUrl: "./new-navigation.html",
    standalone: true,
    imports: [
        ReactiveFormsModule,
        CommonModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
        SingleChartComponent,
        ComponentsBaseModule,
    ],
})
export class ControllerIoChannelSingleThresholdHistoryComponent extends AbstractHistoryChartOverview {}
