import { ActivatedRoute } from '@angular/router';
import { FixActivePowerModalComponent } from './modal/modal.component';
import { Component, Input } from '@angular/core';
import { Edge, EdgeConfig, Service } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { compileComponentFromMetadata } from '@angular/compiler';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: FixActivePowerComponent.SELECTOR,
  templateUrl: './fixactivepower.component.html'
})
export class FixActivePowerComponent {

  @Input() public componentId: string | null = null;


  private static readonly SELECTOR = "fixactivepower";
  private stopOnDestroy: Subject<void> = new Subject<void>();

  private edge: Edge = null;
  public component: EdgeConfig.Component | null = null;
  public chargeState: string;
  public chargeStateValue: number | string;
  state: any;

  constructor(
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      this.service.getConfig().then(config => {
        edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
          this.component = config.getComponent(this.componentId);
          if (this.component.properties.power >= 0) {
            this.chargeState = 'General.dischargePower';
            this.chargeStateValue = this.component.properties.power
          } else if (this.component.properties.power < 0) {
            this.chargeState = 'General.chargePower';
            this.chargeStateValue = this.component.properties.power * -1;
          }

          if (this.component.properties.mode == 'MANUAL_ON') {
            this.state = 'General.on'
          } else if (this.component.properties.mode == 'MANUAL_OFF') {
            this.state = 'General.off'
          } else {
            this.state = '-'
          }
        })
      })
    }
    )
  }
  ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
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
