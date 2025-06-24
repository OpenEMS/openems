import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { RouterModule, Routes } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { AlertingComponent } from "./alerting.component";

const routes: Routes = [
    {
        path: "",
        component: AlertingComponent,
    },
];

@NgModule({
    imports: [
        RouterModule.forChild(routes),
        AlertingComponent,
        ReactiveFormsModule,
        CommonModule,
        IonicModule,
        TranslateModule,
    ],
    declarations: [
    ],
    exports: [
        RouterModule,
        AlertingComponent,
    ],
})
export class AlertingModule { }
