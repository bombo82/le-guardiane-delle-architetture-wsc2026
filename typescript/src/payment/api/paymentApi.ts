// API pubbliche del Payment Bounded Context.

import express, { type Express, type Request, type Response } from 'express';
import { getErrorMessage } from '@/common/api/errorMessage.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { StartTransaction, startTransaction } from '../application/commands/startTransaction.js';
import { PaymentFinder } from '../application/query/paymentFinder.js';
import { PaymentProcessing } from '../application/services/paymentProcessing.js';
import { PaymentId } from '../domain/payment/paymentId.js';
import { PaymentNotFoundException } from '../domain/payment/paymentNotFoundException.js';
import { providerFromLabel } from '../domain/payment/provider.js';
import { ProviderReference } from '../domain/payment/providerReference.js';
import { parseStartTransactionRequest } from './startTransactionRequest.js';
import { toPaymentDetailsResponse } from './paymentDetailsResponse.js';
import { toTransactionResponse } from './transactionResponse.js';
import { requireDependency } from '@/common/utils/requireDependency.js';
import type { WebApi } from '@/common/module/applicationModule.js';

export class PaymentApi implements WebApi {
  private readonly _paymentFinder: PaymentFinder;
  private readonly _paymentProcessing: PaymentProcessing;

  constructor(paymentFinder: PaymentFinder, paymentProcessing: PaymentProcessing) {
    requireDependency(paymentFinder, "paymentFinder");
    requireDependency(paymentProcessing, "paymentProcessing");
    this._paymentFinder = paymentFinder;
    this._paymentProcessing = paymentProcessing;
  }

  configure(app: Express): void {
    const router = express.Router();
    router.use(express.json());

    router.get('/payments/:id', (req, res) => this.getById(req, res));
    router.post('/payments/:id/transactions', (req, res) => this.startTransaction(req, res));

    app.use(router);
  }

  /**
   * @swagger
   * /payments/{id}/transactions:
   *   post:
   *     summary: Avvia una transazione di pagamento (asincrona)
   *     tags: [Payment]
   *     parameters:
   *       - in: path
   *         name: id
   *         required: true
   *         schema:
   *           type: string
   *           format: uuid
   *     requestBody:
   *       required: true
   *       content:
   *         application/json:
   *           schema:
   *             $ref: '#/components/schemas/StartTransactionRequest'
   *     responses:
   *       202:
   *         description: Transazione avviata
   *         content:
   *           application/json:
   *             schema:
   *               $ref: '#/components/schemas/TransactionResponse'
   *       400:
   *         description: Richiesta non valida
   *       404:
   *         description: Pagamento non trovato
   */
  private startTransaction(req: Request, res: Response): void {
    const paymentId = this.parsePaymentId(req, res);
    if (paymentId === null) {
      return;
    }

    let request;
    try {
      request = parseStartTransactionRequest(req.body);
    } catch (error) {
      res.status(400).send(getErrorMessage(error, 'bad request'));
      return;
    }

    let amount: Money;
    try {
      amount = new Money(Number(request.amount));
    } catch (error) {
      res.status(400).send(getErrorMessage(error, 'invalid amount'));
      return;
    }

    let cmd: StartTransaction;
    try {
      const providerReference = request.providerReference === null ? null : new ProviderReference(Uuid.fromString(request.providerReference));
      cmd = startTransaction(
        paymentId,
        providerFromLabel(request.provider),
        providerReference,
        amount,
        Timestamp.now()
      );
    } catch (error) {
      res.status(400).send(getErrorMessage(error, 'invalid request'));
      return;
    }

    let started;
    try {
      started = this._paymentProcessing.invoke(cmd);
    } catch (error) {
      if (error instanceof PaymentNotFoundException) {
        res.status(404).send();
        return;
      }
      res.status(400).send(getErrorMessage(error, 'bad request'));
      return;
    }

    const response = toTransactionResponse({
      id: started.transactionId.value.value,
      provider: started.provider,
      providerReference: cmd.providerReference === null ? null : cmd.providerReference.value.value,
      amount: started.amount,
      status: 'STARTED',
      startedAt: cmd.startedAt,
      completedAt: null,
    });
    res.status(202).json(response);
  }

  /**
   * @swagger
   * /payments/{id}:
   *   get:
   *     summary: Recupera il dettaglio di un pagamento
   *     tags: [Payment]
   *     parameters:
   *       - in: path
   *         name: id
   *         required: true
   *         schema:
   *           type: string
   *           format: uuid
   *     responses:
   *       200:
   *         description: Pagamento trovato
   *         content:
   *           application/json:
   *             schema:
   *               $ref: '#/components/schemas/PaymentDetailsResponse'
   *       400:
   *         description: ID non valido
   *       404:
   *         description: Pagamento non trovato
   */
  private getById(req: Request, res: Response): void {
    const id = this.parsePaymentId(req, res);
    if (id === null) {
      return;
    }

    const payment = this._paymentFinder.findDetailsById(id);
    if (payment === null) {
      res.status(404).send();
      return;
    }

    res.status(200).json(toPaymentDetailsResponse(payment));
  }

  private parsePaymentId(req: Request, res: Response): PaymentId | null {
    const idParam = req.params.id;
    if (typeof idParam !== 'string') {
      res.status(400).send('Invalid payment id format');
      return null;
    }
    try {
      return new PaymentId(Uuid.fromString(idParam));
    } catch (_error) {
      res.status(400).send('Invalid payment id format');
      return null;
    }
  }
}
