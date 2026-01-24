import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { HistoryDataErrorComponent } from "./history-data-error.component";


@NgModule({
    imports: [
        CommonModule,
        IonicModule,
        TranslateModule,
    ],
    declarations: [
        HistoryDataErrorComponent,
    ],
    exports: [
        HistoryDataErrorComponent,
    ],
})

export class HistoryDataErrorModule { }
