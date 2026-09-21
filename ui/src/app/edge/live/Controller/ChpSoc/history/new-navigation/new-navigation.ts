import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ControllerChpChartComponent } from "../chart/chart";

@Component({
    selector: "oe-controller-chp-history",
    templateUrl: "./new-navigation.html",
    imports: [
        CommonModule,
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
        ComponentsBaseModule,
        ControllerChpChartComponent,
    ],
})
export class ControllerChpHistoryComponent extends AbstractModal {}
