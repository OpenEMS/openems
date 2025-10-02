import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { hasEdgeRole } from "src/app/shared/guards/functional-guards";
import { Role } from "src/app/shared/type/role";
import { environment } from "src/themes/openems/environments/edge-dev";
import { ChannelsComponent as EdgeSettingsChannelsComponent } from "./channels/channels.component";
import { IndexComponent as EdgeSettingsComponentInstallIndexComponentComponent } from "./component/install/index.component";
import { ComponentInstallComponent as EdgeSettingsComponentInstallComponentComponent } from "./component/install/install.component";
import { IndexComponent as EdgeSettingsComponentUpdateIndexComponentComponent } from "./component/update/index.component";
import { ComponentUpdateComponent as EdgeSettingsComponentUpdateComponentComponent } from "./component/update/update.component";
import { JsonrpcTestComponent } from "./jsonrpctest/jsonrpctest";
import { SettingsComponent } from "./settings.component";
import { SystemExecuteComponent as EdgeSettingsSystemExecuteComponent } from "./systemexecute/systemexecute.component";

export const settingsRoutes: Routes = [
    { path: "", data: { navbarTitleToBeTranslated: "Menu.edgeSettings" }, component: SettingsComponent },
    { path: "channels", component: EdgeSettingsChannelsComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitle: "Channels" } },
    { path: "component.install", component: EdgeSettingsComponentInstallIndexComponentComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.addComponents" } },
    { path: "component.install/:factoryId", component: EdgeSettingsComponentInstallComponentComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.addComponents" } },
    { path: "component.update", component: EdgeSettingsComponentUpdateIndexComponentComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.adjustComponents" } },
    { path: "component.update/:componentId", component: EdgeSettingsComponentUpdateComponentComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.adjustComponents" } },
    { path: "network", loadComponent: () => import("./network/network.component").then(m => m.NetworkComponent), canActivate: [hasEdgeRole(Role.INSTALLER)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.networkConfiguration" } },
    { path: "profile", loadComponent: () => import("./profile/profile.component").then(m => m.ProfileComponent), data: { navbarTitleToBeTranslated: "Edge.Config.Index.systemProfile" } },
    { path: "profile/:componentId", loadComponent: () => import("./profile/aliasupdate.component").then(m => m.AliasUpdateComponent), data: { navbarTitleToBeTranslated: "Edge.Config.Index.renameComponents" } },
    { path: "systemexecute", component: EdgeSettingsSystemExecuteComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.systemExecute" } },
    { path: "systemlog", loadComponent: () => import("./systemlog/systemlog.component").then(m => m.SystemLogComponent), canActivate: [hasEdgeRole(Role.OWNER)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.liveLog" } },
    { path: "system", loadComponent: () => import("./system/system.component").then(m => m.SystemComponent), canActivate: [hasEdgeRole(Role.OWNER)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.SYSTEM" } },
    { path: "app", loadComponent: () => import("./app/index.component").then(m => m.IndexComponent), canActivate: [hasEdgeRole(Role.OWNER)], data: { navbarTitle: environment.edgeShortName + " Apps" } },
    { path: "app/install/:appId", loadComponent: () => import("./app/install.component").then(m => m.InstallAppComponent), canActivate: [hasEdgeRole(Role.OWNER)] },
    { path: "app/update/:appId", loadComponent: () => import("./app/update.component").then(m => m.UpdateAppComponent), canActivate: [hasEdgeRole(Role.OWNER)] },
    { path: "app/single/:appId", loadComponent: () => import("./app/single.component").then(m => m.SingleAppComponent), canActivate: [hasEdgeRole(Role.OWNER)] },
    { path: "alerting", loadChildren: () => import("./alerting/alerting.module").then(m => m.AlertingModule), canActivate: [hasEdgeRole(Role.OWNER)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.alerting" } },
    { path: "jsonrpctest", component: JsonrpcTestComponent, data: { navbarTitle: "Jsonrpc Test" } },
];

@NgModule({
    imports: [
        RouterModule.forChild(settingsRoutes),
    ],
    exports: [
        RouterModule,
    ],
})
export class SettingsRoutingModule { }

