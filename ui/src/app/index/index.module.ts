import { NgModule } from "@angular/core";
import { RegistrationModule } from "../registration/registration.module";
import { SharedModule } from "./../shared/shared.module";
import { FilterComponent } from "./filter/filter.component";
import { LoginComponent } from "./login.component";
import { OverViewComponent } from "./overview/overview.component";
import { LoadingScreenComponent } from "./shared/loading-screen";
import { SumStateComponent } from "./shared/sumState";

@NgModule({
  imports: [
    SharedModule,
    RegistrationModule,
  ],
  declarations: [
    FilterComponent,
    SumStateComponent,
    LoginComponent,
    OverViewComponent,
    LoadingScreenComponent,
  ],
})
export class IndexModule { }
