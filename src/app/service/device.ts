export class Device {
  name: string;
  natures: string[];
}

export class EssDevice extends Device {
  soc: number;
  activePower: number;
}