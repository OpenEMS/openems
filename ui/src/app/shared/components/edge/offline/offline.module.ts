import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { OfflineComponent } from "./offline.component";

@NgModule({
    imports: [
        BrowserModule,
        IonicModule,
    ],
    declarations: [
        OfflineComponent,
    ],
    exports: [
        OfflineComponent,
    ],
})
export class EdgeOfflineModule { }
