import { ClientSegment, TaxRegime } from './client';
import { ClientsService } from './clients.service';

describe('ClientsService', () => {
  let service: ClientsService;

  beforeEach(() => {
    service = new ClientsService();
  });

  it('contains the required client catalog', () => {
    const clientIds = ['CLI-99821', 'CLI-10002', 'CLI-10003', 'CLI-10004', 'CLI-10005'];

    for (const clientId of clientIds) {
      expect(service.findById(clientId)).toBeDefined();
    }
  });

  it('returns the fiscal information for the sample client', () => {
    const client = service.findById('CLI-99821');

    expect(client).toMatchObject({
      segment: ClientSegment.Wholesale,
      taxRegime: TaxRegime.VATResponsible,
      region: 'Valle del Cauca',
    });
  });

  it('returns undefined for an unknown client', () => {
    expect(service.findById('CLI-99999')).toBeUndefined();
  });
});
