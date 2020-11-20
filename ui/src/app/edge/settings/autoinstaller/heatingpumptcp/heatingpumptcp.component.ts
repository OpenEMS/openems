import { Component } from '@angular/core';
import { Edge, EdgeConfig, Service, ChannelAddress, Websocket, Utils } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { Subject, BehaviorSubject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isUndefined } from 'util';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { GetNetworkConfigRequest } from '../../network/getNetworkConfigRequest';
import { GetNetworkConfigResponse } from '../../network/getNetworkConfigResponse';
import { SetNetworkConfigRequest } from '../../network/setNetworkConfigRequest';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: HeatingpumpTcpInstallerComponent.SELECTOR,
  templateUrl: './heatingpumptcp.component.html'
})
export class HeatingpumpTcpInstallerComponent {

  public checkingState: boolean = false;
  public loading = true;
  public running = false;
  public showInit: boolean = false;
  public appWorking: BehaviorSubject<boolean> = new BehaviorSubject(false);
  public progressPercentage: number = 0;

  public loadingStrings: { string: string, type: string }[] = [];
  public subscribedChannels: ChannelAddress[] = [];
  public components: EdgeConfig.Component[] = [];

  public edge: Edge = null;
  public config: EdgeConfig = null;
  private stopOnDestroy: Subject<void> = new Subject<void>();

  private static readonly SELECTOR = "heatingpumpTcpInstaller";

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
    })
      .then(() => {

        switch (this.gatherAddedComponents().length) {
          case 0:
            this.showInit = true;
            this.loading = false;
            break;

          case 1: case 2: case 3:
            this.loadingStrings.push({ string: 'Teile dieser App sind bereits installiert', type: 'setup' });
            setTimeout(() => {
              this.addHeatingpumpComponents();
            }, 2000);
            break;

          case 4:
            this.addHeatingpumpComponents();
            this.checkingState = true;
            break;
        }
      });
  }

  // used to assemble properties out of created fields and model from 'gather' methods
  private createProperties(fields: FormlyFieldConfig[], model): { name: string, value: any }[] {
    let result: { name: string, value: any }[] = [];
    fields.forEach(field => {
      if (field.key == 'alias') {
        result.push({ name: 'alias', value: 'Wärmepumpe' })
      }
      Object.keys(model).forEach(modelKey => {
        if (field.key == modelKey) {
          result.push({ name: field.key.toString(), value: model[modelKey] })
        }
      })
    })
    return result;
  }

  private gatherType(config: EdgeConfig): { name: string, value: any }[] {
    let factoryId = 'IO.KMtronic'
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
        // set costum modbus-id
        if (property.name == 'Modbus-ID') {
          model[property_id] = 'modbus10';
        }
      }
    }
    let properties = this.createProperties(fields, model);
    return properties;
  }

  private gatherFirstApp(config: EdgeConfig): { name: string, value: any }[] {
    let factoryId = 'Controller.ChannelThreshold';
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
      if (property.name == 'Hysteresis') {
        model[property_id] = 5;
      }
      if (property.name == 'Low threshold') {
        model[property_id] = 0;
      }
      if (property.name == 'High threshold') {
        model[property_id] = 40;
      }
      if (property.name == 'Input Channel') {
        model[property_id] = '_sum/EssSoc';
      }
      if (property.name == 'Output Channel') {
        model[property_id] = 'io0/Relay2';
      }
      if (property.defaultValue != null) {
        model[property_id] = property.defaultValue;
        if (property.name == 'Component-ID') {
          model[property_id] = 'ctrlChannelThreshold2';
        }
      }
    }
    let properties = this.createProperties(fields, model);
    return properties;
  }

  private gatherSecondApp(config: EdgeConfig): { name: string, value: any }[] {
    let factoryId = 'Controller.ChannelThreshold';
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
      if (property.name == 'Hysteresis') {
        model[property_id] = 5;
      }
      if (property.name == 'Low threshold') {
        model[property_id] = 80;
      }
      if (property.name == 'High threshold') {
        model[property_id] = 100;
      }
      if (property.name == 'Input Channel') {
        model[property_id] = '_sum/EssSoc';
      }
      if (property.name == 'Output Channel') {
        model[property_id] = 'io0/Relay3';
      }
      if (property.defaultValue != null) {
        model[property_id] = property.defaultValue;
        if (property.name == 'Component-ID') {
          model[property_id] = 'ctrlChannelThreshold3';
        }
      }
    }
    let properties = this.createProperties(fields, model);
    return properties;
  }

  private gatherCommunication(config: EdgeConfig): { name: string, value: any }[] {
    let factoryId = 'Bridge.Modbus.Tcp';
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
      if (property.name == 'IP-Address') {
        model[property_id] = '192.168.1.199';
      }
      if (property.defaultValue != null) {
        model[property_id] = property.defaultValue;
        // set costum component id
        if (property.name == 'Component-ID') {
          model[property_id] = 'modbus10';
        }
      }
    }
    let properties = this.createProperties(fields, model);
    return properties;
  }

  /**
   * Main method, to add all required components.
   */
  public addHeatingpumpComponents() {

    let addedComponents: number = 0;

    this.loading = true;
    this.showInit = false;

    // Adding modbus tcp bridge 
    this.loadingStrings.push({ string: 'Versuche Bridge.Modbus.Tcp hinzuzufügen..', type: 'setup' });
    this.edge.createComponentConfig(this.websocket, 'Bridge.Modbus.Tcp', this.gatherCommunication(this.config)).then(() => {
      setTimeout(() => {
        this.loadingStrings.push({ string: 'Bridge.Modbus.Tcp wurde hinzugefügt', type: 'success' });
        addedComponents += 1;
      }, 3000);
    }).catch(reason => {
      if (reason.error.code == 1) {
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Bridge.Modbus.Tcp existiert bereits', type: 'success' });
          addedComponents += 1;
        }, 3000);
        return;
      }
      setTimeout(() => {
        this.loadingStrings.push({ string: 'Fehler Bridge.Modbus.Tcp hinzuzufügen', type: 'danger' });
        addedComponents += 1;
      }, 3000);
    });

    // Adding kmtronic relay board
    setTimeout(() => {
      this.loadingStrings.push({ string: 'Versuche IO.KMtronic hinzuzufügen..', type: 'setup' });
      this.edge.createComponentConfig(this.websocket, 'IO.KMtronic', this.gatherType(this.config)).then(() => {
        setTimeout(() => {
          this.loadingStrings.push({ string: 'IO.KMtronic wurde hinzugefügt', type: 'success' });
          addedComponents += 1;
        }, 1000);
      }).catch(reason => {
        if (reason.error.code == 1) {
          setTimeout(() => {
            this.loadingStrings.push({ string: 'IO.KMtronic existiert bereits', type: 'success' });
            addedComponents += 1;
          }, 1000);
          return;
        }
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Fehler IO.KMtronic hinzuzufügen', type: 'danger' });
          addedComponents += 1;
        }, 1000);
      });
    }, 4000);

    // Adding channel threshold controller 1
    setTimeout(() => {
      this.loadingStrings.push({ string: 'Versuche Controller.ChannelThreshold 1 hinzuzufügen..', type: 'setup' });

      this.edge.createComponentConfig(this.websocket, 'Controller.ChannelThreshold', this.gatherFirstApp(this.config)).then(() => {
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Controller.ChannelThreshold 1 wurde hinzugefügt', type: 'success' });
          addedComponents += 1;
        }, 1000);
      }).catch(reason => {
        if (reason.error.code == 1) {
          setTimeout(() => {
            this.loadingStrings.push({ string: 'Controller.ChannelThreshold 1 existiert bereits', type: 'success' });
            addedComponents += 1;
          }, 1000);
          return;
        }
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Fehler Controller.ChannelThreshold 1 hinzuzufügen', type: 'danger' });
          addedComponents += 1;
        }, 1000);
      });
    }, 6000);

    // Adding channel threshold controller 2
    setTimeout(() => {
      this.edge.createComponentConfig(this.websocket, 'Controller.ChannelThreshold', this.gatherSecondApp(this.config)).then(() => {

        this.loadingStrings.push({ string: 'Versuche Controller.ChannelThreshold 2 hinzuzufügen..', type: 'setup' });
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Controller.ChannelThreshold 2 wurde hinzugefügt', type: 'success' });
          addedComponents += 1;
        }, 1000);
      }).catch(reason => {
        if (reason.error.code == 1) {
          setTimeout(() => {
            this.loadingStrings.push({ string: 'Controller.ChannelThreshold 2 existiert bereits', type: 'success' });
            addedComponents += 1;
          }, 1000);
          return;
        }
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Fehler Controller.ChannelThreshold 2 hinzuzufügen', type: 'danger' });
          addedComponents += 1;
        }, 1000);
      });
    }, 8000);

    var regularComponentsInterval = setInterval(() => {
      while (addedComponents == 4) {
        this.progressPercentage = 0.4;
        clearInterval(regularComponentsInterval);
        break;
      }
    }, 300)

    setTimeout(() => {
      this.loadingStrings = [];
      this.loadingStrings.push({ string: 'Versuche statische IP-Adresse anzulegen..', type: 'setup' });
    }, 16000);

    setTimeout(() => {

      // Adding the ip address
      addedComponents += this.addIpAddress('eth0', '192.168.1.198/28') == true ? 1 : 0;
    }, 22000);

    var ipAddressInterval = setInterval(() => {
      while (addedComponents == 5) {
        this.progressPercentage = 0.6;
        clearInterval(ipAddressInterval);
        break;
      }
    }, 300)

    setTimeout(() => {
      this.checkConfiguration();
    }, 27000);
  }

  private gatherAddedComponents(): EdgeConfig.Component[] {
    let result = [];
    this.config.getComponentsByFactory('Bridge.Modbus.Tcp').forEach(component => {
      if (component.id == 'modbus10') {
        result.push(component)
      }
    })
    this.config.getComponentsByFactory('IO.KMtronic').forEach(component => {
      if (component.properties['modbus.id'] == 'modbus10') {
        result.push(component)
      }
    })
    this.config.getComponentsByFactory('Controller.ChannelThreshold').forEach(component => {
      if (component.id == 'ctrlChannelThreshold2' || component.id == 'ctrlChannelThreshold3') {
        result.push(component)
      }
    })
    return result
  }

  private gatherAddedComponentsIntoArray() {
    this.config.getComponentsByFactory('Bridge.Modbus.Tcp').forEach(component => {
      if (component.id == 'modbus10') {
        this.components.push(component)
      }
    })
    this.config.getComponentsByFactory('IO.KMtronic').forEach(component => {
      if (component.properties['modbus.id'] == 'modbus10') {
        this.components.push(component)
      }
    })
    this.config.getComponentsByFactory('Controller.ChannelThreshold').forEach(component => {
      if (component.id == 'ctrlChannelThreshold2' || component.id == 'ctrlChannelThreshold3') {
        this.components.push(component)
      }
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
      if (workState == 4) {
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
        if (this.gatherAddedComponents().length >= 4) {
          this.loadingStrings.push({ string: 'Komponenten korrekt hinzugefügt', type: 'success' });
          this.progressPercentage = 0.95;
          this.gatherAddedComponentsIntoArray();
          return
        }
        this.loadingStrings.push({ string: 'Es konnten nicht alle Komponenten korrekt hinzugefügt werden', type: 'danger' });
        this.loadingStrings.push({ string: 'Bitte Prozess neu starten oder gegebenenfalls manuell korrigieren', type: 'danger' });
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
    this.edge.subscribeChannels(this.websocket, 'heatingpumpTcpInstaller', this.subscribedChannels);
    setTimeout(() => {
      this.loading = false;
      this.running = true;
    }, 5000);
  }

  ionViewDidLeave() {
    this.edge.unsubscribeChannels(this.websocket, 'heatingpumpTcpInstaller');
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }
}