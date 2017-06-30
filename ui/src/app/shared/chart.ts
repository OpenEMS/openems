export interface Dataset {
  label: string;
  data: number[];
}

export const EMPTY_DATASET = [{
  label: "",
  data: []
}];