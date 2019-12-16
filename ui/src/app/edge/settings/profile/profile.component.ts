import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { environment } from '../../../../environments';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { CategorizedComponents } from 'src/app/shared/edge/edgeconfig';

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
    private route: ActivatedRoute
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
}