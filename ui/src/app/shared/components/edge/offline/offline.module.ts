import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { OfflineComponent } from "./offline.component";
import { ManualComponent } from "./producttype/default";

@NgModule({
    imports: [
        BrowserModule,
        IonicModule,
        TranslateModule,
    ],
    declarations: [
        OfflineComponent,
    ],
    exports: [
        ManualComponent,
        OfflineComponent,
    ],
})
export class EdgeOfflineModule { }
