import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { RouterModule, Routes } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { AlertingComponent } from "./ALERTING.COMPONENT";

const routes: Routes = [
    {
        path: "",
        component: AlertingComponent,
    },
];

@NgModule({
    imports: [
        ROUTER_MODULE.FOR_CHILD(routes),
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
