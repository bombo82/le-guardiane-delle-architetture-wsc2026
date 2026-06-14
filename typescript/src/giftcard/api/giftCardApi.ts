// API pubbliche del GiftCard Bounded Context.
// Violazione didattica: l'API dipende direttamente dall'infrastructure repository.

import express, { type Express, type Request, type Response } from 'express';
import { getErrorMessage } from '@/common/api/errorMessage.js';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { GiftCardId } from '../domain/giftcard/giftCardId.js';
import { SqliteGiftCardRepository } from '../infrastructure/sqliteGiftCardRepository.js';
import { GiftCardIssuing } from '../application/usecases/giftCardIssuing.js';
import { TopUpRequesting } from '../application/usecases/topUpRequesting.js';
import { requestGiftCardTopUp } from '../application/commands/requestGiftCardTopUp.js';
import { issueGiftCard } from '../application/commands/issueGiftCard.js';
import { parseRequestTopUpRequest } from './requestTopUpRequest.js';
import { toGiftCardResponse } from './giftCardResponse.js';

export class GiftCardApi {
  private readonly _giftCardIssuing: GiftCardIssuing;
  private readonly _giftCardRepository: SqliteGiftCardRepository;
  private readonly _topUpRequesting: TopUpRequesting;

  constructor(giftCardIssuing: GiftCardIssuing, giftCardRepository: SqliteGiftCardRepository, topUpRequesting: TopUpRequesting) {
    this._giftCardIssuing = giftCardIssuing;
    this._giftCardRepository = giftCardRepository;
    this._topUpRequesting = topUpRequesting;
  }

  configure(app: Express): void {
    const router = express.Router();
    router.use(express.json());

    router.post('/gift-cards', (req, res) => this.issue(req, res));
    router.post('/gift-cards/:id/top-up', (req, res) => this.requestTopUp(req, res));
    router.get('/gift-cards/:id', (req, res) => this.getById(req, res));

    app.use(router);
  }

  /**
   * @swagger
   * /gift-cards:
   *   post:
   *     summary: Emite una nuova gift card
   *     tags: [GiftCard]
   *     responses:
   *       201:
   *         description: Gift card emessa
   *         headers:
   *           Location:
   *             description: URI della gift card creata
   *             schema:
   *               type: string
   *         content:
   *           application/json:
   *             schema:
   *               $ref: '#/components/schemas/GiftCardResponse'
   *       400:
   *         description: Richiesta non valida
   */
  private issue(_req: Request, res: Response): void {
    const cardId = generateId((value) => new GiftCardId(value));

    try {
      this._giftCardIssuing.invoke(issueGiftCard(cardId));
    } catch (error) {
      res.status(400).send(getErrorMessage(error, 'bad request'));
      return;
    }

    const card = this._giftCardRepository.findById(cardId);
    if (card === null) {
      res.status(500).send('gift card not found after issuing');
      return;
    }

    res.status(201)
      .set('Location', `/gift-cards/${cardId.value.value}`)
      .json(toGiftCardResponse(card));
  }

  /**
   * @swagger
   * /gift-cards/{id}/top-up:
   *   post:
   *     summary: Richiede la ricarica di una gift card
   *     tags: [GiftCard]
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
   *             $ref: '#/components/schemas/RequestTopUpRequest'
   *     responses:
   *       200:
   *         description: Ricarica richiesta
   *         content:
   *           application/json:
   *             schema:
   *               $ref: '#/components/schemas/GiftCardResponse'
   *       400:
   *         description: Richiesta non valida
   *       404:
   *         description: Gift card non trovata
   */
  private requestTopUp(req: Request, res: Response): void {
    const cardId = this.parseGiftCardId(req, res);
    if (cardId === null) {
      return;
    }

    let request;
    try {
      request = parseRequestTopUpRequest(req.body);
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

    try {
      this._topUpRequesting.invoke(requestGiftCardTopUp(cardId, amount));
    } catch (error) {
      if (error instanceof Error && error.message.includes('Gift card not found')) {
        res.status(404).send();
        return;
      }
      res.status(400).send(getErrorMessage(error, 'bad request'));
      return;
    }

    const card = this._giftCardRepository.findById(cardId);
    if (card === null) {
      res.status(500).send('gift card not found after top up');
      return;
    }

    res.status(200).json(toGiftCardResponse(card));
  }

  /**
   * @swagger
   * /gift-cards/{id}:
   *   get:
   *     summary: Recupera una gift card per ID
   *     tags: [GiftCard]
   *     parameters:
   *       - in: path
   *         name: id
   *         required: true
   *         schema:
   *           type: string
   *           format: uuid
   *     responses:
   *       200:
   *         description: Gift card trovata
   *         content:
   *           application/json:
   *             schema:
   *               $ref: '#/components/schemas/GiftCardResponse'
   *       400:
   *         description: ID non valido
   *       404:
   *         description: Gift card non trovata
   */
  private getById(req: Request, res: Response): void {
    const id = this.parseGiftCardId(req, res);
    if (id === null) {
      return;
    }

    const card = this._giftCardRepository.findById(id);
    if (card === null) {
      res.status(404).send();
      return;
    }

    res.status(200).json(toGiftCardResponse(card));
  }

  private parseGiftCardId(req: Request, res: Response): GiftCardId | null {
    const idParam = req.params.id;
    if (typeof idParam !== 'string') {
      res.status(400).send('Invalid gift card id format');
      return null;
    }
    try {
      return new GiftCardId(Uuid.fromString(idParam));
    } catch (_error) {
      res.status(400).send('Invalid gift card id format');
      return null;
    }
  }
}
