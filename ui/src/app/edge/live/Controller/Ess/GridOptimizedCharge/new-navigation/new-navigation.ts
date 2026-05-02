import { Component, inject } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../../livedataservice";
import { CONVERT_GRID_OPTIMIZED_CHARGE_STATE, SharedGridOptimizedCharge } from "../shared/shared";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class ControllerEssGridOptimizedChargeHomeComponent extends AbstractFormlyComponent {

    public component: EdgeConfig.Component | null = null;
    public mode: Mode | null = null;
    public state: string | null = null;
    public isSellToGridLimitAvoided: boolean = false;
    public sellToGridLimitMinimumChargeLimit: number | null = null;
    public delayChargeMaximumChargeLimit: number | null = null;

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private routeService: RouteService = inject(RouteService);

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        mode: Mode | null,
        isSellToGridLimitAvoided: boolean,
        delayChargeMaximumChargeLimit: number | null,
        sellToGridLimitMinimumChargeLimit: number | null): OeFormlyView {
        const lines: OeFormlyField[] = [];

        lines.push({
            type: "channel-line",
            name: translate.instant("GENERAL.MODE"),
            channel: component.id + "/_PropertyMode",
            converter: Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(translate),
        });

        if (mode == Mode.OFF) {
            return {
                title: component.alias,
                helpKey: "REDIRECT.CONTROLLER_ESS_GRID_OPTIMIZED_CHARGE",
                icon: { name: "oe-grid-storage", color: "dark", size: "large" },
                lines: lines,
                component: new EdgeConfig.Component(),
            };
        }

        if (isSellToGridLimitAvoided == true) {
            lines.push({
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.GRID_FEED_IN_LIMITATION_IS_AVOIDED"),
            });
            if (sellToGridLimitMinimumChargeLimit != null && sellToGridLimitMinimumChargeLimit > 0) {
                lines.push({
                    type: "channel-line",
                    name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.MINIMUM_CHARGE"),
                    channel: component.id + "/SellToGridLimitMinimumChargeLimit",
                    converter: Utils.CONVERT_WATT_TO_KILOWATT,
                });
            }
        } else {
            lines.push({
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.MINIMUM_CHARGE"),
                channel: component.id + "/DelayChargeState",
                converter: CONVERT_GRID_OPTIMIZED_CHARGE_STATE(translate),
            });
            if (delayChargeMaximumChargeLimit != null && delayChargeMaximumChargeLimit > 0) {
                lines.push({
                    type: "channel-line",
                    name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.MINIMUM_CHARGE"),
                    channel: component.id + "/delayChargeMaximumChargeLimit",
                    converter: Utils.CONVERT_WATT_TO_KILOWATT,
                });
            }
        }

        return {
            title: component.alias,
            helpKey: "REDIRECT.CONTROLLER_ESS_GRID_OPTIMIZED_CHARGE",
            icon: { name: "oe-grid-storage", color: "dark", size: "large" },
            lines: lines,
            component: new EdgeConfig.Component(),
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        this.component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(this.component);

        return ControllerEssGridOptimizedChargeHomeComponent.generateView(this.translate, this.component, this.mode, this.isSellToGridLimitAvoided, this.delayChargeMaximumChargeLimit, this.sellToGridLimitMinimumChargeLimit);
    }

    protected override onCurrentData(currentData: CurrentData) {
        if (this.component == null) {
            return;
        }
        const delayChargeState = currentData.allComponents[this.component.id + "/DelayChargeState"];

        this.mode = currentData.allComponents[this.component.id + "/_PropertyMode"];

        // Check if Grid feed in limitation is avoided
        if (currentData.allComponents[this.component.id + "/SellToGridLimitState"] == 0 ||
            (currentData.allComponents[this.component.id + "/SellToGridLimitState"] == 3
                && currentData.allComponents[this.component.id + "/DelayChargeState"] != 0
                && currentData.allComponents[this.component.id + "/SellToGridLimitMinimumChargeLimit"] > 0)) {
            this.isSellToGridLimitAvoided = true;
        }

        this.sellToGridLimitMinimumChargeLimit = currentData.allComponents[this.component.id + "/SellToGridLimitMinimumChargeLimit"];

        this.state = this.getDelayChargeStateLabel(delayChargeState);

        this.delayChargeMaximumChargeLimit = currentData.allComponents[this.component.id + "/DelayChargeMaximumChargeLimit"];
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedGridOptimizedCharge.getChannelAddresses(this.service, this.routeService, this.component);
    }

    private getDelayChargeStateLabel(delayChargeState: number): string {
        switch (delayChargeState) {
            case -1:
                return this.translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.NOT_DEFINED");
            case 0:
                return this.translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.CHARGE_LIMIT_ACTIVE");
            case 1:
                return this.translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.PASSED_END_TIME");
            case 2:
                return this.translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.STORAGE_ALREADY_FULL");
            case 3:
                return this.translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.END_TIME_NOT_CALCULATED");
            case 4:
                return this.translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.NO_LIMIT_POSSIBLE");
            case 5: // Case 6: 'DISABLED' hides 'state-line', so no Message needed
            case 7:
            case 9:
                return this.translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.NO_LIMIT_ACTIVE");
            case 8:
                return this.translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.CHARGING_DELAYED");
            default:
                return this.translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.NOT_DEFINED");
        }
    }
}
