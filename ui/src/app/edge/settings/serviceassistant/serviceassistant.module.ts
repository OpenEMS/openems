import { NgModule } from "@angular/core";
import { SharedModule } from "src/app/shared/shared.module";
import { ServiceAssistantComponent } from "./serviceassistant.component";
import { SoltaroCellChartComponent } from "./soltarosingle/cell-chart/chart.component";
import { SoltaroVersionB } from "./soltarosingle/soltaroB.component";
import { SoltaroVersionC } from "./soltarosingle/soltaroC.component";

@NgModule({
    imports: [
        SharedModule,
    ],
    entryComponents: [
    ],
    declarations: [
        ServiceAssistantComponent,
        SoltaroCellChartComponent,
        SoltaroVersionB,
        SoltaroVersionC
    ],
    exports: [ServiceAssistantComponent]
})
export class ServiceAssistantModule { }