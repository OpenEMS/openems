import { ActivatedRoute } from '@angular/router';
import { FixActivePowerModalComponent } from './modal/modal.component';
import { Component, Input } from '@angular/core';
import { Edge, EdgeConfig, Service } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
  selector: FixActivePowerComponent.SELECTOR,
  templateUrl: './fixactivepower.component.html'
})
export class FixActivePowerComponent {

  @Input() private componentId: string | null = null;

  private static readonly SELECTOR = "fixactivepower";

  private edge: Edge = null;
  public component: EdgeConfig.Component | null = null;

  constructor(
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      this.service.getConfig().then(config => {
        this.component = config.getComponent(this.componentId);
      })
    })
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: FixActivePowerModalComponent,
      componentProps: {
        component: this.component,
        edge: this.edge,
      }
    });
    return await modal.present();
  }
}
