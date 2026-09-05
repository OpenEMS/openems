import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { commonRoutes } from "src/app/shared/components/navigation/navigation-routing.module";
import { hasEdgeRole } from "src/app/shared/guards/functional-guards";
import { Role } from "src/app/shared/type/role";
import { environment } from "src/environments";
import { EmptyPageComponent } from "../../../shared/components/navigation/empty-page/empty-page";
import { HistoryExcelExportComponent } from "../../history/common/energy/export/export";
import { HistoryChartComponent } from "../../history/common/energy/new-navigation/new-navigation";

export const energyJourneyRoutes: Routes = [
    { path: "", component: EmptyPageComponent, pathMatch: "full" },
    ...commonRoutes,
    {
        path: "history",
        component: HistoryChartComponent,
    },
    {
        path: "history/export",
        component: HistoryExcelExportComponent,
    },
    {
        path: "appcenter",
        loadComponent: () => import("src/app/edge/settings/app/index.component").then((m) => m.IndexComponent),
        canActivate: [hasEdgeRole(Role.OWNER)],
        data: { navbarTitle: environment.edgeShortName + " Apps" },
    },
    {
        path: "appcenter/single/install",
        loadComponent: () => import("src/app/edge/settings/app/install.component").then((m) => m.InstallAppComponent),
        canActivate: [hasEdgeRole(Role.OWNER)],
    },
    {
        path: "appcenter/single/update",
        loadComponent: () => import("src/app/edge/settings/app/update.component").then((m) => m.UpdateAppComponent),
        runGuardsAndResolvers: "always",
        canActivate: [hasEdgeRole(Role.OWNER)],
    },
    {
        path: "appcenter/single",
        loadComponent: () => import("src/app/edge/settings/app/single.component").then((m) => m.SingleAppComponent),
        canActivate: [hasEdgeRole(Role.OWNER)],
    },
    {
        path: "appcenter/oauth",
        data: { navbarTitle: "OAuth" },
        loadComponent: () =>
            import("src/app/edge/settings/app/oauth/oauth.component").then((m) => m.OAuthIndexComponent),
        canActivate: [hasEdgeRole(Role.ADMIN)],
    },
];

@NgModule({
    imports: [RouterModule.forChild(energyJourneyRoutes)],
    exports: [RouterModule],
})
export class EnergyJourneyRoutingModule {}
