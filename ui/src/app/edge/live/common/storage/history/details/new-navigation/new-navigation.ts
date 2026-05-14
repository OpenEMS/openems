import { Component } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { StorageEssChartComponent } from "../chart/esschart";

@Component({
    templateUrl: "./new-navigation.html",
    standalone: true,
    imports: [
        CommonUiModule,
        ComponentsBaseModule,
        StorageEssChartComponent,
    ],
})
export class CommonStorageDetailsComponent extends AbstractModal { }
