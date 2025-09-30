import { NgModule } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { SharedModule } from "./../shared/SHARED.MODULE";
import { ThemePopoverComponent } from "./theme-selection-popup/theme-selection-popover";
import { UserComponent } from "./USER.COMPONENT";

@NgModule({
  imports: [
    SharedModule,
    IonicModule,
  ],
  declarations: [
    UserComponent,
    ThemePopoverComponent,
  ],
  exports: [
    ThemePopoverComponent,
  ],
})
export class UserModule { }
