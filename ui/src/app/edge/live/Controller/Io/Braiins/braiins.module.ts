import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { ScheduleComponent } from "src/app/shared/components/schedule/schedule.component";
import { SharedModule } from "src/app/shared/shared.module";
import { Language } from "src/app/shared/type/language";
import { ControllerBraiinsModeChartComponent } from "./new-navigation/chart/mode-chart";
import { ControllerBraiinsManagedConsumptionChartComponent } from "./new-navigation/chart/power-chart";
import { ControllerBraiinsHomeComponent } from "./new-navigation/new-navigation";
import { ControllerBraiinsModeComponent } from "./pages/mode/mode";
import { ControllerBraiinsScheduleComponent } from "./pages/schedule/schedule.component";
import { ControllerBraiinsAddTaskComponent } from "./pages/schedule/task/add/add";
import { ControllerBraiinsEditTaskComponent } from "./pages/schedule/task/edit/edit";
import de from "./shared/i18n/de.json";
import en from "./shared/i18n/en.json";

@NgModule({
    imports: [
        SharedModule,
        ModalModule,
        CommonModule,
        ComponentsBaseModule,
        ControllerBraiinsHomeComponent,
        ScheduleComponent,
        ControllerBraiinsScheduleComponent,
        ControllerBraiinsAddTaskComponent,
        ControllerBraiinsEditTaskComponent,
        ControllerBraiinsManagedConsumptionChartComponent,
        ControllerBraiinsModeChartComponent,
    ],
    declarations: [ControllerBraiinsModeComponent],
    exports: [ControllerBraiinsHomeComponent],
})
export class ControllerBraiinsModule {
    constructor(private readonly translate: TranslateService) {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then(
            (translations) => {
                for (const { lang, translation, shouldMerge } of translations) {
                    translate.setTranslation(lang, translation, shouldMerge);
                }
            },
        );
    }
}
