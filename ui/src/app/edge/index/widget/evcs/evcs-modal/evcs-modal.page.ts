import { Component, OnInit } from '@angular/core';
import { environment } from 'src/environments/openems-backend-dev-local';
import { PopoverController, ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { Websocket } from 'src/app/shared/shared';

@Component({
  selector: 'app-evcs-modal',
  templateUrl: './evcs-modal.page.html',
  styleUrls: ['./evcs-modal.page.scss'],
})
export class EvcsModalPage implements OnInit {

  public env = environment;

  constructor(
    public popoverController: PopoverController,
    public websocket: Websocket,
    public router: Router
  ) { }

  ngOnInit() {
  }



}
