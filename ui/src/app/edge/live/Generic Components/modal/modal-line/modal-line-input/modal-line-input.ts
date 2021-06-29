import { Component, Input, OnInit } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { Edge, EdgeConfig } from "src/app/shared/shared";
import { AbstractFlatWidgetLine } from "../../../flat/flat-widget-line/abstract-flat-widget-line";
import { AbstractModal } from "../../abstractModal";
import { AbstractModalLine } from "../abstract-modal-line";

/**
 * Shows a Line with Input-Field
 */
@Component({
    selector: 'oe-modal-line-input',
    templateUrl: './modal-line-input.html',
})
export class ModalLineInput extends AbstractModalLine implements OnInit {

    /** Name for parameter, displayed on the left side*/
    @Input() name: string;

    /** FormGroup */
    @Input() formGroup: FormGroup;
}

