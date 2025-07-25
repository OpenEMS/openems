// @ts-strict-ignore
import { formatNumber } from "@angular/common";
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { Icon } from "src/app/shared/type/widget";
import { Controller_ChpCostOptimizationModalComponent } from "./modal/modal.component";

@Component({
    selector: "Controller_ChpCostOptimizationComponent",
    templateUrl: "./ChpCostOptimization.html",
    standalone: false,
})
export class Controller_ChpCostOptimizationComponent extends AbstractFlatWidget {

    private static PROPERTY_MODE: string = "_PropertyMode";
    public propertyModeChannel: ChannelAddress | null = null;
    public modeChannelValue: string;
    public mode: string;

    public stateChannelValue: string;
    public state: string;
    public propertyHighCostsThreshold: number;

    public currentEnergyCostsWithoutChp: number;
    public currentEnergyCosts: number;
    public activePowerTarget: number;

    public icon: Icon = {
        name: "",
        size: "large",
        color: "primary",
    };

    protected meta: EdgeConfig.Component = null;
    protected currency: string = null;
    protected currencyLabel: null;

    protected priceWithCurrency: string = "-";
    protected locale: string = "-";

    get currentEnergyCostsWithoutChpInCtPerKWh(): number | null {
        if (this.currentEnergyCostsWithoutChp == null) { return null; }
        return +(this.currentEnergyCostsWithoutChp).toFixed(2);
    }
    get currentEnergyCostsWithLabel(): string {
        if (this.currentEnergyCosts == null) { return "-"; }
        return formatNumber(this.currentEnergyCosts, this.locale, "1.0-" + 2) + " " + this.currency + "/h";
    }

    get currentEnergyCostsWithoutChpWithLabel(): string {
        if (this.currentEnergyCostsWithoutChp == null) { return "-"; }
        return formatNumber(this.currentEnergyCostsWithoutChp, this.locale, "1.0-" + 2) + " " + this.currency + "/h";
    }

    get thresholdCostsWithLabel(): string {
        if (this.propertyHighCostsThreshold == null) { return "-"; }
        return formatNumber(this.propertyHighCostsThreshold, this.locale, "1.0-" + 2) + " " + this.currency + "/h";
    }

    get currentEnergyCostsPercent(): number {
        if (!this.propertyHighCostsThreshold || this.propertyHighCostsThreshold === 0) {
            return 0;
        }
        return Math.round((this.currentEnergyCosts / this.propertyHighCostsThreshold) * 100);
    }
    get barColor(): string {
        const p = this.currentEnergyCostsPercent;
        if (p > 90) { return "danger"; }
        if (p < 0) { return "success"; }
        return "warning";
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: Controller_ChpCostOptimizationModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,

            },
        });
        return await modal.present();
    }


    protected override getChannelAddresses() {
        this.propertyModeChannel = new ChannelAddress(this.component.id, Controller_ChpCostOptimizationComponent.PROPERTY_MODE);
        return [
            new ChannelAddress(this.component.id, "EnergyCosts"),
            new ChannelAddress(this.component.id, "EnergyCostsWithoutChp"),
            new ChannelAddress(this.component.id, "StateMachine"),
            new ChannelAddress(this.component.id, "AwaitingStartHysteresis"),
            new ChannelAddress(this.component.id, "AwaitingStopHysteresis"),
            new ChannelAddress(this.component.id, "AwaitingTransitionHysteresis"),
            new ChannelAddress(this.component.id, "AwaitingDeviceHysteresis"),
            new ChannelAddress(this.component.id, "ActivePowerTarget"),
            new ChannelAddress(this.component.id, "ChpActivePower"),
            new ChannelAddress(this.component.id, "_PropertyMaxCost"),
            this.propertyModeChannel,
        ];
    }


    protected override onCurrentData(currentData: CurrentData) {
        //console.log("[DEBUG] Channels für", this.component.id, ":", Object.keys(currentData.allComponents));

        // Mode
        this.modeChannelValue = currentData.allComponents[this.propertyModeChannel.toString()];
        switch (this.modeChannelValue) {
            case "MANUAL_ON":
                this.mode = this.translate.instant("General.on");
                break;
            case "MANUAL_OFF":
                this.mode = this.translate.instant("General.off");
                break;
            case "AUTOMATIC":
                this.mode = this.translate.instant("General.automatic");
        }

        // StateMachine
        this.stateChannelValue = currentData.allComponents[this.componentId + "/StateMachine"].toString();

        this.meta = this.config?.getComponent("_meta");
        this.currency = this.config?.getPropertyFromComponent<string>(this.meta, "currency") ?? "€";
        this.locale = (Language.getByKey(localStorage.LANGUAGE) ?? Language.DEFAULT).i18nLocaleKey;

        this.currentEnergyCostsWithoutChp = currentData.allComponents[this.componentId + "/EnergyCostsWithoutChp"];
        this.currentEnergyCosts = currentData.allComponents[this.componentId + "/EnergyCosts"];
        this.propertyHighCostsThreshold = Number(this.component.properties["maxCost"]);

        this.state = this.translateState(this.stateChannelValue);
        this.activePowerTarget = currentData.allComponents[this.componentId + "/ActivePowerTarget"];
    }

    private translateState(stateChannelValue: string): string {
        switch (stateChannelValue) {
            case "-1":
                return this.translate.instant("Edge.Index.Widgets.CHP.CHP_STATE.UNDEFINED");
            case "0":
                return this.translate.instant("Edge.Index.Widgets.CHP.CHP_STATE.IDLE");
            case "1":
                return this.translate.instant("Edge.Index.Widgets.CHP.CHP_STATE.ERROR");
            case "2":
                return this.translate.instant("Edge.Index.Widgets.CHP.CHP_STATE.CHP_ACTIVE");
            case "3":
                return this.translate.instant("Edge.Index.Widgets.CHP.CHP_STATE.CHP_INACTIVE");
            default:
                return "-";
        }
    }
}
