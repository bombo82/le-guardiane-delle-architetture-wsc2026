package it.giannibombelli.wsc2026.giftcard.api;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.module.WebApi;
import it.giannibombelli.wsc2026.giftcard.application.commands.IssueGiftCard;
import it.giannibombelli.wsc2026.giftcard.application.commands.RequestGiftCardTopUp;
import it.giannibombelli.wsc2026.giftcard.application.query.GiftCardDetails;
import it.giannibombelli.wsc2026.giftcard.application.query.GiftCardQueryService;
import it.giannibombelli.wsc2026.giftcard.application.usecases.GiftCardIssuing;
import it.giannibombelli.wsc2026.giftcard.application.usecases.TopUpRequesting;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;

import java.util.UUID;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public final class GiftCardApi implements WebApi {
    private final GiftCardIssuing giftCardIssuing;
    private final GiftCardQueryService giftCardQueryService;
    private final TopUpRequesting topUpRequesting;

    public GiftCardApi(GiftCardIssuing giftCardIssuing, GiftCardQueryService giftCardQueryService, TopUpRequesting topUpRequesting) {
        this.giftCardIssuing = giftCardIssuing;
        this.giftCardQueryService = giftCardQueryService;
        this.topUpRequesting = topUpRequesting;
    }

    @Override
    public void configure(JavalinConfig config) {
        config.routes.apiBuilder(() -> {
            post("/gift-cards", this::issue);
            post("/gift-cards/{id}/top-up", this::requestTopUp);
            get("/gift-cards/{id}", this::getById);
        });
    }

    @OpenApi(
        path = "/gift-cards",
        methods = HttpMethod.POST,
        summary = "Issue a new gift card",
        tags = {"GiftCard"},
        responses = {
            @OpenApiResponse(status = "201", content = {@OpenApiContent(from = GiftCardResponse.class)}),
            @OpenApiResponse(status = "400")
        }
    )
    private void issue(Context ctx) {
        GiftCardId cardId = EntityId.generate(GiftCardId::new);

        try {
            IssueGiftCard cmd = new IssueGiftCard(cardId);
            giftCardIssuing.invoke(cmd);
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result(ex.getMessage());
            return;
        }

        GiftCardResponse response = loadGiftCardResponse(ctx, cardId);
        if (response == null) {
            return;
        }

        ctx.status(201)
            .header("Location", "/gift-cards/" + cardId.value())
            .json(response);
    }

    @OpenApi(
        path = "/gift-cards/{id}/top-up",
        methods = HttpMethod.POST,
        summary = "Request a top-up for a gift card",
        tags = {"GiftCard"},
        pathParams = {@OpenApiParam(name = "id", required = true)},
        requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = RequestTopUpRequest.class)}),
        responses = {
            @OpenApiResponse(status = "200", content = {@OpenApiContent(from = GiftCardResponse.class)}),
            @OpenApiResponse(status = "400"),
            @OpenApiResponse(status = "404")
        }
    )
    private void requestTopUp(Context ctx) {
        GiftCardId cardId = parseGiftCardId(ctx);
        if (cardId == null) {
            return;
        }

        RequestTopUpRequest request = parseRequestTopUpRequest(ctx);
        if (request == null) {
            return;
        }

        if (request.amount() == null) {
            ctx.status(400).result("amount is required");
            return;
        }

        Money amount;
        try {
            amount = new Money(request.amount());
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result(ex.getMessage());
            return;
        }

        try {
            RequestGiftCardTopUp command = new RequestGiftCardTopUp(cardId, amount);
            topUpRequesting.invoke(command);
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result(ex.getMessage());
            return;
        } catch (IllegalStateException ex) {
            ctx.status(404);
            return;
        }

        GiftCardResponse response = loadGiftCardResponse(ctx, cardId);
        if (response == null) {
            return;
        }

        ctx.status(200).json(response);
    }

    @OpenApi(
        path = "/gift-cards/{id}",
        methods = HttpMethod.GET,
        summary = "Get a gift card by id",
        tags = {"GiftCard"},
        pathParams = {@OpenApiParam(name = "id", required = true)},
        responses = {
            @OpenApiResponse(status = "200", content = {@OpenApiContent(from = GiftCardResponse.class)}),
            @OpenApiResponse(status = "400"),
            @OpenApiResponse(status = "404")
        }
    )
    private void getById(Context ctx) {
        GiftCardId id = parseGiftCardId(ctx);
        if (id == null) {
            return;
        }

        var cardOpt = giftCardQueryService.findById(id);
        if (cardOpt.isEmpty()) {
            ctx.status(404);
            return;
        }

        ctx.status(200).json(toResponse(cardOpt.get()));
    }

    private GiftCardId parseGiftCardId(Context ctx) {
        String idParam = ctx.pathParam("id");
        try {
            return new GiftCardId(UUID.fromString(idParam));
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result("Invalid gift card id format");
            return null;
        }
    }

    private RequestTopUpRequest parseRequestTopUpRequest(Context ctx) {
        try {
            return ctx.bodyAsClass(RequestTopUpRequest.class);
        } catch (Exception ex) {
            ctx.status(400).result("request body is required");
            return null;
        }
    }

    private GiftCardResponse loadGiftCardResponse(Context ctx, GiftCardId cardId) {
        var cardOpt = giftCardQueryService.findById(cardId);
        if (cardOpt.isEmpty()) {
            ctx.status(500).result("gift card not found after update");
            return null;
        }
        return toResponse(cardOpt.get());
    }

    static GiftCardResponse toResponse(GiftCardDetails card) {
        return new GiftCardResponse(
            card.id(),
            card.balance().value()
        );
    }
}
