// Helper per avviare un'app Express su porta random e inviare richieste nei test.

import express, { type Express, type ErrorRequestHandler } from 'express';
import http from 'node:http';

export interface TestResponse {
  status: number;
  body: string;
  headers: http.IncomingHttpHeaders;
}

export class ExpressTestHelper {
  private _app: Express | null = null;
  private _server: http.Server | null = null;
  private _port: number = 0;

  async start(configure: (app: Express) => void): Promise<void> {
    this._app = express();
    this._app.use(express.json());

    configure(this._app);

    const errorHandler: ErrorRequestHandler = (err, _req, res, next) => {
      if (err instanceof SyntaxError && 'body' in err) {
        res.status(400).send('request body is required');
        return;
      }
      next(err);
    };
    this._app.use(errorHandler);

    return new Promise((resolve, reject) => {
      this._server = this._app!.listen(0, () => {
        const address = this._server!.address();
        if (address !== null && typeof address === 'object') {
          this._port = address.port;
        }
        resolve();
      });
      this._server.on('error', reject);
    });
  }

  async stop(): Promise<void> {
    return new Promise((resolve) => {
      if (this._server === null) {
        resolve();
        return;
      }
      this._server.close(() => {
        this._server = null;
        this._app = null;
        resolve();
      });
    });
  }

  port(): number {
    return this._port;
  }

  async post(path: string, body?: unknown): Promise<TestResponse> {
    return this.request('POST', path, body);
  }

  async get(path: string): Promise<TestResponse> {
    return this.request('GET', path);
  }

  private request(method: 'GET' | 'POST', path: string, body?: unknown): Promise<TestResponse> {
    return new Promise((resolve, reject) => {
      const options: http.RequestOptions = {
        hostname: 'localhost',
        port: this._port,
        path,
        method,
        headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
      };

      const req = http.request(options, (res) => {
        let data = '';
        res.setEncoding('utf8');
        res.on('data', (chunk) => {
          data += chunk;
        });
        res.on('end', () => {
          resolve({
            status: res.statusCode ?? 0,
            body: data,
            headers: res.headers,
          });
        });
      });

      req.on('error', reject);

      if (body !== undefined) {
        req.write(JSON.stringify(body));
      }
      req.end();
    });
  }
}
