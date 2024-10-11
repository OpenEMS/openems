import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { OfflineComponent } from "./offline.component";
import { DefaultOfflineComponent } from "./producttype/default";
import { Home10OfflineComponent } from "./producttype/home_10";
import { Home20_30OfflineComponent } from "./producttype/home_20_30";


@NgModule({
    imports: [
        BrowserModule,
        IonicModule,
    ],
    declarations: [
        OfflineComponent,
        Home10OfflineComponent,
        Home20_30OfflineComponent,
        DefaultOfflineComponent,
    ],
    exports: [
        OfflineComponent,
        Home10OfflineComponent,
        Home20_30OfflineComponent,
        DefaultOfflineComponent,
    ],
})
export class EdgeOfflineModule { }
