import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Utils, Websocket, EdgeConfig } from '../../../../shared/shared';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';

@Component({
  selector: ComponentInstallComponent.SELECTOR,
  templateUrl: './install.component.html'
})
export class ComponentInstallComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "componentInstall";

  public factory: EdgeConfig.Factory = null;
  public form = null;
  public model = null;
  public fields: FormlyFieldConfig[] = null;

  submit(model) {
    console.log(model);
  }

  // public form = null;

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
  ) {
  }

  ngOnInit() {
    this.service.setCurrentEdge(this.route);
    let factoryId = this.route.snapshot.params["factoryId"];
    this.service.getConfig().then(config => {
      let fields: FormlyFieldConfig[] = [];
      let model = {};
      this.factory = config.factories[factoryId];
      for (let property of this.factory.properties) {
        fields.push({
          key: property.id,
          type: 'input',
          templateOptions: {
            label: property.name,
            description: property.description,
            required: property.isRequired
          }
        })
        // properties[property.id] = property.schema;
        if (property.defaultValue) {
          model[property.id] = property.defaultValue;
        }
      }
      this.form = new FormGroup({});
      this.fields = fields;
      this.model = model;
    });
  }

  ngOnDestroy() {
  }
}