import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { HistoryDataErrorComponent } from "./history-data-error.component";


@NgModule({
  imports: [
    BrowserAnimationsModule,
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
