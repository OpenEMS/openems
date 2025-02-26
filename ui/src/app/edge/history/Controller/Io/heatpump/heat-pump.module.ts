import { CommonModule } from "@angular/common";
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { RouterModule, Routes } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { FlatComponent } from "./flat/flat";
import { OverviewComponent } from "./overview/overview";

const routes: Routes = [
    {
        path: "",
        component: OverviewComponent,
    },
];

@NgModule({
    imports: [
        ReactiveFormsModule,
        CommonModule,
        IonicModule,
        TranslateModule,
        PickdateComponentModule,
        RouterModule.forChild(routes),
        HistoryDataErrorModule,
        NgxSpinnerModule,
    ],
    declarations: [
        FlatComponent,
    ],
    exports: [
        FlatComponent,
        RouterModule,
    ],
    schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
    ],
})
export class HeatPumpModule {
}

