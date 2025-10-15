import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";

@NgModule({
    imports: [
        CommonModule,
        IonicModule,
        TranslateModule,
    ],
    exports: [
        CommonModule,
        IonicModule,
        TranslateModule,
    ],
})
export class CommonUiModule { }
