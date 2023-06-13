import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { Generic_ComponentsModule } from "../../genericComponents/genericComponents";
import { PipeModule } from "../../pipe/pipe";
import { ElectricityMeterComponent } from "./electricity/modal.component";
import { EssChargerComponent } from "./esscharger/modal.component";


@NgModule({
    imports: [
        BrowserModule,
        IonicModule,
        PipeModule,
        Generic_ComponentsModule
    ],
    entryComponents: [
    ],
    declarations: [
        ElectricityMeterComponent,
        EssChargerComponent
    ],
    exports: [
        ElectricityMeterComponent,
        EssChargerComponent
    ]
})
export class MeterModule { }
