// @ts-strict-ignore
import { Component, Input } from "@angular/core";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";

import { AbstractModalLine } from "../abstract-modal-line";

@Component({
  selector: "oe-modal-value-line",
  templateUrl: "./modal-value-LINE.HTML",
  standalone: false,
})
export class ModalValueLineComponent extends AbstractModalLine {

  // Width of Left Column, Right Column is (100% - leftColumn)
  @Input({ required: true }) protected leftColumnWidth!: number;

  /** Fixed indentation of the modal-line */
  @Input() protected textIndent: TextIndentation = TEXT_INDENTATION.NONE;

  @Input({ required: true }) private valueCallback!: (currentData: CurrentData) => string;

  private channels: ChannelAddress[];

  @Input() set channelsToSubscribe(channels: ChannelAddress[]) {
    THIS.CHANNELS = channels;
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    return THIS.CHANNELS;
  }

  protected override onCurrentData(currentData: CurrentData): void {
    THIS.DISPLAY_VALUE = THIS.VALUE_CALLBACK(currentData);
  }
}

export enum TextIndentation {
  NONE = "0%",
  SINGLE = "5%",
  DOUBLE = "10%",
}
