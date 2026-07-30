import { Component, ChangeDetectionStrategy } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { StorageTotalChartComponent } from "../chart/totalchart";

@Component({
    selector: "oe-common-storage-history",
    templateUrl: "./new-navigation.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonUiModule, ComponentsBaseModule, StorageTotalChartComponent],
})
export class CommonStorageHistoryComponent extends AbstractModal {}
