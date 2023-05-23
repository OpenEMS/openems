import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { Generic_ComponentsModule } from "../../genericComponents/genericComponents";
import { PipeModule } from "../../pipe/pipe";
import { AsymmetricMeterComponent } from "./asymmetric/modal.component";
import { EssChargerComponent } from "./esscharger/modal.component";
import { SymmetricMeterComponent } from "./symmetric/modal.component";


@NgModule({
    imports: [
        BrowserModule,
        IonicModule,
        PipeModule,
        Generic_ComponentsModule
    ],
    declarations: [
        AsymmetricMeterComponent,
        EssChargerComponent,
        SymmetricMeterComponent,
    ],
    exports: [
        AsymmetricMeterComponent,
        EssChargerComponent,
        SymmetricMeterComponent,
    ]
})
export class MeterModule { }
