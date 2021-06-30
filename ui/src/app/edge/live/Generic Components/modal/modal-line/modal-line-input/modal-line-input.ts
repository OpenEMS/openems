import { Component } from "@angular/core";
import { AbstractModalLine } from "../abstract-modal-line";

/**
 * Shows a Line with Input-Field on the right
 */
@Component({
    selector: 'oe-modal-line-input',
    templateUrl: './modal-line-input.html',
})
export class ModalLineInput extends AbstractModalLine { }

