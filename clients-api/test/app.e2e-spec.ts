import { INestApplication } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import request from 'supertest';
import { AppModule } from '../src/app.module';

describe('Clients API', () => {
  let app: INestApplication;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  it('returns a client by id', async () => {
    await request(app.getHttpServer())
      .get('/clients/CLI-99821')
      .expect(200)
      .expect({
        clientId: 'CLI-99821',
        name: 'Distribuidora Andina S.A.S',
        segment: 'MAYORISTA',
        taxRegime: 'RESPONSABLE_IVA',
        region: 'Valle del Cauca',
        channel: 'B2B',
      });
  });

  it('returns 404 for an unknown client', async () => {
    await request(app.getHttpServer())
      .get('/clients/CLI-99999')
      .expect(404)
      .expect({
        message: 'client not found',
        error: 'Not Found',
        statusCode: 404,
      });
  });

  it('reports service health', async () => {
    await request(app.getHttpServer()).get('/health').expect(200).expect({ status: 'UP' });
  });
});
