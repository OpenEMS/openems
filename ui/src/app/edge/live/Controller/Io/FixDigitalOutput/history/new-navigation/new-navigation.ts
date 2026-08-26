import { Component, LOCALE_ID, ChangeDetectionStrategy } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { Language } from "src/app/shared/type/language";

@Component({
    selector: "oe-fix-digital-input-history",
    templateUrl: "./new-navigation.html",
    standalone: true,
    imports: [
        ReactiveFormsModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        PickdateComponentModule,
        HistoryDataErrorModule,
    ],
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: LOCALE_ID, useFactory: () => Language.getCurrentLanguage().key }],
})
export class FixDigitalInputHistoryComponent extends AbstractModal {}
