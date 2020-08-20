import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Edge, EdgeConfig, Service } from '../../../shared/shared';
import { EvcsInstallerComponent } from './evcs/evcs.component';
import { HeatingElementRtuInstallerComponent } from './heatingelementrtu/heatingelementrtu.component';
import { HeatingElementTcpInstallerComponent } from './heatingelementtcp/heatingelementtcp.component';
import { HeatingpumpTcpInstallerComponent } from './heatingpumptcp/heatingpumptcp.component';
import { ModalController } from '@ionic/angular';

@Component({
  selector: AutoinstallerComponent.SELECTOR,
  templateUrl: './autoinstaller.component.html'
})
export class AutoinstallerComponent {

  private static readonly SELECTOR = "autoinstaller";

  public edge: Edge = null;
  public config: EdgeConfig = null;

  public factory: EdgeConfig.Factory = null;

  constructor(
    private route: ActivatedRoute,
    private service: Service,
    public modalCtrl: ModalController,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('Automatische Installation', this.route).then(edge => {
      this.edge = edge;
    });
    this.service.getConfig().then(config => {
      this.config = config;
    });
  }

  public async presentModalHeatingelementRTU() {
    const modal = await this.modalCtrl.create({
      component: HeatingElementRtuInstallerComponent,
    });
    return await modal.present();
  }

  public async presentModalHeatingelementTCP() {
    const modal = await this.modalCtrl.create({
      component: HeatingElementTcpInstallerComponent,
    });
    return await modal.present();
  }

  public async presentModalEVCS() {
    const modal = await this.modalCtrl.create({
      component: EvcsInstallerComponent,
    });
    return await modal.present();
  }

  public async presentModalHeatingpump() {
    const modal = await this.modalCtrl.create({
      component: HeatingpumpTcpInstallerComponent,
    });
    return await modal.present();
  }
}