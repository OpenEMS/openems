import { formatNumber } from "@angular/common";
import { Component, Input } from "@angular/core";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";

import { AbstractModalLine } from "../abstract-modal-line";
import { TextIndentation } from "../modal-line/modal-line";

@Component({
  /** If multiple items in line use this */
  selector: "oe-modal-meter-phases",
  templateUrl: "./modal-phases.html"
})
export class ModalPhasesComponent extends AbstractModalLine {

  protected readonly phases: { key: string, name: string }[] = [
    { key: "L1", name: "" },
    { key: "L2", name: "" },
    { key: "L3", name: "" }
  ];
  @Input() private setTranslatedName = (powerPerPhase: number) => { return ""; };

  protected readonly TextIndentation = TextIndentation;

  protected override getChannelAddresses(): ChannelAddress[] {
    let channelAddresses: ChannelAddress[] = [];

    for (let phase of this.phases) {
      channelAddresses.push(
        ChannelAddress.fromString(this.component.id + '/ActivePower' + phase.key)
      );
    }
    return channelAddresses;
  }

  protected override onCurrentData(currentData: CurrentData): void {
    for (let phase of this.phases) {
      let powerPerPhase = currentData.allComponents[this.component.id + '/ActivePower' + phase.key];
      phase.name = this.translate.instant('General.phase') + " " + phase.key + this.setTranslatedName(powerPerPhase);
    }
  }

  /**
   * Converts negative and positive [W] into positive [W]
   * 
   * @param value the value form passed value in html
   * @returns converted value
   */
  protected CONVERT_TO_POSITIVE_WATT = (value: number | null): string => {

    value = Utils.absSafely(value) ?? 0;
    return formatNumber(value, 'de', '1.0-0') + ' W';
  };
}
