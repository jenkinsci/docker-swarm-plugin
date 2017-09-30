package suryagaddipati.jenkinsdockerslaves.docker;

import akka.actor.AbstractActor;

public class Greeter1 extends AbstractActor {

  public static class Greet {
  }

  public static class WhoToGreet {
    public final String who;

    public WhoToGreet(String who) {
      this.who = who;
    }
  }


  private String greeting = "hello";

  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .match(WhoToGreet.class, this::onWhoToGreet)
      .match(Greet.class, this::onGreet)
      .build();
  }

  private void onWhoToGreet(WhoToGreet whoToGreet) {
    greeting = "hello, " + whoToGreet.who;
  }

  private void onGreet(Greet greet) {
    System.out.println(greeting);
  }

}
