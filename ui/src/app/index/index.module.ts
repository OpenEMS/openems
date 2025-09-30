import { NgModule } from "@angular/core";
import { SharedModule } from "./../shared/SHARED.MODULE";
import { FilterComponent } from "./filter/FILTER.COMPONENT";
import { LoginComponent } from "./LOGIN.COMPONENT";
import { OverViewComponent } from "./overview/OVERVIEW.COMPONENT";
import { RegistrationModule } from "./registration/REGISTRATION.MODULE";
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
