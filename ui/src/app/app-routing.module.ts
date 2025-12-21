import { inject, NgModule } from "@angular/core";
import { NoPreloading, RedirectFunction, RouterModule, Routes } from "@angular/router";
import { CookieService } from "ngx-cookie-service";
import { environment } from "src/environments";
import { EdgeComponent } from "./edge/edge.component";
import { DetailsOverviewComponent } from "./edge/history/common/production/details/details.overview";
import { OverviewComponent as ProductionChartOverviewComponent } from "./edge/history/common/production/overview/overview";
import { OverviewComponent as ChannelthresholdChartOverviewComponent } from "./edge/history/Controller/ChannelThreshold/overview/overview";
import { OverviewComponent as EnerixOverviewComponent } from "./edge/history/Controller/EnerixControl/overview/overview";
import { OverviewComponent as GridOptimizedChargeChartOverviewComponent } from "./edge/history/Controller/Ess/GridoptimizedCharge/overview/overview";
import { OverviewComponent as TimeOfUseTariffOverviewComponent } from "./edge/history/Controller/Ess/TimeOfUseTariff/overview/overview";
import { OverviewComponent as HeatchartOverviewComponent, OverviewComponent as HeatmypvchartOverviewComponent } from "./edge/history/Controller/Heat/overview/overview";
import { DetailsOverviewComponent as DigitalOutputDetailsOverviewComponent } from "./edge/history/Controller/Io/DigitalOutput/details/details.overview";
import { OverviewComponent as DigitalOutputChartOverviewComponent } from "./edge/history/Controller/Io/DigitalOutput/overview/overview";
import { OverviewComponent as HeatingelementChartOverviewComponent } from "./edge/history/Controller/Io/heatingelement/overview/overview";
import { OverviewComponent as ModbusTcpApiOverviewComponent } from "./edge/history/Controller/ModbusTcpApi/overview/overview";
import { OverviewComponent as AsymmetricPeakshavingChartOverviewComponent } from "./edge/history/Controller/peak-shaving/asymmetric/overview/overview";
import { OverviewComponent as SymmetricPeakshavingChartOverviewComponent } from "./edge/history/Controller/peak-shaving/symmetric/overview/overview";
import { OverviewComponent as TimeslotPeakshavingChartOverviewComponent } from "./edge/history/Controller/peak-shaving/timeslot/overview/overview";
import { DelayedSellToGridChartOverviewComponent } from "./edge/history/delayedselltogrid/symmetricpeakshavingchartoverview/delayedselltogridchartoverview.component";
import { HistoryComponent as EdgeHistoryComponent } from "./edge/history/history.component";
import { HistoryDataService } from "./edge/history/historydataservice";
import { HistoryParentComponent } from "./edge/history/historyparent.component";
import { OverviewComponent as CommonAutarchyHistoryOverviewComponent } from "./edge/live/common/autarchy/history/overview/overview";
import { CommonConsumptionHistoryOverviewComponent } from "./edge/live/common/consumption/history/overview/overview";
import { CommonConsumptionDetailsOverviewComponent } from "./edge/live/common/consumption/history/phase-accurate/overview/overview";
import { CommonGridDetailsExternalLimitationOverviewComponent } from "./edge/live/common/grid/history/details/external-limitation/overview/details.overview";
import { CommonGridDetailsPhaseAccurateOverviewComponent } from "./edge/live/common/grid/history/details/phase-accurate/overview/details.overview";
import { CommonGridOverviewComponent } from "./edge/live/common/grid/history/overview/overview";
import { OverviewComponent as SelfconsumptionChartOverviewComponent } from "./edge/live/common/selfconsumption/history/overview/overview";
import { LiveDataService } from "./edge/live/livedataservice";
import { LoginComponent } from "./index/login.component";
import { OverViewComponent } from "./index/overview/overview.component";
import { LoadingScreenComponent } from "./index/shared/loading-screen";
import { CurrentAndVoltageOverviewComponent } from "./shared/components/edge/meter/currentVoltage/overview/currentVoltage.overview";
import { DataService } from "./shared/components/shared/dataservice";
import { UserComponent } from "./user/user.component";

export const history: (/** Determines if titles in headers can be set */ customHeaders: boolean) => Routes = (customHeaders) => [{
    path: "history", providers: [{
        useClass: HistoryDataService,
        provide: DataService,
    }],
    component: HistoryParentComponent, children: [
        { path: "", component: EdgeHistoryComponent, data: { ...(customHeaders ? { navbarTitleToBeTranslated: "GENERAL.HISTORY" } : {}) } },
        // History Chart Pages
        { path: ":componentId/asymmetricpeakshavingchart", component: AsymmetricPeakshavingChartOverviewComponent },
        { path: ":componentId/delayedselltogridchart", component: DelayedSellToGridChartOverviewComponent },
        { path: ":componentId/gridOptimizedChargeChart", component: GridOptimizedChargeChartOverviewComponent },
        { path: ":componentId/heatingelementchart", component: HeatingelementChartOverviewComponent },
        { path: ":componentId/heatmypvchart", component: HeatmypvchartOverviewComponent },
        { path: ":componentId/heatchart", component: HeatchartOverviewComponent },
        { path: ":componentId/enerixchart", component: EnerixOverviewComponent },
        { path: ":componentId/heatpumpchart", loadChildren: () => import("./edge/history/Controller/Io/heatpump/heat-pump.module").then(m => m.HeatPumpModule) },
        { path: ":componentId/modbusTcpApi", component: ModbusTcpApiOverviewComponent },
        { path: ":componentId/scheduleChart", component: TimeOfUseTariffOverviewComponent },
        { path: ":componentId/symmetricpeakshavingchart", component: SymmetricPeakshavingChartOverviewComponent },
        { path: ":componentId/timeslotpeakshavingchart", component: TimeslotPeakshavingChartOverviewComponent },
        { path: "autarchychart", component: CommonAutarchyHistoryOverviewComponent },
        { path: "consumptionchart", component: CommonConsumptionHistoryOverviewComponent },
        { path: "consumptionchart/:componentId", component: CommonConsumptionDetailsOverviewComponent },
        { path: "consumptionchart/:componentId/currentVoltage", component: CurrentAndVoltageOverviewComponent },
        { path: "gridchart", component: CommonGridOverviewComponent },
        { path: "gridchart/externalLimitation", component: CommonGridDetailsExternalLimitationOverviewComponent },
        { path: "gridchart/:componentId", component: CommonGridDetailsPhaseAccurateOverviewComponent },
        { path: "gridchart/:componentId/currentVoltage", component: CurrentAndVoltageOverviewComponent },
        { path: "productionchart", component: ProductionChartOverviewComponent },
        { path: "productionchart/:componentId", component: DetailsOverviewComponent },
        { path: "productionchart/:componentId/currentVoltage", component: CurrentAndVoltageOverviewComponent },
        { path: "selfconsumptionchart", component: SelfconsumptionChartOverviewComponent },
        { path: "storagechart", loadChildren: () => import("./edge/history/common/storage/storage").then(m => m.CommonStorage) },

        // Controllers
        { path: "channelthresholdchart", component: ChannelthresholdChartOverviewComponent },
        { path: "digitaloutputchart", component: DigitalOutputChartOverviewComponent },
        { path: "digitaloutputchart/:componentId", component: DigitalOutputDetailsOverviewComponent },
    ],
}];

export const routes: Routes = [

    // TODO should be removed in the future
    { path: "", redirectTo: oauthRedirectFunction("index"), pathMatch: "full" },
    { path: "index", component: LoadingScreenComponent },
    { path: "login", component: LoginComponent, data: { navbarTitle: environment.uiTitle } },

    { path: "overview", component: OverViewComponent },

    { path: "user", component: UserComponent, data: { navbarTitleToBeTranslated: "MENU.USER" } },
    { path: "changelog", loadChildren: () => import("./changelog/changelog.module").then(m => m.ChangelogModule), data: { navbarTitleToBeTranslated: "MENU.CHANGELOG" } },

    // Edge Pages
    {
        path: "device/:edgeId", component: EdgeComponent, children: [
            { path: "", redirectTo: "live", pathMatch: "full" },
            {
                path: "live", data: { navbarTitle: environment.uiTitle }, providers: [{
                    useClass: LiveDataService,
                    provide: DataService,
                }], loadChildren: () => import("./shared/components/navigation/navigation-routing.module").then(m => m.NavigationRoutingModule),
            },
            ...history(false),
            { path: "settings", loadChildren: () => import("./edge/settings/settings-routing.module").then(m => m.SettingsRoutingModule) },
        ],
    },

    { path: "demo", component: LoginComponent },
    // Fallback
    { path: "**", pathMatch: "full", redirectTo: "index" },
];

export const appRoutingProviders: any[] = [];

@NgModule({
    imports: [
        RouterModule.forRoot(routes, { preloadingStrategy: NoPreloading, paramsInheritanceStrategy: "always" }),
    ],
    exports: [RouterModule],
})
export class AppRoutingModule { }

/**
 * Creates a RedirectFunction, which checks for a state parameter
 * in the query parameters and also the active oauth state if both
 * are present navigates to the active oauth state.
 *
 * @param defaultRoute the default route to navigate to if no oauth state is present
 * @returns the created RedirectFunction
 */
function oauthRedirectFunction(defaultRoute: string): RedirectFunction {
    return redirectData => {
        const state = redirectData.queryParams["state"] as string | undefined;
        if (!state) {
            return defaultRoute;
        }

        const cookieService = inject(CookieService);
        const oauthRedirectStateRaw = cookieService.get("oauthredirectstate");
        if (!oauthRedirectStateRaw) {
            return defaultRoute;
        }

        const queryParamsString = Object.entries(redirectData.queryParams)
            .map(([key, value]) => key + "=" + value).join("&");

        const oauthRedirectState = JSON.parse(oauthRedirectStateRaw) as { href: string };
        return oauthRedirectState.href + "?" + queryParamsString;
    };
}
