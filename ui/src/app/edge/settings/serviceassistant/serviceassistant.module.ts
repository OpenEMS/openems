import { NgModule } from "@angular/core";
import { SharedModule } from "src/app/shared/shared.module";
import { ServiceAssistantComponent } from "./serviceassistant.component";
import { SoltaroCellChartComponent } from "./soltarosingle/cell-chart/chart.component";
import { SoltaroVersionBComponent } from "./soltarosingle/soltaroB.component";
import { SoltaroVersionCComponent } from "./soltarosingle/soltaroC.component";

@NgModule({
    imports: [
        SharedModule
    ],
    entryComponents: [
    ],
    declarations: [
        ServiceAssistantComponent,
        SoltaroCellChartComponent,
        SoltaroVersionBComponent,
        SoltaroVersionCComponent
    ],
    exports: [ServiceAssistantComponent]
})
export class ServiceAssistantModule { }