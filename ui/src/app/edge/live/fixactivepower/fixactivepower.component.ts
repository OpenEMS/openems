import { Component, Input, ViewContainerRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { UUID } from 'angular2-uuid';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { AbstractFlatWidgetComponent } from '../abstractFlatWidget.component';
import { FixActivePowerModalComponent } from './modal/modal.component';

@Component({
  selector: 'fixactivepower',
  templateUrl: './fixactivepower.component.html'
})
export class FixActivePowerComponent extends AbstractFlatWidgetComponent {

  @Input() private componentId: string | null = null;

  private stopOnDestroy: Subject<void> = new Subject<void>();
  public edge: Edge = null;
  public component: EdgeConfig.Component | null = null;
  public chargeState: string;
  public chargeStateValue: number | string;
  public state: string;
  public channels: ChannelAddress[] = []
  public randomselector: string = UUID.UUID().toString();

  constructor(
    public translate: TranslateService,
    public route: ActivatedRoute,
    public modalCtrl: ModalController,
    public service: Service,
    public viewContainerRef: ViewContainerRef,
    public websocket: Websocket

  ) {
    super(websocket)
  }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      this.service.getConfig().then(config => {
        this.component = config.components[this.componentId];
        let power = this.componentId + '/_PropertyPower';
        let mode = this.componentId + '/_PropertyMode';
        this.channels.push(ChannelAddress.fromString(power), ChannelAddress.fromString(mode));
        this.subscribeOnChannels(this.randomselector, this.channels);

        this.edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
          let channelPower = currentData.channel[power];
          let channelMode = currentData.channel[mode]

          if (channelPower >= 0) {
            this.chargeState = this.translate.instant('General.dischargePower');
            this.chargeStateValue = this.component.properties.power
          } else if (channelPower < 0) {
            this.chargeState = this.translate.instant('General.chargePower');
            this.chargeStateValue = this.component.properties.power * -1;
          }
          if (channelMode == 'MANUAL_ON') {
            this.state = this.translate.instant('General.on');
          } else if (channelMode == 'MANUAL_OFF') {
            this.state = this.translate.instant('General.off');
          } else {
            this.state = '-'
          }
        });
      })
    })
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
