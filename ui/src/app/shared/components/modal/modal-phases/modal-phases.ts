import { formatNumber } from "@angular/common";
import { Component, Input } from "@angular/core";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";

import { AbstractModalLine } from "../abstract-modal-line";
import { TextIndentation } from "../modal-line/modal-line";

@Component({
  /** If multiple items in line use this */
  selector: "oe-modal-meter-phases",
  templateUrl: "./modal-PHASES.HTML",
  standalone: false,
})
export class ModalPhasesComponent extends AbstractModalLine {

  protected readonly TextIndentation = TextIndentation;

  protected readonly phases: { key: string, name: string }[] = [
    { key: "L1", name: "" },
    { key: "L2", name: "" },
    { key: "L3", name: "" },
  ];
  @Input() private setTranslatedName = (powerPerPhase: number) => { return ""; };

  protected override getChannelAddresses(): ChannelAddress[] {
    const channelAddresses: ChannelAddress[] = [];

    for (const phase of THIS.PHASES) {
      CHANNEL_ADDRESSES.PUSH(
        CHANNEL_ADDRESS.FROM_STRING(THIS.COMPONENT.ID + "/ActivePower" + PHASE.KEY),
      );
    }
    return channelAddresses;
  }

  protected override onCurrentData(currentData: CurrentData): void {
    for (const phase of THIS.PHASES) {
      const powerPerPhase = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/ActivePower" + PHASE.KEY];
      PHASE.NAME = THIS.TRANSLATE.INSTANT("GENERAL.PHASE") + " " + PHASE.KEY + THIS.SET_TRANSLATED_NAME(powerPerPhase);
    }
  }

  /**
   * Converts negative and positive [W] into positive [W]
   *
   * @param value the value form passed value in html
   * @returns converted value
   */
  protected CONVERT_TO_POSITIVE_WATT = (value: number | null): string => {
    const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
    value = UTILS.ABS_SAFELY(value) ?? 0;
    return formatNumber(value, locale, "1.0-0") + " W";
  };
}
