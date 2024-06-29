import {Component, Input} from "@angular/core";
import {AbstractModalLine} from "../abstract-modal-line";
import {ButtonLabel} from "../modal-button/modal-button";

@Component({
  selector: 'oe-modal-line',
  templateUrl: './modal-line.html',
})
export class ModalLineComponent extends AbstractModalLine {

  /** ControlName for Form Field */
    @Input({ required: true }) public override controlName!: string;

    // Width of Left Column, Right Column is (100% - leftColumn)
  @Input({ required: true })
  protected leftColumnWidth!: number;


  @Input() protected button: ButtonLabel | null = null;
  /** ControlName for Toggle Button */
  @Input({ required: true }) protected control!:
    { type: 'TOGGLE' } |
    { type: 'INPUT', properties?: { unit: 'W' | '%' } } |
    /* the available select options*/
    { type: 'SELECT', options: { value: string, name: string }[] } |
    /* the properties for range slider*/
    { type: 'RANGE', properties: { min: number, max: number, unit: 'H' | '%', step?: number } } |
    /* the properties for range slider*/
    { type: 'DATE_PICKER', properties?: { label: string } };

  /** Fixed indentation of the modal-line */
  @Input() protected textIndent: TextIndentation = TextIndentation.NONE;
}

export enum TextIndentation {
  NONE = '0%',
  SINGLE = '5%',
  DOUBLE = '10%',
}
