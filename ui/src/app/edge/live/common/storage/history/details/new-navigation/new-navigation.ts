import { Component, ChangeDetectionStrategy } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { StorageEssChartComponent } from "../chart/esschart";

@Component({
    selector: "oe-common-storage-details",
    templateUrl: "./new-navigation.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonUiModule, ComponentsBaseModule, StorageEssChartComponent],
})
export class CommonStorageDetailsComponent extends AbstractModal {}
