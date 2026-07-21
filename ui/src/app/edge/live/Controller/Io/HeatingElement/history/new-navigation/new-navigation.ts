import { Component, ChangeDetectionStrategy } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ControllerIoHeatingElementChartComponent } from "../chart/chart";

@Component({
    selector: "oe-controller-heating-element-history",
    templateUrl: "./new-navigation.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
        ComponentsBaseModule,
        ControllerIoHeatingElementChartComponent,
    ],
})
export class ControllerHeatingElementHistoryComponent extends AbstractModal {}
