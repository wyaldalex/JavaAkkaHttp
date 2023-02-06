import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerBehavior extends AbstractBehavior<CustomerBehavior.Command> {

    public interface Command {
    }

    public static class UpdateCustomerCommand implements Command {
        private Customer customer;

        public UpdateCustomerCommand(Customer customer) {
            this.customer = customer;
        }

        public Customer getCustomer() {
            return customer;
        }
    }

    public static class GetCustomerCommand implements Command {
        private int customerId;
        private ActorRef<Customer> sender;

        public GetCustomerCommand(int customerId, ActorRef<Customer> sender) {
            this.customerId = customerId;
            this.sender = sender;
        }

        public int getCustomerId() {
            return customerId;
        }

        public ActorRef<Customer> getSender() {
            return sender;
        }
    }

    public static class GetCustomersCommand implements Command {
        private ActorRef<List<Customer>> sender;

        public GetCustomersCommand(ActorRef<List<Customer>> sender) {
            this.sender = sender;
        }

        public ActorRef<List<Customer>> getSender() {
            return sender;
        }
    }

    private Map<Integer,Customer> customers;

    private CustomerBehavior(ActorContext<Command> context) {
        super(context);
        customers = new HashMap<>();
        customers.put(1,new Customer(1,"James","Black",21,true));
        customers.put(2,new Customer(2,"Sally","Green",26,false));
        customers.put(3,new Customer(3,"Phil","Brown",28,true));
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(CustomerBehavior::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(UpdateCustomerCommand.class, message -> {
                   customers.put(message.getCustomer().getId(), message.getCustomer());
                   return Behaviors.same();
                })
                .onMessage(GetCustomerCommand.class, message -> {
                    Customer customer = customers.get(message.getCustomerId());
                    message.getSender().tell(customer);
                    return Behaviors.same();
                })
                .onMessage(GetCustomersCommand.class, message -> {
                    message.getSender().tell(new ArrayList(customers.values()));
                    return Behaviors.same();
                })
                .build();
    }
}
