import { Environment } from "../app/shared/type/environment";
import { DefaultTypes } from '../app/shared/service/defaulttypes';

class VA7282Environment extends Environment {
  public readonly production = true;

  public readonly url = "ws://" + location.hostname + ":8085";

  public readonly backend: DefaultTypes.Backend = "OpenEMS Backend";

  // public getCustomFields(config: Config) {
  //   return {
  //     sps0: {
  //       WaterLevel: {
  //         title: "Water level",
  //         unit: "centimeter"
  //       },
  //       GetPivotOn: {
  //         title: "Pivot",
  //         map: {
  //           0: 'Off',
  //           1: 'On'
  //         }
  //       },
  //       GetBorehole1On: {
  //         title: "Borehole 1",
  //         map: {
  //           0: 'Off',
  //           1: 'On'
  //         }
  //       },
  //       GetBorehole2On: {
  //         title: "Borehole 2",
  //         map: {
  //           0: 'Off',
  //           1: 'On'
  //         }
  //       },
  //       GetBorehole3On: {
  //         title: "Borehole 3",
  //         map: {
  //           0: 'Off',
  //           1: 'On'
  //         }
  //       },
  //       GetClima1On: {
  //         title: "Aircondition 1",
  //         map: {
  //           0: 'Off',
  //           1: 'On'
  //         }
  //       },
  //       GetClima2On: {
  //         title: "Aircondition 2",
  //         map: {
  //           0: 'Off',
  //           1: 'On'
  //         }
  //       },
  //       GetOfficeOn: {
  //         title: "Office",
  //         map: {
  //           0: 'Off',
  //           1: 'On'
  //         }
  //       },
  //       GetTraineeCentereOn: {
  //         title: "Trainee Center",
  //         map: {
  //           0: 'Off',
  //           1: 'On'
  //         }
  //       },
  //       AutomaticMode: {
  //         title: "Automatic Mode",
  //         map: {
  //           0: 'Off',
  //           1: 'On'
  //         }
  //       },
  //       ManualMode: {
  //         title: "Manual Mode",
  //         map: {
  //           0: 'Off',
  //           1: 'On'
  //         }
  //       },
  //       EmergencyStop: {
  //         title: "Emergency Stop",
  //         map: {
  //           0: 'Off',
  //           1: 'On'
  //         }
  //       },
  //       SwitchStatePivotPump: {
  //         title: "Switch State Pivot Pump",
  //         map: {
  //           0: 'Off',
  //           1: 'On'
  //         }
  //       },
  //       SwitchStatePivotDrive: {
  //         title: "Switch State Pivot Drive",
  //         map: {
  //           0: 'Off',
  //           1: 'On'
  //         }
  //       }
  //     }
  //   }
  // }
}

export const environment = new VA7282Environment();