import { Component } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChartComponent } from "../chart/chart";

@Component({
    selector: "oe-history",
    templateUrl: "./new-navigation.html",
    standalone: true,
    imports: [CommonUiModule, ComponentsBaseModule, ChartComponent],
})
export class HistoryChartComponent extends AbstractModal {}
