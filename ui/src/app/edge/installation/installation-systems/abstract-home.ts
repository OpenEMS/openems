import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { SetupProtocol, SubmitSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';
import { ComponentData } from 'src/app/shared/type/componentData';

import { ComponentConfigurator, ConfigurationMode } from '../views/configuration-execute/component-configurator';
import { SafetyCountry } from '../views/configuration-execute/safety-country';
import { FeedInSetting, FeedInType } from '../views/protocol-feed-in-limitation/protocol-feed-in-limitation.component';
import { Ibn } from './abstract-ibn';

export type TowerData = {
  label: string;
  value: string;
};

export abstract class AbstractHomeIbn extends Ibn {

  // Protocol-pv-component
  public batteryInverter?: {
    shadowManagementDisabled?: boolean;
  };

  // protocol-serial-numbers
  public battery?: {
    serialNumbers?: {
      tower0?: TowerData[];
      tower1?: TowerData[];
      tower2?: TowerData[];
    };
  };

  // configuration-emergency-reserve
  public emergencyReserve?: {
    isEnabled: boolean;
    isReserveSocEnabled: boolean;
    value: number;
  } = {
      isEnabled: true,
      value: 20,
      isReserveSocEnabled: false,
    };

  // pre-installation
  public readonly imageUrl = 'assets/img/Home-Typenschild-web.jpg';

  public setRequiredControllers() {
    let requiredControllerIds: string[];
    if (this.emergencyReserve.isEnabled) {
      requiredControllerIds = [
        'ctrlEmergencyCapacityReserve0',
        'ctrlGridOptimizedCharge0',
        'ctrlEssSurplusFeedToGrid0',
        'ctrlBalancing0',
      ];
    } else {
      requiredControllerIds = [
        'ctrlGridOptimizedCharge0',
        'ctrlEssSurplusFeedToGrid0',
        'ctrlBalancing0',
      ];
    }

    this.requiredControllerIds = requiredControllerIds;
  }


  public addCustomBatteryData(batteryData: ComponentData[]) {
    batteryData.push({
      label: 'Notstromfunktion aktiviert?',
      value: this.emergencyReserve.isEnabled ? 'ja' : 'nein',
    });

    if (this.emergencyReserve.isEnabled) {
      batteryData.push({
        label: 'Notstromfunktion Wert',
        value: this.emergencyReserve.value,
      });
    }

    return batteryData;
  }

  public addCustomBatteryInverterData(batteryInverterData: ComponentData[]) {
    const feedInLimitation = this.feedInLimitation;

    feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
      ? batteryInverterData.push(
        {
          label: 'Maximale Einspeiseleistung',
          value: feedInLimitation.maximumFeedInPower,
        },
        {
          label: 'Schattenmanagement deaktiviert',
          value: this.batteryInverter?.shadowManagementDisabled ? 'ja' : 'nein',
        },
        {
          label: 'Typ',
          value: feedInLimitation.feedInSetting ?? FeedInSetting.Undefined
        }
      )
      : batteryInverterData.push(
        {
          label: "Rundsteuerempfänger aktiviert",
          value: "ja"
        })

    if (
      feedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor
    ) {
      batteryInverterData.push({
        label: 'Cos ɸ Festwert ',
        value: feedInLimitation.fixedPowerFactor,
      });
    }
    return batteryInverterData;
  }

  public addCustomPvData(pvData: ComponentData[]) {
    let dcNr = 1;
    for (const dc of [this.pv.dc1, this.pv.dc2]) {
      if (dc.isSelected) {
        pvData = pvData.concat([
          { label: 'Alias MPPT' + dcNr, value: dc.alias },
          { label: 'Wert MPPT' + dcNr, value: dc.value },
        ]);
        if (dc.orientation) {
          pvData.push({
            label: 'Ausrichtung MPPT' + dcNr,
            value: dc.orientation,
          });
        }
        if (dc.moduleType) {
          pvData.push({ label: 'Modultyp MPPT' + dcNr, value: dc.moduleType });
        }
        if (dc.modulesPerString) {
          pvData.push({
            label: 'Anzahl PV-Module MPPT' + dcNr,
            value: dc.modulesPerString,
          });
        }
        dcNr++;
      }
    }
    return pvData;
  }

  public getProtocol(edge: Edge, websocket: Websocket): Promise<string> {
    const installer = this.installer;
    const customer = this.customer;
    const battery = this.battery;
    const feedInLimitation = this.feedInLimitation;
    const pv = this.pv;
    const emergencyReserve = this.emergencyReserve;
    const lineSideMeterFuse = this.lineSideMeterFuse;
    const serialNumbers = battery.serialNumbers;
    const dc1 = pv.dc1;
    const dc2 = pv.dc2;
    const ac = pv.ac;

    const installerObj: any = {
      firstname: installer.firstName,
      lastname: installer.lastName,
    };

    const customerObj: any = {
      firstname: customer.firstName,
      lastname: customer.lastName,
      email: customer.email,
      phone: customer.phone,
      address: {
        street: customer.street,
        city: customer.city,
        zip: customer.zip,
        country: customer.country,
      },
    };

    if (customer.isCorporateClient) {
      customerObj.company = {
        name: customer.companyName,
      };
    }

    const protocol: SetupProtocol = {
      fems: {
        id: edge.id,
      },
      installer: installerObj,
      customer: customerObj,
    };

    // If location data is different to customer data, the location
    // data gets sent too
    if (!this.location.isEqualToCustomerData) {
      const location = this.location;
      protocol.location = {
        firstname: location.firstName,
        lastname: location.lastName,
        email: location.email,
        phone: location.phone,
        address: {
          street: location.street,
          city: location.city,
          zip: location.zip,
          country: location.country,
        },
        company: {
          name: location.companyName,
        },
      };
    }

    protocol.items = [];
    protocol.items.push({
      category: 'Angaben zu Notstrom',
      name: 'Notstrom?',
      value: emergencyReserve.isEnabled ? 'ja' : 'nein',
    });

    if (emergencyReserve.isEnabled) {
      protocol.items.push({
        category: 'Angaben zu Notstrom',
        name: 'Notstromreserve [%]',
        value: emergencyReserve.value ? emergencyReserve.value.toString() : '',
      });
    }

    let lineSideMeterFuseValue: number;
    if (lineSideMeterFuse.fixedValue === -1) {
      lineSideMeterFuseValue = lineSideMeterFuse.otherValue;
    } else {
      lineSideMeterFuseValue = lineSideMeterFuse.fixedValue;
    }

    protocol.items.push({
      category: 'Vorsicherung Hausanschlusszähler',
      name: 'Wert [A]',
      value: lineSideMeterFuseValue ? lineSideMeterFuseValue.toString() : '',
    });

    // DC-PV 1
    if (dc1.isSelected) {
      protocol.items.push({
        category: 'DC-PV-Installation',
        name: 'Alias MPPT1',
        value: dc1.alias,
      });

      protocol.items.push({
        category: 'DC-PV-Installation',
        name: 'Wert MPPT1 [Wp]',
        value: dc1.value ? dc1.value.toString() : '',
      });

      dc1.orientation && protocol.items.push({
        category: 'DC-PV-Installation',
        name: 'Ausrichtung MPPT1',
        value: dc1.orientation,
      });

      dc1.moduleType && protocol.items.push({
        category: 'DC-PV-Installation',
        name: 'Modultyp MPPT1',
        value: dc1.moduleType,
      });

      dc1.modulesPerString && protocol.items.push({
        category: 'DC-PV-Installation',
        name: 'Anzahl PV-Module MPPT1',
        value: dc1.modulesPerString ? dc1.modulesPerString.toString() : '',
      });
    }

    // DC-PV 2
    if (dc2.isSelected) {
      protocol.items.push({
        category: 'DC-PV-Installation',
        name: 'Wert MPPT2 [Wp]',
        value: dc2.value ? dc2.value.toString() : '',
      });

      protocol.items.push({
        category: 'DC-PV-Installation',
        name: 'Alias MPPT2',
        value: dc2.alias,
      });

      dc2.orientation && protocol.items.push({
        category: 'DC-PV-Installation',
        name: 'Ausrichtung MPPT2',
        value: dc2.orientation,
      });

      dc2.moduleType && protocol.items.push({
        category: 'DC-PV-Installation',
        name: 'Modultyp MPPT2',
        value: dc2.moduleType,
      });

      dc2.modulesPerString && protocol.items.push({
        category: 'DC-PV-Installation',
        name: 'Anzahl PV-Module MPPT2',
        value: dc2.modulesPerString ? dc2.modulesPerString.toString() : '',
      });
    }

    protocol.items.push({
      category: 'Einspeisemanagement',
      name: 'Rundsteuerempfänger aktiviert?',
      value: feedInLimitation.feedInType == FeedInType.EXTERNAL_LIMITATION
        ? "ja"
        : "nein"
    });
    protocol.items.push({
      category: 'Einspeisemanagement',
      name: 'Dynamische Begrenzung der Einspeisung aktiviert?',
      value: feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
        ? "ja"
        : "nein"
    });

    if (feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION) {
      protocol.items.push({
        category: 'Einspeisemanagement',
        name: 'Maximale Einspeiseleistung [W]',
        value: feedInLimitation.maximumFeedInPower
          ? feedInLimitation.maximumFeedInPower.toString()
          : (0).toString(),
      });

      protocol.items.push({
        category: 'Einspeisemanagement',
        name: 'Typ',
        value: feedInLimitation.feedInSetting,
      });

      if (feedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor) {
        protocol.items.push({
          category: 'Einspeisemanagement',
          name: 'Cos φ Festwert',
          value: feedInLimitation.fixedPowerFactor,
        });
      }

    }

    for (let index = 0; index < ac.length; index++) {
      const element = ac[index];
      const label = 'AC' + (index + 1);

      protocol.items.push({
        category: 'Zusätzliche AC-Erzeuger',
        name: 'Alias ' + label,
        value: element.alias,
      });

      protocol.items.push({
        category: 'Zusätzliche AC-Erzeuger',
        name: 'Wert ' + label + ' [Wp]',
        value: element.value ? element.value.toString() : '',
      });

      element.orientation && protocol.items.push({
        category: 'Zusätzliche AC-Erzeuger',
        name: 'Ausrichtung ' + label,
        value: element.orientation,
      });

      element.moduleType && protocol.items.push({
        category: 'Zusätzliche AC-Erzeuger',
        name: 'Modultyp ' + label,
        value: element.moduleType,
      });

      element.modulesPerString && protocol.items.push({
        category: 'Zusätzliche AC-Erzeuger',
        name: 'Anzahl PV-Module ' + label,
        value: element.modulesPerString
          ? element.modulesPerString.toString()
          : '',
      });

      element.meterType && protocol.items.push({
        category: 'Zusätzliche AC-Erzeuger',
        name: 'Zählertyp ' + label,
        value: element.meterType,
      });

      element.modbusCommunicationAddress && protocol.items.push({
        category: 'Zusätzliche AC-Erzeuger',
        name: 'Modbus Kommunikationsadresse ' + label,
        value: element.modbusCommunicationAddress
          ? element.modbusCommunicationAddress.toString()
          : '',
      });
    }

    protocol.items.push({
      category: 'FEMS',
      name: 'FEMS Nummer',
      value: edge.id,
    });

    // Speichersystemkomponenten
    protocol.lots = [];
    for (const serialNumber of serialNumbers.tower0) {
      if (serialNumber.value !== null && serialNumber.value !== '') {
        protocol.lots.push({
          category: 'Speichersystemkomponenten',
          name: serialNumber.label + ' Seriennummer',
          serialNumber: serialNumber.value,
        });
      }
    }

    // Batterieturm 2
    for (const serialNumber of serialNumbers.tower1) {
      if (serialNumber.value !== null && serialNumber.value !== '') {
        protocol.lots.push({
          category: 'Batterieturm 2',
          name: serialNumber.label + ' Seriennummer',
          serialNumber: serialNumber.value,
        });
      }
    }

    // Batterieturm 3
    for (const serialNumber of serialNumbers.tower2) {
      if (serialNumber.value !== null && serialNumber.value !== '') {
        protocol.lots.push({
          category: 'Batterieturm 3',
          name: serialNumber.label + ' Seriennummer',
          serialNumber: serialNumber.value,
        });
      }
    }

    return new Promise((resolve, reject) => {
      websocket
        .sendRequest(new SubmitSetupProtocolRequest({ protocol }))
        .then((response: JsonrpcResponseSuccess) => {
          resolve(response.result['setupProtocolId']);
        })
        .catch((reason) => {
          reject(reason);
        });
    });
  }

  public getComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket, service?: Service): ComponentConfigurator {
    const componentConfigurator: ComponentConfigurator =
      new ComponentConfigurator(edge, config, websocket);
    // modbus1
    componentConfigurator.add({
      factoryId: 'Bridge.Modbus.Serial',
      componentId: 'modbus1',
      alias: 'Kommunikation mit dem Batterie-Wechselrichter',
      properties: [
        { name: 'enabled', value: true },
        { name: 'portName', value: '/dev/busUSB2' },
        { name: 'baudRate', value: 9600 },
        { name: 'databits', value: 8 },
        { name: 'stopbits', value: 'ONE' },
        { name: 'parity', value: 'NONE' },
        { name: 'logVerbosity', value: 'NONE' },
        { name: 'invalidateElementsAfterReadErrors', value: 1 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // modbus0
    componentConfigurator.add({
      factoryId: 'Bridge.Modbus.Serial',
      componentId: 'modbus0',
      alias: 'Kommunikation mit der Batterie',
      properties: [
        { name: 'enabled', value: true },
        { name: 'portName', value: '/dev/busUSB1' },
        { name: 'baudRate', value: 19200 },
        { name: 'databits', value: 8 },
        { name: 'stopbits', value: 'ONE' },
        { name: 'parity', value: 'NONE' },
        { name: 'logVerbosity', value: 'NONE' },
        { name: 'invalidateElementsAfterReadErrors', value: 1 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // meter0
    componentConfigurator.add({
      factoryId: 'GoodWe.Grid-Meter',
      componentId: 'meter0',
      alias: 'Netzzähler',
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // io0
    componentConfigurator.add({
      factoryId: 'IO.KMtronic.4Port',
      componentId: 'io0',
      alias: 'Relaisboard',
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus0' },
        { name: 'modbusUnitId', value: 2 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // battery0
    componentConfigurator.add({
      factoryId: 'Battery.Fenecon.Home',
      componentId: 'battery0',
      alias: 'Batterie',
      properties: [
        { name: 'enabled', value: true },
        { name: 'startStop', value: 'AUTO' },
        { name: 'modbus.id', value: 'modbus0' },
        { name: 'modbusUnitId', value: 1 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // Determine safety country
    let safetyCountry: SafetyCountry;
    if (this.location.isEqualToCustomerData) {
      safetyCountry = SafetyCountry.getSafetyCountry(this.customer.country);
    } else {
      safetyCountry = SafetyCountry.getSafetyCountry(this.location.country);
    }

    // Determine feed-in-setting
    let feedInSetting: FeedInSetting;
    const feedInLimitation = this.feedInLimitation;
    if (
      feedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor
    ) {
      feedInSetting = feedInLimitation.fixedPowerFactor;
    } else {
      feedInSetting = feedInLimitation.feedInSetting;
    }

    // batteryInverter0
    componentConfigurator.add({
      factoryId: 'GoodWe.BatteryInverter',
      componentId: 'batteryInverter0',
      alias: 'Batterie-Wechselrichter',
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
        { name: 'safetyCountry', value: safetyCountry },
        {
          name: 'backupEnable',
          value: this.emergencyReserve.isEnabled ? 'ENABLE' : 'DISABLE',
        },
        { name: 'feedPowerEnable', value: 'ENABLE' },
        {
          name: 'feedPowerPara',
          value: feedInLimitation.maximumFeedInPower,
        },
        { name: 'setfeedInPowerSettings', value: feedInSetting },
        { name: 'emsPowerMode', value: 'UNDEFINED' },
        { name: 'emsPowerSet', value: -1 },
        {
          name: 'mpptForShadowEnable',
          value: this.batteryInverter?.shadowManagementDisabled
            ? 'DISABLE'
            : 'ENABLE',
        },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // meter1
    const acArray = this.pv.ac;
    const isAcCreated: boolean = acArray.length >= 1;

    // TODO if more than 1 meter should be created, this logic must be changed
    const acAlias = isAcCreated ? acArray[0].alias : '';
    const acModbusUnitId = isAcCreated
      ? acArray[0].modbusCommunicationAddress
      : 0;

    componentConfigurator.add({
      factoryId: 'Meter.Socomec.Threephase',
      componentId: 'meter1',
      alias: acAlias,
      properties: [
        { name: 'enabled', value: true },
        { name: 'type', value: 'PRODUCTION' },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: acModbusUnitId },
        { name: 'invert', value: false },
      ],
      mode: isAcCreated
        ? ConfigurationMode.RemoveAndConfigure
        : ConfigurationMode.RemoveOnly,
    });

    // charger0
    componentConfigurator.add({
      factoryId: 'GoodWe.Charger-PV1',
      componentId: 'charger0',
      alias: this.pv.dc1.alias,
      properties: [
        { name: 'enabled', value: true },
        { name: 'essOrBatteryInverter.id', value: 'batteryInverter0' },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
      ],
      mode: this.pv.dc1.isSelected
        ? ConfigurationMode.RemoveAndConfigure
        : ConfigurationMode.RemoveOnly,
    });

    // charger1
    componentConfigurator.add({
      factoryId: 'GoodWe.Charger-PV2',
      componentId: 'charger1',
      alias: this.pv.dc2.alias,
      properties: [
        { name: 'enabled', value: true },
        { name: 'essOrBatteryInverter.id', value: 'batteryInverter0' },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
      ],
      mode: this.pv.dc2.isSelected
        ? ConfigurationMode.RemoveAndConfigure
        : ConfigurationMode.RemoveOnly,
    });

    // ess0
    componentConfigurator.add({
      factoryId: 'Ess.Generic.ManagedSymmetric',
      componentId: 'ess0',
      alias: 'Speichersystem',
      properties: [
        { name: 'enabled', value: true },
        { name: 'startStop', value: 'START' },
        { name: 'batteryInverter.id', value: 'batteryInverter0' },
        { name: 'battery.id', value: 'battery0' },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // predictor0
    componentConfigurator.add({
      factoryId: 'Predictor.PersistenceModel',
      componentId: 'predictor0',
      alias: 'Prognose',
      properties: [
        { name: 'enabled', value: true },
        {
          name: 'channelAddresses',
          value: ['_sum/ProductionActivePower', '_sum/ConsumptionActivePower'],
        },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // ctrlGridOptimizedCharge0
    componentConfigurator.add({
      factoryId: 'Controller.Ess.GridOptimizedCharge',
      componentId: 'ctrlGridOptimizedCharge0',
      alias: 'Netzdienliche Beladung',
      properties: [
        { name: 'enabled', value: true },
        { name: 'ess.id', value: 'ess0' },
        { name: 'meter.id', value: 'meter0' },
        { name: 'sellToGridLimitEnabled', value: true },
        {
          name: 'maximumSellToGridPower',
          value: feedInLimitation.maximumFeedInPower,
        },
        { name: 'delayChargeRiskLevel', value: 'MEDIUM' },
        { name: 'mode', value: 'AUTOMATIC' },
        { name: 'manualTargetTime', value: '17:00' },
        { name: 'debugMode', value: false },
        { name: 'sellToGridLimitRampPercentage', value: 2 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // ctrlEssSurplusFeedToGrid0
    componentConfigurator.add({
      factoryId: 'Controller.Ess.Hybrid.Surplus-Feed-To-Grid',
      componentId: 'ctrlEssSurplusFeedToGrid0',
      alias: 'Überschusseinspeisung',
      properties: [
        { name: 'enabled', value: true },
        { name: 'ess.id', value: 'ess0' },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // ctrlBalancing0
    componentConfigurator.add({
      factoryId: 'Controller.Symmetric.Balancing',
      componentId: 'ctrlBalancing0',
      alias: 'Eigenverbrauchsoptimierung',
      properties: [
        { name: 'enabled', value: true },
        { name: 'ess.id', value: 'ess0' },
        { name: 'meter.id', value: 'meter0' },
        { name: 'targetGridSetpoint', value: 0 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    const emergencyReserve = this.emergencyReserve;
    componentConfigurator.add({
      factoryId: 'GoodWe.EmergencyPowerMeter',
      componentId: 'meter2',
      alias: 'Notstromverbraucher',
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
      ],
      mode: emergencyReserve.isEnabled
        ? ConfigurationMode.RemoveAndConfigure
        : ConfigurationMode.RemoveOnly,
    });

    componentConfigurator.add({
      factoryId: 'Controller.Ess.EmergencyCapacityReserve',
      componentId: 'ctrlEmergencyCapacityReserve0',
      alias: 'Ansteuerung der Notstromreserve',
      properties: [
        { name: 'enabled', value: true },
        { name: 'ess.id', value: 'ess0' },
        { name: 'mode', value: emergencyReserve.isReserveSocEnabled },
        {
          name: 'reserveSoc',
          value: emergencyReserve.value ?? 5 /* minimum allowed value */,
        },
      ],
      mode: emergencyReserve.isEnabled
        ? ConfigurationMode.RemoveAndConfigure
        : ConfigurationMode.RemoveOnly,
    });
    return componentConfigurator;
  }
}
