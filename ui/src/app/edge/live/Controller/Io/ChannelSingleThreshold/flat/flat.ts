// @ts-strict-ignore
import { Component, signal, WritableSignal } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { Filter } from "src/app/shared/components/shared/filter";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { CurrentDataUtils } from "src/app/shared/type/currentdata";
import { Icon } from "src/app/shared/type/widget";
import { StringUtils } from "src/app/shared/utils/string/string.utils";
import { Controller_Io_ChannelSingleThresholdModalComponent } from "../modal/modal.component";

@Component({
    selector: "oe-controller-io-channelsinglethreshold",
    templateUrl: "./flat.html",
    standalone: false,
})
export class Controller_Io_ChannelSingleThresholdComponent extends AbstractFlatWidget {

    protected readonly Filter = Filter;
    protected inputChannel: WritableSignal<ChannelAddress | null> = signal(null);
    protected invert: WritableSignal<boolean | null> = signal(null);
    protected outputChannel: ChannelAddress;
    protected state: string;
    protected mode: string;
    protected modeValue: string;
    protected icon: Icon = {
        name: "",
        color: "",
        size: "large",
    };
    protected dependentOnLabel: string | null = null;
    protected currentValue: string | null = null;
    protected isOtherInputAddress: boolean;
    protected unitOfInputChannel: string | null = null;
    protected outputChannelValue: number | null = null;
    protected switchState: string;
    protected switchValue: string;
    protected switchConverter = Utils.CONVERT_WATT_TO_KILOWATT;
    protected modalComponent: Modal | null = null;

    /**
     * Gets the current value label in the form of e.g. "1000 W"
     *
     * @param dependendOnValue the value of the channel this controller dependends on
     * @param unitOfInputChannel the unit of the channel this controller dependends on
     * @returns the {@link dependendOnValue} and the {@link unitOfInputChannel} if defined, else null
     */
    private static createCurrentValueLabel(dependendOnValue: string | null, unitOfInputChannel: string | null): string | null {
        if (dependendOnValue == null || unitOfInputChannel == null) {
            return null;
        }
        return Formatter.formatSafelyWithSuffix(dependendOnValue, "1.0-0", unitOfInputChannel);
    }

    /**
     * Gets the switch state label
     *
     * @param invert the invert value
     * @param outputChannelValue the outputchannel value
     * @param threshold the threshold
     * @param translate the translate service
     * @returns a the switch state label
     */
    private static createSwitchStateLabel(invert: boolean, outputChannelValue: number | null, threshold: number | null, translate: TranslateService) {
        const isThresholdPositive = threshold !== null && threshold > 0;;
        const label = SwitchStateLabel.find(el =>
            el.invert === invert
            && (el.outputChannelValue === outputChannelValue)
            && (el.propertyThresholdPositive === isThresholdPositive)
        )?.label ?? null;

        return label == null ? null : translate.instant(label);
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: Controller_Io_ChannelSingleThresholdModalComponent,
            componentProps: {
                component: this.component,
                config: this.config,
                edge: this.edge,
                outputChannel: this.outputChannel,
                inputChannel: this.inputChannel,
                inputChannelUnit: this.unitOfInputChannel,
            },
        });
        return await modal.present();
    }

    protected getModalComponent(): Modal {
        return {
            component: Controller_Io_ChannelSingleThresholdModalComponent,
            componentProps: {
                component: this.component,
                config: this.config,
                edge: this.edge,
                outputChannel: this.outputChannel,
                inputChannel: this.inputChannel,
                inputChannelUnit: this.unitOfInputChannel,
            },
        };
    };

    protected override async afterIsInitialized(): Promise<void> {
        this.modalComponent = this.getModalComponent();
        this.inputChannel.set(ChannelAddress.fromStringSafely(
            this.component.getPropertyFromComponent("inputChannelAddress")));
        this.invert.set(this.component.getPropertyFromComponent("invert"));
    }

    protected override getChannelAddresses() {
        const outputChannelAddress: string | string[] = this.component.properties["outputChannelAddress"];
        if (typeof outputChannelAddress === "string") {
            this.outputChannel = ChannelAddress.fromString(outputChannelAddress);
        } else {
            // Takes only the first output for simplicity reasons
            this.outputChannel = ChannelAddress.fromString(outputChannelAddress[0]);
        }
        return [
            this.outputChannel,
            this.inputChannel(),

            ChannelAddress.fromString(this.component.id + "/_PropertyInvert"),
            ChannelAddress.fromString(this.component.id + "/_PropertyMode"),
            ChannelAddress.fromString(this.component.id + "/_PropertyThreshold"),
        ];
    }

    protected override async onCurrentData(currentData: CurrentData) {

        if (this.unitOfInputChannel == null && this.dependentOnLabel == null) {
            this.onInputChannelValueChange(currentData);
        }

        const inputChannel = ChannelAddress.fromString(this.component.getPropertyFromComponent<string>("inputChannelAddress"));
        const invert = this.getPropertyInvert(currentData) == 1;

        if (this.inputChannel().toString() !== inputChannel.toString() || (this.invert() != invert)) {
            this.inputChannel.set(inputChannel);
            this.invert.set(invert);
            this.onInputChannelValueChange(currentData);
        };

        const dependendOnValue = CurrentDataUtils.getChannel<string>(this.inputChannel(), currentData.allComponents);
        this.currentValue = Controller_Io_ChannelSingleThresholdComponent.createCurrentValueLabel(dependendOnValue, this.unitOfInputChannel);
        this.outputChannelValue = currentData.allComponents[this.outputChannel.toString()];

        // Icon, State
        switch (this.outputChannelValue) {
            case 0:
                this.icon.name = "radio-button-off-outline";
                this.state = this.translate.instant("GENERAL.OFF");
                break;
            case 1:
                this.icon.name = "aperture-outline";
                this.state = this.translate.instant("GENERAL.ON");
                break;
        }

        // Mode
        this.modeValue = currentData.allComponents[this.component.id + "/_PropertyMode"];
        switch (this.modeValue) {
            case "ON":
                this.mode = this.translate.instant("GENERAL.ON");
                break;
            case "OFF":
                this.mode = this.translate.instant("GENERAL.OFF");
                break;
            case "AUTOMATIC":
                this.mode = this.translate.instant("GENERAL.AUTOMATIC");
        }

        // True when InputAddress doesnt match any of the following channelIds
        this.isOtherInputAddress = StringUtils.isNotInArr(this.inputChannel.toString(),
            [null, "_sum/EssSoc", "_sum/GridActivePower", "_sum/ProductionActivePower"]);
    }

    /**
     * Acts on a inputChannel change
     *
     * @param currentData the current data
     */
    private async onInputChannelValueChange(currentData: CurrentData | null) {
        const res = await this.edge.getChannel(this.websocket, ChannelAddress.fromString(
            this.component.properties["inputChannelAddress"]));

        this.unitOfInputChannel = res?.unit ?? null;

        const inputChannel = this.inputChannel();
        this.dependentOnLabel = this.createDependenOnLabel(inputChannel, currentData);
        this.setSwitchValues(inputChannel, currentData);
    }

    /**
     * Sets the switch values
     *
     * @param inputChannel the chosen input channel
     * @param currentData the current data
     */
    private setSwitchValues(inputChannel: ChannelAddress, currentData: CurrentData | null) {
        const propertyThreshold = this.getPropertyThreshold(currentData);
        this.switchValue = propertyThreshold.toString();

        switch (inputChannel.toString()) {
            case "_sum/EssSoc":
                this.switchConverter = Utils.CONVERT_TO_PERCENT;
                break;
            case "_sum/ProductionActivePower":
                this.switchConverter = Utils.CONVERT_TO_WATT;
                break;
            case "_sum/GridActivePower":
                if (propertyThreshold < 0) {
                    if (this.outputChannelValue == 0) {
                        this.switchValue = (propertyThreshold * -1).toString();
                    } else if (this.outputChannelValue == 1) {
                        this.switchValue = (propertyThreshold * -1 - this.component.properties["switchedLoadPower"]).toString();;
                    }

                } else if (propertyThreshold > 0) {
                    if (this.outputChannelValue === 1) {
                        this.switchValue = (propertyThreshold - this.component.properties["switchedLoadPower"]).toString();
                    }

                    this.switchConverter = Utils.CONVERT_TO_WATT;
                }
                break;
            default:
                if (propertyThreshold < 0) {
                    this.switchValue = Utils.multiplySafely(this.component.properties["threshold"], -1)
                        + this.unitOfInputChannel !== "" ? this.unitOfInputChannel : "";
                } else if (propertyThreshold > 0) {
                    this.switchValue += this.unitOfInputChannel !== "" ? this.unitOfInputChannel : "";
                }
                this.switchConverter = Utils.CONVERT_TO_WATT;
                break;
        }

        // Threshold kleiner als 0 und invert == false und outputChannelValue == 0, dann switch on Below
        const outputChannelValue = CurrentDataUtils.getChannel<number | null>(this.outputChannel, currentData?.allComponents ?? null);
        this.switchState = Controller_Io_ChannelSingleThresholdComponent.createSwitchStateLabel(this.invert(), outputChannelValue, propertyThreshold, this.translate);
    }

    /**
     * Creates the dependent on label from given input channel
     *
     * @param inputChannel the chosen input channel
     * @param currentData the current data
     * @returns a label
     */
    private createDependenOnLabel(inputChannel: ChannelAddress, currentData: CurrentData | null): string {
        switch (inputChannel.toString()) {
            case "_sum/EssSoc":
                return this.translate.instant("GENERAL.SOC");
            case "_sum/GridActivePower": {
                const propertyThreshold = this.getPropertyThreshold(currentData);
                if (propertyThreshold < 0) {
                    return this.translate.instant("GENERAL.GRID_SELL");
                }
                return this.translate.instant("GENERAL.GRID_BUY");
            }
            case "_sum/ProductionActivePower":
                return this.translate.instant("GENERAL.PRODUCTION");
            default:
                return this.translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.OTHER") + " (" + inputChannel + ")";
        }
    }

    /**
     * Gets the property threshold
     *
     * @param currentData the currentData
     * @returns the value from a channel, if not set uses component properties value instead
     */
    private getPropertyThreshold(currentData: CurrentData): number | null {
        const channel: string = this.component.id + "/_PropertyThreshold";
        return (currentData && channel in currentData.allComponents) ? currentData.allComponents[channel] : this.component.getPropertyFromComponent("threshold");
    }

    /**
     * Gets the property threshold
     *
     * @param currentData the currentData
     * @returns the value from a channel, if not set uses component properties value instead
     */
    private getPropertyInvert(currentData: CurrentData): number | null {
        const channel: string = this.component.id + "/_PropertyInvert";
        return (currentData && channel in currentData.allComponents) ? currentData.allComponents[channel] : this.component.getPropertyFromComponent("invert");
    }
}

enum RelayState {
    OFF = 0,
    ON = 1,
}

const SwitchStateLabel = [
    { propertyThresholdPositive: true, invert: true, outputChannelValue: RelayState.OFF, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW" },
    { propertyThresholdPositive: true, invert: true, outputChannelValue: RelayState.ON, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW" },
    { propertyThresholdPositive: true, invert: false, outputChannelValue: RelayState.OFF, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE" },
    { propertyThresholdPositive: true, invert: false, outputChannelValue: RelayState.ON, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE" },
    { propertyThresholdPositive: false, invert: true, outputChannelValue: RelayState.OFF, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE" },
    { propertyThresholdPositive: false, invert: true, outputChannelValue: RelayState.ON, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE" },
    { propertyThresholdPositive: false, invert: false, outputChannelValue: RelayState.OFF, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW" },
    { propertyThresholdPositive: false, invert: false, outputChannelValue: RelayState.ON, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW" },
] as const;

