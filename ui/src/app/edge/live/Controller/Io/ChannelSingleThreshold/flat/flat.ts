// @ts-strict-ignore
import { Component, signal, WritableSignal } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Filter } from "src/app/shared/components/shared/filter";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { CurrentDataUtils } from "src/app/shared/type/currentdata";
import { Icon } from "src/app/shared/type/widget";
import { StringUtils } from "src/app/shared/utils/string/STRING.UTILS";
import { Controller_Io_ChannelSingleThresholdModalComponent } from "../modal/MODAL.COMPONENT";

@Component({
  selector: "oe-controller-io-channelsinglethreshold",
  templateUrl: "./FLAT.HTML",
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

  /**
   * Gets the current value label in the form of E.G. "1000 W"
   *
   * @param dependendOnValue the value of the channel this controller dependends on
   * @param unitOfInputChannel the unit of the channel this controller dependends on
   * @returns the {@link dependendOnValue} and the {@link unitOfInputChannel} if defined, else null
   */
  private static createCurrentValueLabel(dependendOnValue: string | null, unitOfInputChannel: string | null): string | null {
    if (dependendOnValue == null || unitOfInputChannel == null) {
      return null;
    }
    return FORMATTER.FORMAT_SAFELY_WITH_SUFFIX(dependendOnValue, "1.0-0", unitOfInputChannel);
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
    const label = SWITCH_STATE_LABEL.FIND(el =>
      EL.INVERT === invert
      && (EL.OUTPUT_CHANNEL_VALUE === outputChannelValue)
      && (EL.PROPERTY_THRESHOLD_POSITIVE === isThresholdPositive)
    )?.label ?? null;

    return label == null ? null : TRANSLATE.INSTANT(label);
  }

  async presentModal() {
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: Controller_Io_ChannelSingleThresholdModalComponent,
      componentProps: {
        component: THIS.COMPONENT,
        config: THIS.CONFIG,
        edge: THIS.EDGE,
        outputChannel: THIS.OUTPUT_CHANNEL,
        inputChannel: THIS.INPUT_CHANNEL,
        inputChannelUnit: THIS.UNIT_OF_INPUT_CHANNEL,
      },
    });
    return await MODAL.PRESENT();
  }

  protected override async afterIsInitialized(): Promise<void> {
    THIS.INPUT_CHANNEL.SET(CHANNEL_ADDRESS.FROM_STRING_SAFELY(
      THIS.COMPONENT.GET_PROPERTY_FROM_COMPONENT("inputChannelAddress")));
    THIS.INVERT.SET(THIS.COMPONENT.GET_PROPERTY_FROM_COMPONENT("invert"));
  }

  protected override getChannelAddresses() {
    const outputChannelAddress: string | string[] = THIS.COMPONENT.PROPERTIES["outputChannelAddress"];
    if (typeof outputChannelAddress === "string") {
      THIS.OUTPUT_CHANNEL = CHANNEL_ADDRESS.FROM_STRING(outputChannelAddress);
    } else {
      // Takes only the first output for simplicity reasons
      THIS.OUTPUT_CHANNEL = CHANNEL_ADDRESS.FROM_STRING(outputChannelAddress[0]);
    }
    return [
      THIS.OUTPUT_CHANNEL,
      THIS.INPUT_CHANNEL(),

      CHANNEL_ADDRESS.FROM_STRING(THIS.COMPONENT.ID + "/_PropertyInvert"),
      CHANNEL_ADDRESS.FROM_STRING(THIS.COMPONENT.ID + "/_PropertyMode"),
      CHANNEL_ADDRESS.FROM_STRING(THIS.COMPONENT.ID + "/_PropertyThreshold"),
    ];
  }

  protected override async onCurrentData(currentData: CurrentData) {

    if (THIS.UNIT_OF_INPUT_CHANNEL == null && THIS.DEPENDENT_ON_LABEL == null) {
      THIS.ON_INPUT_CHANNEL_VALUE_CHANGE(currentData);
    }

    const inputChannel = CHANNEL_ADDRESS.FROM_STRING(THIS.COMPONENT.GET_PROPERTY_FROM_COMPONENT<string>("inputChannelAddress"));
    const invert = THIS.GET_PROPERTY_INVERT(currentData) == 1;

    if (THIS.INPUT_CHANNEL().toString() !== INPUT_CHANNEL.TO_STRING() || (THIS.INVERT() != invert)) {
      THIS.INPUT_CHANNEL.SET(inputChannel);
      THIS.INVERT.SET(invert);
      THIS.ON_INPUT_CHANNEL_VALUE_CHANGE(currentData);
    };

    const dependendOnValue = CURRENT_DATA_UTILS.GET_CHANNEL<string>(THIS.INPUT_CHANNEL(), CURRENT_DATA.ALL_COMPONENTS);
    THIS.CURRENT_VALUE = Controller_Io_ChannelSingleThresholdComponent.createCurrentValueLabel(dependendOnValue, THIS.UNIT_OF_INPUT_CHANNEL);
    THIS.OUTPUT_CHANNEL_VALUE = CURRENT_DATA.ALL_COMPONENTS[THIS.OUTPUT_CHANNEL.TO_STRING()];

    // Icon, State
    switch (THIS.OUTPUT_CHANNEL_VALUE) {
      case 0:
        THIS.ICON.NAME = "radio-button-off-outline";
        THIS.STATE = THIS.TRANSLATE.INSTANT("GENERAL.OFF");
        break;
      case 1:
        THIS.ICON.NAME = "aperture-outline";
        THIS.STATE = THIS.TRANSLATE.INSTANT("GENERAL.ON");
        break;
    }

    // Mode
    THIS.MODE_VALUE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/_PropertyMode"];
    switch (THIS.MODE_VALUE) {
      case "ON":
        THIS.MODE = THIS.TRANSLATE.INSTANT("GENERAL.ON");
        break;
      case "OFF":
        THIS.MODE = THIS.TRANSLATE.INSTANT("GENERAL.OFF");
        break;
      case "AUTOMATIC":
        THIS.MODE = THIS.TRANSLATE.INSTANT("GENERAL.AUTOMATIC");
    }

    // True when InputAddress doesnt match any of the following channelIds
    THIS.IS_OTHER_INPUT_ADDRESS = STRING_UTILS.IS_NOT_IN(THIS.INPUT_CHANNEL.TO_STRING(),
      [null, "_sum/EssSoc", "_sum/GridActivePower", "_sum/ProductionActivePower"]);
  }

  /**
   * Acts on a inputChannel change
   *
   * @param currentData the current data
   */
  private async onInputChannelValueChange(currentData: CurrentData | null) {
    const res = await THIS.EDGE.GET_CHANNEL(THIS.WEBSOCKET, CHANNEL_ADDRESS.FROM_STRING(
      THIS.COMPONENT.PROPERTIES["inputChannelAddress"]));

    THIS.UNIT_OF_INPUT_CHANNEL = res?.unit ?? null;

    const inputChannel = THIS.INPUT_CHANNEL();
    THIS.DEPENDENT_ON_LABEL = THIS.CREATE_DEPENDEN_ON_LABEL(inputChannel, currentData);
    THIS.SET_SWITCH_VALUES(inputChannel, currentData);
  }

  /**
   * Sets the switch values
   *
   * @param inputChannel the chosen input channel
   * @param currentData the current data
   */
  private setSwitchValues(inputChannel: ChannelAddress, currentData: CurrentData | null) {
    const propertyThreshold = THIS.GET_PROPERTY_THRESHOLD(currentData);
    THIS.SWITCH_VALUE = PROPERTY_THRESHOLD.TO_STRING();

    switch (INPUT_CHANNEL.TO_STRING()) {
      case "_sum/EssSoc":
        THIS.SWITCH_CONVERTER = Utils.CONVERT_TO_PERCENT;
        break;
      case "_sum/ProductionActivePower":
        THIS.SWITCH_CONVERTER = Utils.CONVERT_TO_WATT;
        break;
      case "_sum/GridActivePower":
        if (propertyThreshold < 0) {
          if (THIS.OUTPUT_CHANNEL_VALUE == 0) {
            THIS.SWITCH_VALUE = (propertyThreshold * -1).toString();
          } else if (THIS.OUTPUT_CHANNEL_VALUE == 1) {
            THIS.SWITCH_VALUE = (propertyThreshold * -1 - THIS.COMPONENT.PROPERTIES["switchedLoadPower"]).toString();;
          }

        } else if (propertyThreshold > 0) {
          if (THIS.OUTPUT_CHANNEL_VALUE === 1) {
            THIS.SWITCH_VALUE = (propertyThreshold - THIS.COMPONENT.PROPERTIES["switchedLoadPower"]).toString();
          }

          THIS.SWITCH_CONVERTER = Utils.CONVERT_TO_WATT;
        }
        break;
      default:
        if (propertyThreshold < 0) {
          THIS.SWITCH_VALUE = UTILS.MULTIPLY_SAFELY(THIS.COMPONENT.PROPERTIES["threshold"], -1)
            + THIS.UNIT_OF_INPUT_CHANNEL !== "" ? THIS.UNIT_OF_INPUT_CHANNEL : "";
        } else if (propertyThreshold > 0) {
          THIS.SWITCH_VALUE += THIS.UNIT_OF_INPUT_CHANNEL !== "" ? THIS.UNIT_OF_INPUT_CHANNEL : "";
        }
        THIS.SWITCH_CONVERTER = Utils.CONVERT_TO_WATT;
        break;
    }

    // Threshold kleiner als 0 und invert == false und outputChannelValue == 0, dann switch on Below
    const outputChannelValue = CURRENT_DATA_UTILS.GET_CHANNEL<number | null>(THIS.OUTPUT_CHANNEL, currentData?.allComponents ?? null);
    THIS.SWITCH_STATE = Controller_Io_ChannelSingleThresholdComponent.createSwitchStateLabel(THIS.INVERT(), outputChannelValue, propertyThreshold, THIS.TRANSLATE);
  }

  /**
   * Creates the dependent on label from given input channel
   *
   * @param inputChannel the chosen input channel
   * @param currentData the current data
   * @returns a label
   */
  private createDependenOnLabel(inputChannel: ChannelAddress, currentData: CurrentData | null): string {
    switch (INPUT_CHANNEL.TO_STRING()) {
      case "_sum/EssSoc":
        return THIS.TRANSLATE.INSTANT("GENERAL.SOC");
      case "_sum/GridActivePower": {
        const propertyThreshold = THIS.GET_PROPERTY_THRESHOLD(currentData);
        if (propertyThreshold < 0) {
          return THIS.TRANSLATE.INSTANT("GENERAL.GRID_SELL");
        }
        return THIS.TRANSLATE.INSTANT("GENERAL.GRID_BUY");
      }
      case "_sum/ProductionActivePower":
        return THIS.TRANSLATE.INSTANT("GENERAL.PRODUCTION");
      default:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.OTHER") + " (" + inputChannel + ")";
    }
  }

  /**
   * Gets the property threshold
   *
   * @param currentData the currentData
   * @returns the value from a channel, if not set uses component properties value instead
   */
  private getPropertyThreshold(currentData: CurrentData): number | null {
    const channel: string = THIS.COMPONENT.ID + "/_PropertyThreshold";
    return (currentData && channel in CURRENT_DATA.ALL_COMPONENTS) ? CURRENT_DATA.ALL_COMPONENTS[channel] : THIS.COMPONENT.GET_PROPERTY_FROM_COMPONENT("threshold");
  }

  /**
   * Gets the property threshold
   *
   * @param currentData the currentData
   * @returns the value from a channel, if not set uses component properties value instead
   */
  private getPropertyInvert(currentData: CurrentData): number | null {
    const channel: string = THIS.COMPONENT.ID + "/_PropertyInvert";
    return (currentData && channel in CURRENT_DATA.ALL_COMPONENTS) ? CURRENT_DATA.ALL_COMPONENTS[channel] : THIS.COMPONENT.GET_PROPERTY_FROM_COMPONENT("invert");
  }
}

enum RelayState {
  OFF = 0,
  ON = 1,
}

const SwitchStateLabel = [
  { propertyThresholdPositive: true, invert: true, outputChannelValue: RELAY_STATE.OFF, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW" },
  { propertyThresholdPositive: true, invert: true, outputChannelValue: RELAY_STATE.ON, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW" },
  { propertyThresholdPositive: true, invert: false, outputChannelValue: RELAY_STATE.OFF, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE" },
  { propertyThresholdPositive: true, invert: false, outputChannelValue: RELAY_STATE.ON, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE" },
  { propertyThresholdPositive: false, invert: true, outputChannelValue: RELAY_STATE.OFF, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE" },
  { propertyThresholdPositive: false, invert: true, outputChannelValue: RELAY_STATE.ON, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE" },
  { propertyThresholdPositive: false, invert: false, outputChannelValue: RELAY_STATE.OFF, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW" },
  { propertyThresholdPositive: false, invert: false, outputChannelValue: RELAY_STATE.ON, label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW" },
] as const;

