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
import { BatteryExtensionComponent } from "./energy-journey/pages/battery-extension/battery-extension";
import { ElectricityPriceSelectionComponent } from "./energy-journey/pages/electricity-price-choice/electricity-price-selection";
import { JsonrpcTestComponent } from "./jsonrpctest/jsonrpctest";
import { NetworkOldComponent as EdgeSettingsNetworkOldComponent } from "./network.old/network.old.component";
import { SettingsComponent } from "./settings.component";
import { SystemExecuteComponent as EdgeSettingsSystemExecuteComponent } from "./systemexecute/systemexecute.component";
import { SystemUpdateOldComponent as EdgeSettingsSystemUpdateOldComponent } from "./systemupdate.old/systemupdate.old.component";

export const settingsRoutes: Routes = [
    { path: "", data: { navbarTitleToBeTranslated: "Menu.edgeSettings" }, component: SettingsComponent },
    { path: "industrialLAssistant", loadComponent: () => import("./assistant/industrial/industrial").then(m => m.IndustrialServiceAssistantComponent), data: { navbarTitle: "Industrial L-Assistent" } },
    { path: "channels", component: EdgeSettingsChannelsComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitle: "Channels" } },
    { path: "component.install", component: EdgeSettingsComponentInstallIndexComponentComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.addComponents" } },
    { path: "component.install/:factoryId", component: EdgeSettingsComponentInstallComponentComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.addComponents" } },
    { path: "component.update", component: EdgeSettingsComponentUpdateIndexComponentComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.adjustComponents" } },
    { path: "component.update/:componentId", component: EdgeSettingsComponentUpdateComponentComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.adjustComponents" } },
    { path: "network", loadComponent: () => import("./network/network.component").then(m => m.NetworkComponent), canActivate: [hasEdgeRole(Role.INSTALLER)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.networkConfiguration" } },
    { path: "network.old", component: EdgeSettingsNetworkOldComponent, data: { navbarTitle: "Netzwerk Konfiguration" } },
    { path: "profile", loadComponent: () => import("./profile/profile.component").then(m => m.ProfileComponent), data: { navbarTitleToBeTranslated: "Edge.Config.Index.systemProfile" } },
    { path: "profile/:componentId", loadComponent: () => import("./profile/aliasupdate.component").then(m => m.AliasUpdateComponent), data: { navbarTitleToBeTranslated: "Edge.Config.Index.renameComponents" } },
    { path: "serviceAssistant", loadComponent: () => import("./serviceassistant/serviceassistant.component").then(m => m.ServiceAssistantComponent), data: { navbarTitle: "Service Assistant" } },
    { path: "systemexecute", component: EdgeSettingsSystemExecuteComponent, canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.systemExecute" } },
    { path: "systemlog", loadComponent: () => import("./systemlog/systemlog.component").then(m => m.SystemLogComponent), canActivate: [hasEdgeRole(Role.OWNER)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.liveLog" } },
    { path: "system", loadComponent: () => import("./system/system.component").then(m => m.SystemComponent), canActivate: [hasEdgeRole(Role.OWNER)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.SYSTEM" } },
    { path: "system.old", component: EdgeSettingsSystemUpdateOldComponent },
    { path: "app", loadComponent: () => import("./app/index.component").then(m => m.IndexComponent), canActivate: [hasEdgeRole(Role.OWNER)], data: { navbarTitle: environment.edgeShortName + " Apps" } },
    { path: "app/install/:appId", loadComponent: () => import("./app/install.component").then(m => m.InstallAppComponent), canActivate: [hasEdgeRole(Role.OWNER)] },
    { path: "app/update/:appId", loadComponent: () => import("./app/update.component").then(m => m.UpdateAppComponent), canActivate: [hasEdgeRole(Role.OWNER)] },
    { path: "app/single/:appId", loadComponent: () => import("./app/single.component").then(m => m.SingleAppComponent), canActivate: [hasEdgeRole(Role.OWNER)] },
    { path: "app/oauth", data: { navbarTitle: "OAuth" }, loadComponent: () => import("./app/oauth/oauth.component").then(m => m.OAuthIndexComponent), canActivate: [hasEdgeRole(Role.ADMIN)] },
    { path: "energyJourney", loadComponent: () => import("./energy-journey/energy-journey").then(m => m.EnergyJourneyComponent), data: { navbarTitle: "Energy Journey" } },
    { path: "energyJourney/batteryExtension", component: BatteryExtensionComponent, data: { navbarTitleToBeTranslated: "SETTINGS.ENERGY_JOURNEY.ANALYSE_BATTERY_EXTENSION_TITLE" } },
    { path: "energyJourney/batteryExtension/electricityPriceChoice", component: ElectricityPriceSelectionComponent, data: { navbarTitleToBeTranslated: "SETTINGS.ENERGY_JOURNEY.ANALYSE_BATTERY_EXTENSION_TITLE" } },
    { path: "alerting", loadChildren: () => import("./alerting/alerting.module").then(m => m.AlertingModule), canActivate: [hasEdgeRole(Role.OWNER)], data: { navbarTitleToBeTranslated: "Edge.Config.Index.alerting" } },
    { path: "homeServiceAssistant", loadComponent: () => import("./assistant/home/home").then(m => m.HomeServiceAssistantComponent), data: { navbarTitle: "Home-Assistent" } },
    { path: "jsonrpctest", component: JsonrpcTestComponent, data: { navbarTitle: "Jsonrpc Test" } },
    { path: "powerAssistant", loadComponent: () => import("./assistant/powerassistant/powerassistant").then(m => m.PowerAssistantComponent), canActivate: [hasEdgeRole(Role.ADMIN)], data: { navbarTitle: "Power-Assistant" } },
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

