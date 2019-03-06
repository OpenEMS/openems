import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Utils, Websocket, EdgeConfig } from '../../../../shared/shared';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';

@Component({
  selector: ComponentInstallComponent.SELECTOR,
  templateUrl: './install.component.html'
})
export class ComponentInstallComponent implements OnInit {

  private static readonly SELECTOR = "componentInstall";

  public factory: EdgeConfig.Factory = null;
  public form = null;
  public model = null;
  public fields: FormlyFieldConfig[] = null;

  submit(model) {
    console.log(model);
  }

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
  ) {
  }

  ngOnInit() {
    this.service.setCurrentEdge(this.route);
  }
}