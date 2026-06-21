// API interne del Payment Bounded Context per setup e test.

import express, { type Express, type Request, type Response } from 'express';
import { getErrorMessage } from '@/common/api/errorMessage.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { RequestPayment, requestPayment } from '../application/commands/requestPayment.js';
import { PaymentFinder } from '../application/query/paymentFinder.js';
import { PaymentRequesting } from '../application/usecases/paymentRequesting.js';
import { PaymentId } from '../domain/payment/paymentId.js';
import { parseCreatePaymentRequest } from './createPaymentRequest.js';
import { toPaymentDetailsResponse } from './paymentDetailsResponse.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class PaymentInternalApi {
  private readonly _paymentRequesting: PaymentRequesting;
  private readonly _paymentFinder: PaymentFinder;

  constructor(paymentRequesting: PaymentRequesting, paymentFinder: PaymentFinder) {
    requireDependency(paymentRequesting, "paymentRequesting");
    requireDependency(paymentFinder, "paymentFinder");
    this._paymentRequesting = paymentRequesting;
    this._paymentFinder = paymentFinder;
  }

  configure(app: Express): void {
    const router = express.Router();
    router.use(express.json());

    router.post('/internals/payments', (req, res) => this.create(req, res));
    router.get('/internals/payments', (req, res) => this.getByClientReference(req, res));

    app.use(router);
  }

  /**
   * @swagger
   * /internals/payments:
   *   post:
   *     summary: Crea un pagamento (uso interno e test)
   *     tags: [Payment Internal]
   *     requestBody:
   *       required: true
   *       content:
   *         application/json:
   *           schema:
   *             $ref: '#/components/schemas/CreatePaymentRequest'
   *     responses:
   *       201:
   *         description: Pagamento creato
   *         headers:
   *           Location:
   *             description: URI del pagamento creato
   *             schema:
   *               type: string
   *         content:
   *           application/json:
   *             schema:
   *               $ref: '#/components/schemas/PaymentDetailsResponse'
   *       400:
   *         description: Richiesta non valida
   */
  private create(req: Request, res: Response): void {
    let request;
    try {
      request = parseCreatePaymentRequest(req.body);
    } catch (error) {
      res.status(400).send(getErrorMessage(error, 'bad request'));
      return;
    }

    let paymentId: PaymentId;
    try {
      paymentId = new PaymentId(Uuid.fromString(request.paymentId));
    } catch (error) {
      res.status(400).send(getErrorMessage(error, 'invalid paymentId'));
      return;
    }

    let command: RequestPayment;
    try {
      command = requestPayment(
        paymentId,
        new ClientReference(Uuid.fromString(request.clientReference)),
        new Money(Number(request.amount)),
        new Timestamp(new Date(request.requestedAt))
      );
    } catch (error) {
      res.status(400).send(getErrorMessage(error, 'invalid request'));
      return;
    }

    try {
      this._paymentRequesting.invoke(command);
    } catch (error) {
      res.status(400).send(getErrorMessage(error, 'bad request'));
      return;
    }

    const details = this._paymentFinder.findDetailsById(paymentId);
    if (details === null) {
      res.status(500).send('payment not found after creation');
      return;
    }

    res.status(201)
      .set('Location', `/payments/${paymentId.value.value}`)
      .json(toPaymentDetailsResponse(details));
  }

  /**
   * @swagger
   * /internals/payments:
   *   get:
   *     summary: Cerca un pagamento per client reference
   *     tags: [Payment Internal]
   *     parameters:
   *       - in: query
   *         name: clientReference
   *         required: true
   *         schema:
   *           type: string
   *     responses:
   *       200:
   *         description: Pagamento trovato
   *         content:
   *           application/json:
   *             schema:
   *               $ref: '#/components/schemas/PaymentDetailsResponse'
   *       400:
   *         description: Parametro clientReference mancante o non valido
   *       404:
   *         description: Pagamento non trovato
   */
  private getByClientReference(req: Request, res: Response): void {
    const clientReference = req.query.clientReference;
    if (clientReference === undefined || clientReference === null || typeof clientReference !== 'string' || clientReference.trim().length === 0) {
      res.status(400).send('clientReference query parameter is required');
      return;
    }

    let payment;
    try {
      payment = this._paymentFinder.findDetailsByClientReference(new ClientReference(Uuid.fromString(clientReference)));
    } catch (error) {
      res.status(400).send(getErrorMessage(error, 'bad request'));
      return;
    }

    if (payment === null) {
      res.status(404).send();
      return;
    }

    res.status(200).json(toPaymentDetailsResponse(payment));
  }
}
