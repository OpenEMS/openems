export class Device {
  _name: string;
  _natures: string[];

  set(field: string, value: number) {
    console.log("Set "+field+" to " + value);
  }
}