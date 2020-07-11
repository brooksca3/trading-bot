/* HOW TO RUN
   1) Configure things in the Configuration class
   2) Compile: javac Bot.java
   3) Run in loop: while true; do java Bot; sleep 1; done
*/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

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

public class Bot {

    public static int orderid = 0;
    public static int bonds = 0;

    public static void main(String[] args) {
        /* The boolean passed to the Configuration constructor dictates whether or not the
           bot is connecting to the prod or test exchange. Be careful with this switch! */
        Configuration config = new Configuration(true);
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
            System.err.printf("The exchange replied: %s\n", reply);
	    
            to_exchange.println("ADD " + orderid++ + " BOND BUY 999 25");
	    bonds = 25;
            while (true) {

                reply = from_exchange.readLine().trim();
		String[] line = reply.split(" ");
		System.out.println(line[0]);
		if (line[0].equals("FILL") && line[3].equals("BUY")) {
		    bonds += Integer.parseInt(line[5]);
		}
		if (line[0].equals("FILL") && line[3].equals("SELL")) {
		    bonds -= Integer.parseInt(line[5]);
		}
                if (bonds < 100) {
                    to_exchange.println("ADD " + orderid++ + " BOND BUY 999 " + (100 - bonds));
		}
		if (bonds > -100) {
                    to_exchange.println("ADD " + orderid++ + " BOND SELL 1000 " + (29 + bonds));
                }
		System.out.println(bonds);
                String ans = from_exchange.readLine().trim();
                System.err.printf("The exchange replied: %s\n", ans);
                Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    //public static void penny() {

    //}


}

