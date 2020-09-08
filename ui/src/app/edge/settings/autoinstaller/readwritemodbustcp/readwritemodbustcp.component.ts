import { Component } from '@angular/core';
import { Edge, EdgeConfig, Service, Websocket, Utils } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: ReadWriteModbusTCPInstallerComponent.SELECTOR,
  templateUrl: './readwritemodbustcp.component.html'
})
export class ReadWriteModbusTCPInstallerComponent {

  public showInit: boolean = false;
  public progressPercentage: number = 0;
  public installing = false;
  public running = false;
  public loading = true;
  public loadingStrings: { string: string, type: string }[] = [];

  public edge: Edge = null;
  public config: EdgeConfig = null;

  private static readonly SELECTOR = "ModbusTcpReadWriteInstaller";

  constructor(
    private route: ActivatedRoute,
    public service: Service,
    public modalCtrl: ModalController,
    private websocket: Websocket,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('Automatische Installation', this.route).then(edge => {
      this.service.getConfig().then(config => {
        this.edge = edge;
        this.config = config;
        if (this.hasReadOnly() == true || this.hasReadWrite(config) == false) {
          this.showInit = true;
          this.loading = false;
        } else if (this.hasReadWrite(config) == true) {
          this.loading = false;
          this.running = true;
        }
      })
    });
  }

  private hasReadOnly(): boolean {
    let compArr: EdgeConfig.Component[] = [];

    this.config.getComponentsByFactory('Controller.Api.ModbusTcp.ReadOnly').forEach(component => {
      compArr.push(component)
    })
    if (compArr.length > 0) {
      return true;
    } else {
      return false;
    }
  }

  private hasReadWrite(config: EdgeConfig): boolean {
    let compArr: EdgeConfig.Component[] = [];

    config.getComponentsByFactory('Controller.Api.ModbusTcp.ReadWrite').forEach(component => {
      compArr.push(component)
    })
    if (compArr.length > 0) {
      return true;
    } else {
      return false;
    }
  }

  public startInstallation() {
    this.showInit = false;
    this.installing = true;
    this.loadingStrings.push({ string: 'Suche Controller.Api.ModbusTcp.ReadOnly..', type: 'setup' });
    this.progressPercentage = 0.1;

    if (this.hasReadOnly() == true) {
      setTimeout(() => {
        this.loadingStrings.push({ string: 'Controller.Api.ModbusTcp.ReadOnly gefunden', type: 'setup' });
        this.progressPercentage = 0.2;
      }, 2000)
      setTimeout(() => {
        this.loadingStrings.push({ string: 'Versuche Controller.Api.ModbusTcp.ReadOnly zu entfernen..', type: 'setup' });
        this.progressPercentage = 0.3;
      }, 5000)
      this.config.getComponentsByFactory('Controller.Api.ModbusTcp.ReadOnly').forEach(component => {
        this.edge.deleteComponentConfig(this.websocket, component.id).then(() => {
          setTimeout(() => {
            this.loadingStrings.push({ string: 'Controller.Api.ModbusTcp.ReadOnly wird entfernt', type: 'success' });
            this.progressPercentage = 0.5;
          }, 7000);
        })
      })
      setTimeout(() => {
        this.loadingStrings.push({ string: 'Versuche Controller.Api.ModbusTcp.ReadWrite hinzuzufügen..', type: 'setup' });
        this.progressPercentage = 0.7;
      }, 10000)

      this.edge.createComponentConfig(this.websocket, 'Controller.Api.ModbusTcp.ReadWrite', this.gatherModbusTCPReadWrite(this.config)).then(() => {
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Controller.Api.ModbusTcp.ReadWrite wird hinzugefügt', type: 'success' });
          this.progressPercentage = 0.9;
        }, 12000);
      }).catch(() => {
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Fehler Controller.Api.ModbusTcp.ReadWrite hinzuzufügen', type: 'danger' });
        }, 12000);
      });
    } else {
      setTimeout(() => {
        this.loadingStrings.push({ string: 'Controller.Api.ModbusTcp.ReadOnly nicht vorhanden', type: 'success' });
        this.progressPercentage = 0.5;
      }, 3000)

      setTimeout(() => {
        this.loadingStrings.push({ string: 'Versuche Controller.Api.ModbusTcp.ReadWrite hinzuzufügen..', type: 'setup' });
        this.progressPercentage = 0.7;
      }, 7000)

      this.edge.createComponentConfig(this.websocket, 'Controller.Api.ModbusTcp.ReadWrite', this.gatherModbusTCPReadWrite(this.config)).then(() => {
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Controller.Api.ModbusTcp.ReadWrite wird hinzugefügt', type: 'success' });
          this.progressPercentage = 0.9;
        }, 11000);
      }).catch(reason => {
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Fehler Controller.Api.ModbusTcp.ReadWrite hinzuzufügen', type: 'danger' });
          this.loadingStrings.push({ string: reason.error.message.toString(), type: 'danger' });
        }, 11000);
      });
    }
    setTimeout(() => {
      this.checkConfiguration()
    }, 15000)
  }

  private checkConfiguration() {
    this.loadingStrings = [];
    this.loadingStrings.push({ string: 'Überprüfe ob Komponenten korrekt hinzugefügt wurden..', type: 'setup' });
    this.progressPercentage = 0.9;
    setTimeout(() => {
      this.service.getConfig().then(config => {
        this.config = config;
        if (this.hasReadWrite(config) == true) {
          this.loadingStrings.push({ string: 'Komponenten korrekt hinzugefügt', type: 'success' });
          this.progressPercentage = 0.98;
          setTimeout(() => {
            this.running = true;
            this.installing = false;
          }, 2000)
        } else {
          this.loadingStrings.push({ string: 'Es konnten nicht alle Komponenten korrekt hinzugefügt werden', type: 'danger' });
          this.loadingStrings.push({ string: 'Bitte Prozess neu starten oder gegebenenfalls manuell korrigieren', type: 'danger' });
        }
      })
    }, 6000);
  }

  private gatherModbusTCPReadWrite(config: EdgeConfig): { name: string, value: any }[] {
    let factoryId = 'Controller.Api.ModbusTcp.ReadWrite';
    let factory = config.factories[factoryId];
    let fields: FormlyFieldConfig[] = [];
    let model = {};
    for (let property of factory.properties) {
      let property_id = property.id.replace('.', '_');
      let field: FormlyFieldConfig = {
        key: property_id,
        type: 'input',
        templateOptions: {
          label: property.name,
          description: property.description
        }
      }
      // add Property Schema 
      Utils.deepCopy(property.schema, field);
      fields.push(field);
      if (property.defaultValue != null) {
        model[property_id] = property.defaultValue;
        if (property.name == 'Component-IDs') {
          model[property_id] = ["_sum", "ess0"];
        }
        // set costum component id
        if (property.name == 'Component-ID') {
          model[property_id] = 'ctrlApiModbusTcp1';
        }
      }
    }
    let properties = this.createProperties(fields, model);
    return properties;
  }

  // used to assemble properties out of created fields and model from 'gather' methods
  private createProperties(fields: FormlyFieldConfig[], model): { name: string, value: any }[] {
    let result: { name: string, value: any }[] = [];
    fields.forEach(field => {
      if (field.key == 'alias') {
        result.push({ name: 'alias', value: '' })
      }
      Object.keys(model).forEach(modelKey => {
        if (field.key == modelKey) {
          result.push({ name: field.key.toString(), value: model[modelKey] })
        }
      })
    })
    return result;
  }
}