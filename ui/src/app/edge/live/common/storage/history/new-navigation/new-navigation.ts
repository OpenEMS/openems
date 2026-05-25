import { Component } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { StorageTotalChartComponent } from "../chart/totalchart";

@Component({
    templateUrl: "./new-navigation.html",
    standalone: true,
    imports: [
        CommonUiModule,
        ComponentsBaseModule,
        StorageTotalChartComponent,
    ],
})
export class CommonStorageHistoryComponent extends AbstractModal { }
