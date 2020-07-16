package com.lightstreamer.jms.demo.stocklist_client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.lightstreamer.jms.LSConnectionFactory;
import com.lightstreamer.jms.LSSession;
import com.lightstreamer.jms.demo.stocklist_service.message.FeedMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;

public class App {

  private static final String TOPIC_NAME = "stocksTopic";

  private static final String DEFAULT_SERVER = "http://localhost:8080/";

  private static final String DEFAULT_CONNECTOR = "ActiveMQ";

  private static final String FORMAT = "%-6s %-19s %-6s %5s %6s %5s %5s %5s %5s %5s%n";

  @Parameter(
      names = {"-s", "--server"},
      description = "JMS Extender end-point")
  private String server = DEFAULT_SERVER;

  @Parameter(
      names = {"-c", "--connector"},
      description = "JMS connector name")
  private String connector = DEFAULT_CONNECTOR;

  @Parameter(
      names = {"-u", "--user"},
      description = "User name")
  private String user;

  @Parameter(
      names = {"-p", "--password"},
      description = "Password",
      password = true)
  private String password;

  @Parameter(
      names = {"-?", "-h", "--help"},
      description = "Show usage",
      help = true)
  private boolean usage;

  /////////////////////////////////////////////////////////////////////////
  // StockList

  public void run() {
    try {
      System.out.printf("Connecting to %s using connector %s ...%n%n", server, connector);

      // Create the connection
      ConnectionFactory factory = new LSConnectionFactory(server, connector);
      Connection connection = factory.createConnection(user, password);

      connection.start();

      System.out.println("Connected, subscribing...");

      // Create the session
      Session session = connection.createSession(false, LSSession.PRE_ACKNOWLEDGE);

      // Subscribe to the stocklist topic
      Topic topic = session.createTopic(TOPIC_NAME);
      MessageConsumer consumer = session.createConsumer(topic);

      System.out.println("Subscribed, waiting for data...");
      System.out.println();

      System.out.printf(
          FORMAT,
          "Item",
          "Stock name",
          "Time",
          "Last",
          "Change",
          "Open",
          "Min",
          "Max",
          "Bid",
          "Ask");

      consumer.setMessageListener(
          message -> {
            try {
              // Cast to application message
              ObjectMessage objMessage = (ObjectMessage) message;
              FeedMessage feedMessage = (FeedMessage) objMessage.getObject();

              System.out.printf(
                  FORMAT,
                  feedMessage.itemName,
                  feedMessage.currentValues.get("stock_name"),
                  feedMessage.currentValues.get("time"),
                  feedMessage.currentValues.get("last_price"),
                  feedMessage.currentValues.get("pct_change"),
                  feedMessage.currentValues.get("open_price"),
                  feedMessage.currentValues.get("min"),
                  feedMessage.currentValues.get("max"),
                  feedMessage.currentValues.get("bid"),
                  feedMessage.currentValues.get("ask"));

            } catch (Exception e) {
              e.printStackTrace();
            }
          });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /////////////////////////////////////////////////////////////////////////
  // Main

  public static void main(String[] args) {
    App stocklist = new App();

    JCommander commander = JCommander.newBuilder().addObject(stocklist).build();

    commander.parse(args);

    if (stocklist.usage) {
      commander.usage();
      return;
    }

    stocklist.run();
  }
}
