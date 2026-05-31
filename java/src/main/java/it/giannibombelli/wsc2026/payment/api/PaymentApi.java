package it.giannibombelli.wsc2026.payment.api;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.application.commands.StartTransaction;
import it.giannibombelli.wsc2026.payment.application.query.PaymentDetails;
import it.giannibombelli.wsc2026.payment.application.query.PaymentFinder;
import it.giannibombelli.wsc2026.payment.application.services.PaymentProcessing;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionStarted;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.Provider;
import it.giannibombelli.wsc2026.payment.domain.payment.ProviderReference;

import java.util.UUID;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public final class PaymentApi {
    private final PaymentFinder paymentFinder;
    private final PaymentProcessing paymentProcessing;

    public PaymentApi(PaymentFinder paymentFinder, PaymentProcessing paymentProcessing) {
        this.paymentFinder = paymentFinder;
        this.paymentProcessing = paymentProcessing;
    }

    public void configure(JavalinConfig config) {
        config.routes.apiBuilder(() -> {
            get("/payments/{id}", this::getById);
            post("/payments/{id}/transactions", this::startTransaction);
        });
    }

    @OpenApi(
        path = "/payments/{id}/transactions",
        methods = HttpMethod.POST,
        summary = "Start a payment transaction (asynchronous)",
        tags = {"Payment"},
        pathParams = {@OpenApiParam(name = "id", required = true)},
        requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = StartTransactionRequest.class)}),
        responses = {
            @OpenApiResponse(status = "202", content = {@OpenApiContent(from = TransactionResponse.class)}),
            @OpenApiResponse(status = "400")
        }
    )
    private void startTransaction(Context ctx) {
        PaymentId paymentId = parsePaymentId(ctx);
        if (paymentId == null) {
            return;
        }

        StartTransactionRequest request = parseStartTransactionRequest(ctx);
        if (request == null) {
            return;
        }

        if (request.provider() == null || request.provider().isBlank()) {
            ctx.status(400).result("provider is required");
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

        StartTransaction cmd;
        try {
            ProviderReference providerReference = request.providerReference() == null ? null : new ProviderReference(request.providerReference());
            cmd = new StartTransaction(paymentId, Provider.fromLabel(request.provider()), providerReference, amount, Timestamp.now());
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result(ex.getMessage());
            return;
        }

        if (paymentFinder.findDetailsById(paymentId).isEmpty()) {
            ctx.status(404);
            return;
        }

        TransactionStarted started;
        try {
            started = paymentProcessing.invoke(cmd);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ctx.status(400).result(ex.getMessage());
            return;
        }

        TransactionResponse response = new TransactionResponse(
            started.transactionId().value(),
            started.provider().label(),
            cmd.providerReference() == null ? null : cmd.providerReference().value(),
            started.amount().value(),
            "STARTED",
            cmd.startedAt().value(),
            null
        );
        ctx.status(202).json(response);
    }

    @OpenApi(
        path = "/payments/{id}",
        methods = HttpMethod.GET,
        summary = "Get a payment by id",
        tags = {"Payment"},
        pathParams = {@OpenApiParam(name = "id", required = true)},
        responses = {
            @OpenApiResponse(status = "200", content = {@OpenApiContent(from = PaymentDetailsResponse.class)}),
            @OpenApiResponse(status = "400"),
            @OpenApiResponse(status = "404")
        }
    )
    private void getById(Context ctx) {
        PaymentId id = parsePaymentId(ctx);
        if (id == null) {
            return;
        }

        var paymentOpt = paymentFinder.findDetailsById(id);
        if (paymentOpt.isEmpty()) {
            ctx.status(404);
            return;
        }

        ctx.status(200).json(PaymentDetailsResponse.from(paymentOpt.get()));
    }

    private PaymentId parsePaymentId(Context ctx) {
        String idParam = ctx.pathParam("id");
        try {
            return new PaymentId(UUID.fromString(idParam));
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result("Invalid payment id format");
            return null;
        }
    }

    private StartTransactionRequest parseStartTransactionRequest(Context ctx) {
        try {
            return ctx.bodyAsClass(StartTransactionRequest.class);
        } catch (Exception ex) {
            ctx.status(400).result("request body is required");
            return null;
        }
    }

    private static TransactionResponse toTransactionResponse(PaymentDetails.TransactionDetail detail) {
        return new TransactionResponse(
            detail.id(),
            detail.provider().label(),
            detail.providerReference(),
            detail.amount().value(),
            detail.status().name(),
            detail.startedAt().value(),
            detail.completedAt() == null ? null : detail.completedAt().value()
        );
    }
}
