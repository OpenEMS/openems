// @ts-strict-ignore
import { Component, Input } from "@angular/core";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";

import { OeFormlyField } from "../../shared/oe-formly-component";
import { AbstractModalLine } from "../abstract-modal-line";

@Component({
    selector: "oe-modal-value-line",
    templateUrl: "./modal-value-line.html",
    standalone: false,
})
export class ModalValueLineComponent extends AbstractModalLine {

    // Width of Left Column, Right Column is (100% - leftColumn)
    @Input({ required: true }) public leftColumnWidth!: number;

    /** Fixed indentation of the modal-line */
    @Input() public textIndent: TextIndentation = TextIndentation.NONE;

    @Input({ required: true }) public valueCallback!: (currentData: CurrentData) => string;

    protected shouldShow: boolean = false;
    private channels: ChannelAddress[];
    private _filters: OeFormlyField.ValueFromChannelsLine["filter"] | null = null;

    @Input() public set channelsToSubscribe(channels: ChannelAddress[]) {
        this.channels = channels;
    }

    @Input() public set filters(self: OeFormlyField.ValueFromChannelsLine["filter"] | null) {
        this._filters = self;
    }


    protected override getChannelAddresses(): ChannelAddress[] {
        return this.channels;
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.shouldShow = this._filters !== null && typeof this._filters === "function" ? this._filters(currentData) : true;
        this.displayValue = this.valueCallback(currentData);
    }
}

export enum TextIndentation {
    NONE = "0%",
    SINGLE = "5%",
    DOUBLE = "10%",
}
