import { Logger } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

const logger = new Logger('Bootstrap');

async function bootstrap(): Promise<void> {
  const port = readPort();
  const app = await NestFactory.create(AppModule);
  app.enableShutdownHooks();
  await app.listen(port, '0.0.0.0');
  logger.log(`Clients API listening on port ${port}`);
}

function readPort(): number {
  const value = process.env.CLIENTS_API_PORT?.trim();
  if (!value) {
    throw new Error('CLIENTS_API_PORT is required');
  }

  if (!/^\d+$/.test(value)) {
    throw new Error('CLIENTS_API_PORT must be a valid port');
  }

  const port = Number(value);
  if (port < 1 || port > 65535) {
    throw new Error('CLIENTS_API_PORT must be between 1 and 65535');
  }

  return port;
}

void bootstrap().catch((error: unknown) => {
  logger.error(error instanceof Error ? error.message : 'Clients API failed to start');
  process.exitCode = 1;
});
