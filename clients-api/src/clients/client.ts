export enum ClientSegment {
  Wholesale = 'MAYORISTA',
  Retail = 'MINORISTA',
}

export enum TaxRegime {
  VATResponsible = 'RESPONSABLE_IVA',
  NotResponsible = 'NO_RESPONSABLE',
}

export interface Client {
  clientId: string;
  name: string;
  segment: ClientSegment;
  taxRegime: TaxRegime;
  region: string;
  channel: string;
}
