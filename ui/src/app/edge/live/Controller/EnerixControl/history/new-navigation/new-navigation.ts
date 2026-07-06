import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { EnerixControlChartComponent } from "../chart/chart";

@Component({
    templateUrl: "./new-navigation.html",
    standalone: true,
    imports: [
        CommonUiModule,
        ComponentsBaseModule,
        CommonModule,
        IonicModule,
        EnerixControlChartComponent,
        PickdateComponentModule,
    ],
})
export class ControllerEnerixControlHistoryComponent extends AbstractModal {}
