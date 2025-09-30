import { Component } from "@angular/core";
import { AbstractModalLine } from "../../abstract-modal-line";

@Component({
  /** If multiple items in line use this */
  selector: "oe-modal-line-item",
  templateUrl: "./modal-line-ITEM.HTML",
  standalone: false,
})
export class ModalLineItemComponent extends AbstractModalLine { }
