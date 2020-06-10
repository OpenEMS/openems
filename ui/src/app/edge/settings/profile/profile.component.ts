import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { environment } from '../../../../environments';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { CategorizedComponents } from 'src/app/shared/edge/edgeconfig';
import { ModbusApiUtil } from './modbusapi/modbusapi';
import { PopoverController } from '@ionic/angular';
import { ProfilePopoverComponent } from './popover/popover.component';

@Component({
  selector: ProfileComponent.SELECTOR,
  templateUrl: './profile.component.html'
})
export class ProfileComponent {

  private static readonly SELECTOR = "profile";

  public env = environment;

  public edge: Edge = null;
  public config: EdgeConfig = null;
  public subscribedChannels: ChannelAddress[] = [];

  public components: CategorizedComponents[];

  constructor(
    private service: Service,
    private route: ActivatedRoute,
    public popoverController: PopoverController,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent("Anlagenprofil" /* TODO translate */, this.route).then(edge => {
      this.edge = edge;
    });
    this.service.getConfig().then(config => {
      this.config = config;
      let categorizedComponentIds: string[] = ["_componentManager", "_cycle", "_meta", "_power", "_sum"]
      this.components = config.listActiveComponents(categorizedComponentIds);
    })
  }

  async presentPopover(component: EdgeConfig.Component) {
    const popover = await this.popoverController.create({
      component: ProfilePopoverComponent,
      // event: ev,
      translucent: true,
      componentProps: {
        component: component,
        edge: this.edge,
      }
    });
    return await popover.present();
  }

  public getModbusProtocol(componentId: string) {
    ModbusApiUtil.getModbusProtocol(this.service, componentId);
  };
}