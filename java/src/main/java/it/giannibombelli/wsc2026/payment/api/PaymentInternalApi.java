package it.giannibombelli.wsc2026.payment.api;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.module.WebApi;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.application.commands.RequestPayment;
import it.giannibombelli.wsc2026.payment.application.query.PaymentDetails;
import it.giannibombelli.wsc2026.payment.application.query.PaymentFinder;
import it.giannibombelli.wsc2026.payment.application.usecases.PaymentRequesting;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;

import java.util.Optional;
import java.util.UUID;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;
import static java.util.Objects.requireNonNull;

/**
 * Endpoint interni del Payment BC, separati da {@link PaymentApi} perché non fanno parte
 * del contratto pubblico.
 */
public final class PaymentInternalApi implements WebApi {

    private final PaymentRequesting paymentRequesting;
    private final PaymentFinder paymentFinder;

    public PaymentInternalApi(PaymentRequesting paymentRequesting, PaymentFinder paymentFinder) {
        this.paymentRequesting = requireNonNull(paymentRequesting);
        this.paymentFinder = requireNonNull(paymentFinder);
    }

    @Override
    public void configure(JavalinConfig config) {
        config.routes.apiBuilder(() -> {
            post("/internals/payments", this::create);
            get("/internals/payments", this::getByClientReference);
        });
    }

    @OpenApi(
        path = "/internals/payments",
        methods = HttpMethod.POST,
        summary = "Create a payment (internal use only)",
        tags = {"Payment Internal"},
        requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = CreatePaymentRequest.class)}),
        responses = {
            @OpenApiResponse(status = "201", content = {@OpenApiContent(from = PaymentDetailsResponse.class)}),
            @OpenApiResponse(status = "400")
        }
    )
    private void create(Context ctx) {
        CreatePaymentRequest request = parseCreatePaymentRequest(ctx);
        if (request == null) {
            return;
        }

        if (request.paymentId() == null) {
            ctx.status(400).result("paymentId is required");
            return;
        }
        if (request.clientReference() == null || request.clientReference().isBlank()) {
            ctx.status(400).result("clientReference is required");
            return;
        }
        if (request.amount() == null) {
            ctx.status(400).result("amount is required");
            return;
        }
        if (request.requestedAt() == null) {
            ctx.status(400).result("requestedAt is required");
            return;
        }

        PaymentId paymentId;
        try {
            paymentId = new PaymentId(request.paymentId());
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result(ex.getMessage());
            return;
        }

        RequestPayment command;
        try {
            command = new RequestPayment(
                paymentId,
                new ClientReference(UUID.fromString(request.clientReference())),
                new Money(request.amount()),
                new Timestamp(request.requestedAt())
            );
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result(ex.getMessage());
            return;
        }

        try {
            paymentRequesting.invoke(command);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ctx.status(400).result(ex.getMessage());
            return;
        }

        PaymentDetails details = paymentFinder.findDetailsById(paymentId).orElse(null);
        if (details == null) {
            ctx.status(500).result("payment not found after creation");
            return;
        }

        ctx.status(201)
            .header("Location", "/payments/" + paymentId.value())
            .json(PaymentDetailsResponse.from(details));
    }

    @OpenApi(
        path = "/internals/payments",
        methods = HttpMethod.GET,
        summary = "Get a payment by client reference (internal use only)",
        tags = {"Payment Internal"},
        queryParams = {@OpenApiParam(name = "clientReference", required = true)},
        responses = {
            @OpenApiResponse(status = "200", content = {@OpenApiContent(from = PaymentDetailsResponse.class)}),
            @OpenApiResponse(status = "400"),
            @OpenApiResponse(status = "404")
        }
    )
    private void getByClientReference(Context ctx) {
        String clientReference = ctx.queryParam("clientReference");
        if (clientReference == null || clientReference.isBlank()) {
            ctx.status(400).result("clientReference query parameter is required");
            return;
        }

        Optional<PaymentDetails> payment;
        try {
            payment = paymentFinder.findDetailsByClientReference(new ClientReference(UUID.fromString(clientReference)));
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result(ex.getMessage());
            return;
        }

        if (payment.isEmpty()) {
            ctx.status(404);
            return;
        }

        ctx.status(200).json(PaymentDetailsResponse.from(payment.get()));
    }

    private CreatePaymentRequest parseCreatePaymentRequest(Context ctx) {
        try {
            return ctx.bodyAsClass(CreatePaymentRequest.class);
        } catch (Exception ex) {
            ctx.status(400).result("request body is required");
            return null;
        }
    }
}
