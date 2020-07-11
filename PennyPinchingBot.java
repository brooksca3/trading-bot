/* HOW TO RUN
   1) Configure things in the Configuration class
   2) Compile: javac Bot.java
   3) Run in loop: while true; do java PennyPinchingBot; sleep 1; done
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

class Configuration {
    String exchange_name;
    int exchange_port;
    /* 0 = prod-like
       1 = slow
       2 = empty
    */
    final Integer test_exchange_kind = 0;
    /* replace REPLACEME with your team name! */
    final String team_name = "PROSPECTAVENUE";

    Configuration(Boolean test_mode) {
        if (!test_mode) {
            /* If we pass in false, we're putting our bot into PRODUCTION */
            exchange_port = 20000;
            exchange_name = "production";
        } else {
            /* If we pass in true, we're TESTING */
            exchange_port = 20000 + test_exchange_kind;
            exchange_name = "test-exch-" + this.team_name;
        }
    }

    String exchange_name() {
        return exchange_name;
    }

    Integer port() {
        return exchange_port;
    }
}

public class PennyPinchingBot {

    public static int bonds = 0;
    public static int orderid = 0;
    public static boolean open = true;

    public static void main(String[] args) {
        /* The boolean passed to the Configuration constructor dictates whether or not the
           bot is connecting to the prod or test exchange. Be careful with this switch! */
        Configuration config = new Configuration(false);
        try {
            Socket skt = new Socket(config.exchange_name(), config.port());
            BufferedReader from_exchange = new BufferedReader(new InputStreamReader(skt.getInputStream()));
            PrintWriter to_exchange = new PrintWriter(skt.getOutputStream(), true);

            /*
              A common mistake people make is to to_exchange.println() > 1
              time for every from_exchange.readLine() response.
              Since many write messages generate marketdata, this will cause an
              exponential explosion in pending messages. Please, don't do that!
            */
            to_exchange.println(("HELLO " + config.team_name).toUpperCase());
            String reply = from_exchange.readLine().trim();
            // System.err.printf("The exchange replied: %s\n", reply);

            //to_exchange.println("ADD " + orderid++ + " BOND BUY 999 25");
            while (true) {

		 if (!open) {
		     rejoin(to_exchange, from_exchange);
		     continue;
		 }
                 reply = from_exchange.readLine().trim();
                 Thread.sleep(5);
                 String[] line = reply.split(" ");
                 System.out.println(line[0]);
                 if (line[0].equals("FILL") && line[3].equals("BUY")) {
                 bonds += Integer.parseInt(line[5]);
                 }
                 if (line[0].equals("FILL") && line[3].equals("SELL")) {
                 bonds -= Integer.parseInt(line[5]);
                 }
                 line = reply.split(" ");
                 System.out.println(line[0]);
                 if (bonds < 100) {
                 to_exchange.println("ADD " + orderid++ + " BOND BUY 999 " + 10);
                 }
                 if (bonds > -100) {
                 to_exchange.println("ADD " + orderid++ + " BOND SELL 1000 " + 10);
                 }
                 System.out.println(bonds);
                 String ans = from_exchange.readLine().trim();
                 System.err.printf("The exchange replied: %s\n", ans);
                Thread.sleep(1);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }


    // waiting for market to open
    public static void rejoin(PrintWriter write, BufferedReader read) throws IOException {
        while (!open) {
            write.println("HELLO PROSPECTAVENUE");
            if (read.readLine().trim().split(" ")[0].equals("HELLO"))
                open = true;
        }
    }

}
