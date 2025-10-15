import { NgModule } from "@angular/core";
import { FlatWidgetButtonComponent } from "../shared/components/flat/flat-widget-button/flat-widget-button";
import { SharedModule } from "./../shared/shared.module";
import { FilterComponent } from "./filter/filter.component";
import { LoginComponent } from "./login.component";
import { OverViewComponent } from "./overview/overview.component";
import { RegistrationModule } from "./registration/registration.module";
import { LoadingScreenComponent } from "./shared/loading-screen";
import { SumStateComponent } from "./shared/sumState";

@NgModule({
  imports: [
    SharedModule,
    RegistrationModule,
    FlatWidgetButtonComponent,
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
