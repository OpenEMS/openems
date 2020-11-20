import { Component } from '@angular/core';
import { Edge, EdgeConfig, Service, ChannelAddress, Websocket, Utils } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { Subject, BehaviorSubject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isUndefined } from 'util';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { GetNetworkConfigResponse } from '../../network/getNetworkConfigResponse';
import { GetNetworkConfigRequest } from '../../network/getNetworkConfigRequest';
import { SetNetworkConfigRequest } from '../../network/setNetworkConfigRequest';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: EvcsInstallerComponent.SELECTOR,
  templateUrl: './evcs.component.html'
})
export class EvcsInstallerComponent {

  public checkingState: boolean = false;
  public loading = true;
  public running = false;
  public showInit: boolean = false;
  public appWorking: BehaviorSubject<boolean> = new BehaviorSubject(false);
  public progressPercentage: number = 0;

  public loadingStrings: { string: string, type: string }[] = [];
  public subscribedChannels: ChannelAddress[] = [];
  public components: EdgeConfig.Component[] = [];

  public evcsId = null;
  public edge: Edge = null;
  public config: EdgeConfig = null;
  private stopOnDestroy: Subject<void> = new Subject<void>();

  private static readonly SELECTOR = "evcsInstaller";

  constructor(
    private route: ActivatedRoute,
    public service: Service,
    public modalCtrl: ModalController,
    private websocket: Websocket,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('Automatische Installation', this.route).then(edge => {
      this.edge = edge;
    });
    this.service.getConfig().then(config => {
      this.config = config;
    }).then(() => {
      switch (this.gatherAddedComponents().length) {
        case 0: {
          this.showInit = true;
          this.loading = false;
          break;
        }
        case 1: {
          this.loadingStrings.push({ string: 'Teile dieser App sind bereits installiert', type: 'setup' });
          setTimeout(() => {
            this.addEVCSComponents();
          }, 2000);
          break;
        }
        case 2: {
          this.gatherAddedComponentsIntoArray();
          this.checkingState = true;
          break;
        }
      }
    });
  }

  // used to assemble properties out of created fields and model from 'gather' methods
  private createProperties(fields: FormlyFieldConfig[], model): { name: string, value: any }[] {
    let result: { name: string, value: any }[] = [];
    fields.forEach(field => {
      if (field.key == 'alias') {
        result.push({ name: 'alias', value: 'Ladestation' })
      }
      Object.keys(model).forEach(modelKey => {
        if (field.key == modelKey) {
          result.push({ name: field.key.toString(), value: model[modelKey] })
        }
      })
    })
    return result;
  }

  private gatherChargingStation(config: EdgeConfig): { name: string, value: any }[] {
    let factoryId = 'Evcs.Keba.KeContact'
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
        if (property.name == 'IP-Address') {
          model[property_id] = '192.168.25.11';
        }
      }
    }
    let properties = this.createProperties(fields, model);
    return properties;
  }

  private gatherController(config: EdgeConfig): { name: string, value: any }[] {
    let factoryId = 'Controller.Evcs';
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
      }
    }
    let properties = this.createProperties(fields, model);
    return properties;
  }

  /**
   * Main method, to add all required components.
   */
  public addEVCSComponents() {

    let addedComponents: number = 0;

    this.loading = true;
    this.showInit = false;

    this.loadingStrings.push({ string: 'Versuche Evcs.Keba.KeContact hinzuzufügen..', type: 'setup' });
    this.edge.createComponentConfig(this.websocket, 'Evcs.Keba.KeContact', this.gatherChargingStation(this.config)).then(() => {
      setTimeout(() => {
        this.loadingStrings.push({ string: 'Evcs.Keba.KeContact wurde hinzugefügt', type: 'success' });
        addedComponents += 1;
      }, 2000);
    }).catch(reason => {
      if (reason.error.code == 1) {
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Evcs.Keba.KeContact existiert bereits', type: 'success' });
          addedComponents += 1;
        }, 2000);
        return;
      }
      setTimeout(() => {
        this.loadingStrings.push({ string: 'Fehler Evcs.Keba.KeContact hinzuzufügen', type: 'danger' });
        addedComponents += 1;
      }, 2000);
    });

    setTimeout(() => {
      this.loadingStrings.push({ string: 'Versuche Controller.Evcs hinzuzufügen..', type: 'setup' });
      this.edge.createComponentConfig(this.websocket, 'Controller.Evcs', this.gatherController(this.config)).then(() => {
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Controller.Evcs wurde hinzugefügt', type: 'success' });
          addedComponents += 1;
        }, 1000);
      }).catch(reason => {
        if (reason.error.code == 1) {
          setTimeout(() => {
            this.loadingStrings.push({ string: 'Controller.Evcs existiert bereits', type: 'success' });
            addedComponents += 1;
          }, 1000);
          return;
        }
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Fehler Controller.Evcs hinzuzufügen', type: 'danger' });
          addedComponents += 1;
        }, 1000);
      });
    }, 3000);

    var percentageInterval = setInterval(() => {
      while (addedComponents == 2) {
        this.progressPercentage = 0.4;
        clearInterval(percentageInterval);
        break;
      }
    }, 300)

    setTimeout(() => {
      this.loadingStrings = [];
      this.loadingStrings.push({ string: 'Versuche statische IP-Adresse anzulegen..', type: 'setup' });
    }, 6000);

    setTimeout(() => {

      // Adding the ip address
      addedComponents += this.addIpAddress('eth0', '192.168.25.10/24') == true ? 1 : 0;
    }, 10000);

    var ipAddressInterval = setInterval(() => {
      while (addedComponents == 3) {
        this.progressPercentage = 0.6;
        clearInterval(ipAddressInterval);
        break;
      }
    }, 300)

    setTimeout(() => {
      this.checkConfiguration();
    }, 14000);
  }

  private gatherAddedComponents(): EdgeConfig.Component[] {
    let result = [];
    this.config.getComponentsByFactory('Evcs.Keba.KeContact').forEach(component => {
      result.push(component)
    })
    this.config.getComponentsByFactory('Controller.Evcs').forEach(component => {
      result.push(component)
    })
    return result
  }

  private gatherAddedComponentsIntoArray() {
    this.config.getComponentsByFactory('Controller.Evcs').forEach(component => {
      this.components.push(component)
    })
    this.config.getComponentsByFactory('Evcs.Keba.KeContact').forEach(component => {
      this.evcsId = component.id;
      this.components.push(component)
    })
    this.edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
      let workState = 0;
      this.components.forEach(component => {
        let state = currentData.channel[component.id + '/State'];
        if (!isUndefined(state)) {
          if (state == 0) {
            workState += 1;
          }
        }
      })
      if (workState == 2) {
        this.appWorking.next(true);
      } else {
        this.appWorking.next(false);
      }
    })
    this.subscribeOnAddedComponents();
  }

  /**
   * Adds an ip address.
   * Returns false if it is already present.
   * 
   * @param interfaceName Interface default 'eth0'
   * @param ip Ip that should be added
   */
  private addIpAddress(interfaceName: string, ip: string): boolean {

    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({ componentId: "_host", payload: new GetNetworkConfigRequest() })).then(response => {

        let result = (response as GetNetworkConfigResponse).result;
        if (result.interfaces[interfaceName].addresses.includes(ip)) {
          this.loadingStrings.push({ string: 'Statische IP-Adresse existiert bereits', type: 'success' });
          return false;
        } else {
          result.interfaces[interfaceName].addresses.push(ip);

          this.edge.sendRequest(this.websocket,
            new ComponentJsonApiRequest({
              componentId: "_host", payload: new SetNetworkConfigRequest(result)
            })).then(response => {
              this.loadingStrings.push({ string: 'Statische IP-Adresse wird hinzugefügt', type: 'success' });
              return true;
            }).catch(reason => {
              this.loadingStrings.push({ string: 'Fehler statische IP-Adresse hinzuzufügen', type: 'danger' });
              return true;
            })
        }
      })
    return false;
  }

  private checkConfiguration() {
    this.loadingStrings = [];
    this.loadingStrings.push({ string: 'Überprüfe ob Komponenten korrekt hinzugefügt wurden..', type: 'setup' });
    this.progressPercentage = 0.6;
    setTimeout(() => {
      this.service.getConfig().then(config => {
        this.config = config;
      }).then(() => {
        //TODO: Check it properly
        if (this.gatherAddedComponents().length >= 2) {
          this.loadingStrings.push({ string: 'Komponenten korrekt hinzugefügt', type: 'success' });
          this.progressPercentage = 0.95;
          this.gatherAddedComponentsIntoArray();
          return
        }
        this.loadingStrings.push({ string: 'Es konnten nicht alle Komponenten korrekt hinzugefügt werden', type: 'danger' });
        this.loadingStrings.push({ string: 'Bitte Neu starten oder manuell korrigieren', type: 'danger' });
      })
    }, 10000);
  }

  private subscribeOnAddedComponents() {
    this.loadingStrings.push({ string: 'Überprüfe Status der Komponenten..', type: 'setup' });
    this.components.forEach(component => {
      this.subscribedChannels.push(new ChannelAddress(component.id, 'State'));
      Object.keys(component.channels).forEach(channel => {
        if (component.channels[channel]['level']) {
          let levelChannel = new ChannelAddress(component.id, channel);
          this.subscribedChannels.push(levelChannel)
        }
      });
    })
    this.edge.subscribeChannels(this.websocket, 'evcsInstaller', this.subscribedChannels);
    setTimeout(() => {
      this.loading = false;
      this.running = true;
    }, 5000);
  }

  ionViewDidLeave() {
    this.edge.unsubscribeChannels(this.websocket, 'evcsInstaller');
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }
}