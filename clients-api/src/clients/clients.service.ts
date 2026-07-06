import { Injectable } from '@nestjs/common';
import { Client, ClientSegment, TaxRegime } from './client';

@Injectable()
export class ClientsService {
  private readonly clients = new Map<string, Client>(
    [
      {
        clientId: 'CLI-99821',
        name: 'Distribuidora Andina S.A.S',
        segment: ClientSegment.Wholesale,
        taxRegime: TaxRegime.VATResponsible,
        region: 'Valle del Cauca',
        channel: 'B2B',
      },
      {
        clientId: 'CLI-10002',
        name: 'Comercializadora del Pacífico',
        segment: ClientSegment.Wholesale,
        taxRegime: TaxRegime.VATResponsible,
        region: 'Guayas',
        channel: 'B2B',
      },
      {
        clientId: 'CLI-10003',
        name: 'Minimercado La Central',
        segment: ClientSegment.Retail,
        taxRegime: TaxRegime.NotResponsible,
        region: 'Managua',
        channel: 'B2B',
      },
      {
        clientId: 'CLI-10004',
        name: 'Abastecedora Metropolitana',
        segment: ClientSegment.Wholesale,
        taxRegime: TaxRegime.VATResponsible,
        region: 'San José',
        channel: 'B2B',
      },
      {
        clientId: 'CLI-10005',
        name: 'Tienda El Encuentro',
        segment: ClientSegment.Retail,
        taxRegime: TaxRegime.NotResponsible,
        region: 'Antioquia',
        channel: 'B2B',
      },
    ].map((client) => [client.clientId, client]),
  );

  findById(clientId: string): Client | undefined {
    return this.clients.get(clientId);
  }
}
