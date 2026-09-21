import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { FixPowerComponent } from "../../../Ess/FixActivePower/shared/shared-new-navigation";

@Component({
    selector: "oe-controller-ess-fix-reactive-power",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerEssFixReactivePowerComponent extends FixPowerComponent {
    protected override readonly powerConverter: (value: number | null) => string =
        Converter.CONVERT_VAR_TO_KILO_VOLT_AMPERE_REACTIVE;
    protected override readonly unit: string = "kvar";
}
