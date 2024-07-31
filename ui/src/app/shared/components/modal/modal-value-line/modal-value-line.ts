// @ts-strict-ignore
import { Component, Input } from "@angular/core";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";

import { AbstractModalLine } from "../abstract-modal-line";

@Component({
  selector: 'oe-modal-value-line',
  templateUrl: './modal-value-line.html',
})
export class ModalValueLineComponent extends AbstractModalLine {

  // Width of Left Column, Right Column is (100% - leftColumn)
  @Input({ required: true }) protected leftColumnWidth!: number;

  /** Fixed indentation of the modal-line */
  @Input() protected textIndent: TextIndentation = TextIndentation.NONE;

  @Input({ required: true }) private valueCallback!: (currentData: CurrentData) => string;

  private channels: ChannelAddress[];

  @Input() set channelsToSubscribe(channels: ChannelAddress[]) {
    this.channels = channels;
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    return this.channels;
  }

  protected override onCurrentData(currentData: CurrentData): void {
    this.displayValue = this.valueCallback(currentData);
  }
}

export enum TextIndentation {
  NONE = '0%',
  SINGLE = '5%',
  DOUBLE = '10%',
}
