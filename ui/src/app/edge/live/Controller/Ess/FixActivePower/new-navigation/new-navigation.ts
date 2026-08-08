import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Utils } from "src/app/shared/shared";
import { FixPowerComponent } from "../shared/shared-new-navigation";

@Component({
    selector: "oe-controller-ess-fix-active-power",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerEssFixActivePowerComponent extends FixPowerComponent {
    protected readonly powerConverter: (value: number | null) => string = Utils.CONVERT_WATT_TO_KILOWATT;
    protected readonly unit: string = "kW";
}
