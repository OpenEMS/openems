import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { OfflineComponent } from "./offline.component";

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
        OfflineComponent,
    ],
})
export class EdgeOfflineModule { }
