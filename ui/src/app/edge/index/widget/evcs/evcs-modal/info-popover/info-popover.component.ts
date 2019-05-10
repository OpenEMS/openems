import { Component, OnInit, Input } from '@angular/core';
import { PopoverController } from '@ionic/angular';

@Component({
  selector: 'app-info-popover',
  templateUrl: './info-popover.component.html',
  styleUrls: ['./info-popover.component.scss'],
})
export class InfoPopoverComponent implements OnInit {

  @Input() chargeMode: String;

  constructor(public popoverController: PopoverController) { }

  ngOnInit() {
  }

  cancel() {
    console.log("verlassen des popups!!!");
    this.popoverController.dismiss();
  }
}
