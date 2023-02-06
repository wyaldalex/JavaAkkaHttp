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

public class SimpleAsynchronousServer {

    ActorSystem<CustomerBehavior.Command> actorSystem = ActorSystem.create(CustomerBehavior.create(),
            "simpleServer");

    Function<HttpRequest, CompletionStage<HttpResponse> > asynchronizedMethodHandler = (httpRequest) -> {
        CompletableFuture<HttpResponse> response = new CompletableFuture<>();
        if(httpRequest.getUri().path().equalsIgnoreCase("/api/customer")) {
            if(httpRequest.method() == HttpMethods.POST) {
                Unmarshaller<HttpEntity, Customer> unmarshaller = Jackson.unmarshaller(Customer.class);
                CompletionStage<Customer> customerFuture = unmarshaller.unmarshal(httpRequest.entity(),actorSystem);

                customerFuture.whenComplete((customer, throwable) -> {
                   actorSystem.tell(new CustomerBehavior.UpdateCustomerCommand(customer));
                   httpRequest.discardEntityBytes(actorSystem);
                   response.complete(HttpResponse.create().withStatus(StatusCodes.CREATED));
                });
            } else if(httpRequest.method() == HttpMethods.GET) {

                CompletionStage<List<Customer>> customersFuture = AskPattern.ask(actorSystem, me ->
                        new CustomerBehavior.GetCustomersCommand(me),
                        Duration.ofSeconds(5), actorSystem.scheduler());
                customersFuture.whenComplete((customers, throwable) -> {
                    try {
                        String json = new ObjectMapper().writeValueAsString(customers);
                        httpRequest.discardEntityBytes(actorSystem);
                        response.complete(HttpResponse.create().withStatus(200).withEntity(json));

                    } catch (JsonProcessingException e) {
                        httpRequest.discardEntityBytes(actorSystem);
                        response.complete(HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR));
                    }
                });


            } else {
                httpRequest.discardEntityBytes(actorSystem);
                response.complete(HttpResponse.create().withStatus(StatusCodes.OK));
            }

        } else {
            httpRequest.discardEntityBytes(actorSystem);
            response.complete(HttpResponse.create().withStatus(StatusCodes.NOT_FOUND));
        }
        return  response;
    };

    public void run() {
        Source<IncomingConnection, CompletionStage<ServerBinding>> source =
                Http.get(actorSystem).newServerAt("localhost", 9001).connectionSource() ;

        Flow<IncomingConnection, IncomingConnection, NotUsed> flow =
                Flow.of(IncomingConnection.class).map(connection -> {
                            System.out.println("Incoming connection from " + connection.remoteAddress().toString());
                            connection.handleWithAsyncHandler(asynchronizedMethodHandler, Materializer.createMaterializer(actorSystem));
                            return connection;
                        }
                );

        Sink<IncomingConnection, CompletionStage<Done>> sink = Sink.ignore();

        CompletionStage<ServerBinding> server = source.via(flow).to(sink).run(actorSystem);

        server.whenComplete((binding, throwable) -> {
            if(throwable != null) {
                System.out.println("Something went wrong " + throwable);
            } else {
                System.out.println("The server is running at " + binding.localAddress());
            }
        });

    }
}
