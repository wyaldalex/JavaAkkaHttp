import akka.actor.typed.ActorSystem;
import akka.actor.typed.scaladsl.Behaviors;

public class SimpleSynchronousServer {

    public void run() {

        ActorSystem actorSystem = ActorSystem.create(Behaviors.empty(),
                "simpleServer");

    }
}
