import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import akka.http.javadsl.Http;
import akka.http.javadsl.IncomingConnection;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.*;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.japi.function.Function;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.Directives.*;

public class HighLevelServer {

    ActorSystem<CustomerBehavior.Command> actorSystem = ActorSystem.create(CustomerBehavior.create(),
            "simpleServer");

    Function<HttpRequest, CompletionStage<HttpResponse>> updateCustomer = (request) -> {
        CompletableFuture<HttpResponse> response = new CompletableFuture<>();
        Unmarshaller<HttpEntity, Customer> unmarshaller = Jackson.unmarshaller(Customer.class);
        CompletionStage<Customer> customerFuture = unmarshaller.unmarshal(request.entity(),actorSystem);

        customerFuture.whenComplete((customer, throwable) -> {
            actorSystem.tell(new CustomerBehavior.UpdateCustomerCommand(customer));
            request.discardEntityBytes(actorSystem);
            response.complete(HttpResponse.create().withStatus(StatusCodes.CREATED));
        });
        return  response;
    };


    Function<HttpRequest, CompletionStage<HttpResponse>> getAllCustomers = (request) -> {

        CompletableFuture<HttpResponse> response = new CompletableFuture<>();
        CompletionStage<List<Customer>> customersFuture = AskPattern.ask(actorSystem, me ->
                        new CustomerBehavior.GetCustomersCommand(me),
                Duration.ofSeconds(5), actorSystem.scheduler());
        customersFuture.whenComplete((customers, throwable) -> {
            try {
                String json = new ObjectMapper().writeValueAsString(customers);
                request.discardEntityBytes(actorSystem);
                response.complete(HttpResponse.create().withStatus(200).withEntity(json));

            } catch (JsonProcessingException e) {
                request.discardEntityBytes(actorSystem);
                response.complete(HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR));
            }
        });
        return  response;
    };

    private Route createRoute() {
        return pathPrefix("api", () ->
                path("customer", () ->
                        concat(
                                get(() -> handle(getAllCustomers)),
                                post(() -> handle(updateCustomer))
                        )));
//        Route getAllCustomerRoute = get(() -> handle(getAllCustomers));
//        Route updateCustomerRoute = post(() -> handle(updateCustomer));
//        Route combinedRoute = concat(getAllCustomerRoute,updateCustomerRoute);
//        Route customerRoute = path("customer", () -> combinedRoute);
//        Route allRoutes = pathPrefix("api", () -> customerRoute);
//
//        return allRoutes;

    }
    public void run() {

        CompletionStage<ServerBinding> server = Http.get(actorSystem)
                .newServerAt("localhost", 9001).bind(createRoute()) ;

        server.whenComplete((binding, throwable) -> {
            if(throwable != null) {
                System.out.println("Something went wrong " + throwable);
            } else {
                System.out.println("The server is running at " + binding.localAddress());
            }
        });

    }

}

