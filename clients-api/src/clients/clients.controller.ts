import { Controller, Get, NotFoundException, Param } from '@nestjs/common';
import { Client } from './client';
import { ClientsService } from './clients.service';

@Controller('clients')
export class ClientsController {
  constructor(private readonly clientsService: ClientsService) {}

  @Get(':clientId')
  findById(@Param('clientId') clientId: string): Client {
    const client = this.clientsService.findById(clientId);
    if (!client) {
      throw new NotFoundException('client not found');
    }

    return client;
  }
}
