import { ActivatedRoute } from '@angular/router';
import { CategorizedComponents } from 'src/app/shared/edge/edgeconfig';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { Component } from '@angular/core';
import { environment } from '../../../../environments';
import { ModbusApiUtil } from './modbusapi/modbusapi';
import { PopoverController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';

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
    private translate: TranslateService,
  ) { }

  ionViewWillEnter() {
    this.service.setCurrentComponent(this.translate.instant('Edge.Config.Index.systemProfile'), this.route).then(edge => {
      this.edge = edge;
      this.service.getConfig().then(config => {
        this.config = config;
        let categorizedComponentIds: string[] = ["_componentManager", "_cycle", "_meta", "_power", "_sum"]
        this.components = config.listActiveComponents(categorizedComponentIds);
      })
    });
  }

  public getModbusProtocol(componentId: string) {
    ModbusApiUtil.getModbusProtocol(this.service, componentId);
  };
}