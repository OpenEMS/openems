import { ActivatedRoute } from '@angular/router';
import { Component, Input } from '@angular/core';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { HeatPumpModalComponent } from './modal/modal.component';
import { BehaviorSubject } from 'rxjs';
import { Subject } from 'rxjs/internal/Subject';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: HeatPumpComponent.SELECTOR,
  templateUrl: './heatpump.component.html'
})
export class HeatPumpComponent {

  private static readonly SELECTOR = "heatpump";

  @Input() private componentId: string;

  private edge: Edge = null;
  public component: EdgeConfig.Component = null;
  public status: BehaviorSubject<{ name: string }> = new BehaviorSubject(null);
  private stopOnDestroy: Subject<void> = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    private websocket: Websocket,
    public service: Service,

  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      this.service.getConfig().then(config => {
        this.component = config.components[this.componentId];
        console.log("component", this.component)
        let channels = [
          new ChannelAddress(this.componentId, 'Status'),
          new ChannelAddress(this.componentId, 'State'),
        ]
        this.edge.subscribeChannels(this.websocket, HeatPumpComponent.SELECTOR, channels);

        this.edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
          // TODO TRANSLATE
          switch (currentData.channel[this.component.id + '/Status']) {
            case -1:
              this.status.next({ name: 'Undefiniert' })
              break;
            case 0:
              this.status.next({ name: 'Sperre' })
              break;
            case 1:
              this.status.next({ name: 'Normalbetrieb' })
              break;
            case 2:
              this.status.next({ name: 'Einschaltempfehlung' })
              break;
            case 3:
              this.status.next({ name: 'Einschaltbefehl' })
              break;
          }
        })

      })
    })

  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: HeatPumpModalComponent,
      componentProps: {
        edge: this.edge,
        component: this.component
      }
    });
    return await modal.present();
  }

  ngOnDestroy() {
    this.edge.unsubscribeChannels(this.websocket, HeatPumpComponent.SELECTOR);
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }
}
