import { Module } from '@nestjs/common';
import { ClientsController } from './clients/clients.controller';
import { ClientsService } from './clients/clients.service';
import { HealthController } from './health/health.controller';

@Module({
  controllers: [ClientsController, HealthController],
  providers: [ClientsService],
})
export class AppModule {}
