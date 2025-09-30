import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { ChangelogViewComponent } from "./view/view";

const routes: Routes = [
    {
        path: "",
        component: ChangelogViewComponent,
    },
];
@NgModule({
    imports: [ROUTER_MODULE.FOR_CHILD(routes)],
    exports: [RouterModule],
})
export class ChangelogRoutingModule { }
