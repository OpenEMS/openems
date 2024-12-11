import { NgModule } from "@angular/core";
import { SharedModule } from "./../shared/shared.module";
import { UserComponent } from "./user.component";

@NgModule({
  imports: [
    UserComponent,
    SharedModule,
  ],
})
export class UserModule { }
