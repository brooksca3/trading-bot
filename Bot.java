/* HOW TO RUN
   1) Configure things in the Configuration class
   2) Compile: javac Bot.java
   3) Run in loop: while true; do java Bot; sleep 1; done
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

public class Bot {

    public static int orderid = 0;
    public static int bonds = 0;
    public static boolean open = true;

    static ArrayList<Double> BOND = new ArrayList<Double>();
    static ArrayList<Double> VALBZ = new ArrayList<Double>();
    static ArrayList<Double> VALE = new ArrayList<Double>();
    static ArrayList<Double> GS = new ArrayList<Double>();
    static ArrayList<Double> MS = new ArrayList<Double>();
    static ArrayList<Double> WFC = new ArrayList<Double>();
    static ArrayList<Double> XLF = new ArrayList<Double>();

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
            // System.err.printf("The exchange replied: %s\n", reply);

            //to_exchange.println("ADD " + orderid++ + " BOND BUY 999 25");
            bonds = 0;
            while (true) {
                getInfo(from_exchange);
                tradeADR(to_exchange);
                tradeETF(to_exchange);
                if (open) {
                    tradeADR(to_exchange);
                    tradeETF(to_exchange);
                } else {
                    rejoin(to_exchange, from_exchange);
                }

                /*
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
                 if (line[0].equals("FILL") && line[3].equals("BUY")) {
                 bonds += Integer.parseInt(line[5]);
                 }
                 if (line[0].equals("FILL") && line[3].equals("SELL")) {
                 bonds -= Integer.parseInt(line[5]);
                 }
                 if (bonds < 100) {
                 to_exchange.println("ADD " + orderid++ + " BOND BUY 999 " + 10);
                 }
                 if (bonds > -100) {
                 to_exchange.println("ADD " + orderid++ + " BOND SELL 1000 " + 10);
                 }
                 System.out.println(bonds);
                 String ans = from_exchange.readLine().trim();
                 System.err.printf("The exchange replied: %s\n", ans);
                 */
                Thread.sleep(1);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    // get info
    public static void getInfo(BufferedReader read) throws IOException, InterruptedException {
        System.err.println("Getting market info.");
        int counter = 0;
        for (int i = 0; i < 1000; i++) {
            String line = read.readLine().trim();
            System.err.printf("Info: %s\n", line);
            String[] info = line.split(" ");
            if (info[0].equals("CLOSE")) {
                open = false;
                System.err.println("Market is closed.");
                return;
            }
            if (line == null)
                continue;
            if (info[0].equals("TRADE")) {
                if (info[1].equals("BOND"))
                    BOND.add(Double.parseDouble(info[2]));
                else if (info[1].equals("VALBZ"))
                    VALBZ.add(Double.parseDouble(info[2]));
                else if (info[1].equals("VALE"))
                    VALE.add(Double.parseDouble(info[2]));
                else if (info[1].equals("GS"))
                    GS.add(Double.parseDouble(info[2]));
                else if (info[1].equals("MS"))
                    MS.add(Double.parseDouble(info[2]));
                else if (info[1].equals("WFC"))
                    WFC.add(Double.parseDouble(info[2]));
            }
            Thread.sleep(1);
        }
    }

    // calculate average of 'lastN' items in list
    public static double avg(ArrayList<Double> prices, int lastN) {
        double sum = 0;
        for (int i = prices.size() - 1; i > prices.size() - lastN - 1; i--) {
            sum += prices.get(i);
        }
        return (sum / (double) lastN);
    }

    // waiting for market to open
    public static void rejoin(PrintWriter write, BufferedReader read) throws IOException {
        while (!open) {
            write.println("HELLO PROSPECTAVENUE");
            if (read.readLine().trim().split(" ")[0].equals("HELLO"))
                open = true;
        }
    }


    // adr pair trading
    public static void tradeADR(PrintWriter write) {
        int SIZE = 10;
        if (VALE.size() < SIZE || VALBZ.size() < SIZE)
            return;
        double ADR = avg(VALE, SIZE);
        double REG = avg(VALBZ, SIZE);
        double diff = REG - ADR;
        if (diff >= 4) {
            System.err.println("Buying ADR / selling regular");
            write.println("ADD " + orderid++ + " VALE BUY " + ((int) ADR + 1) + " " + SIZE / 2 + 3);
            write.println("CONVERT " + orderid++ + " VALE SELL " + SIZE);
            write.println("ADD " + orderid++ + " VALBZ SELL " + ((int) REG - 1) + " " + SIZE / 2 + 3);
        } else if (diff <= -4) {
            System.err.println("Buying regular / selling ADR");
            write.println("ADD " + orderid++ + " VALBZ BUY " + ((int) REG + 1) + " " + SIZE / 2 + 3);
            write.println("CONVERT " + orderid++ + " VALE BUY " + SIZE);
            write.println("ADD " + orderid++ + " VALE SELL " + ((int) ADR - 1) + " " + SIZE / 2 + 3);
        }
    }

    // etf arbitrage

    public static void tradeETF(PrintWriter write) {

        int SIZE = 20;
        if (WFC.size() < SIZE || BOND.size() < SIZE)
            return;
        if (GS.size() < SIZE || MS.size() < SIZE)
            return;
        if (XLF.size() < SIZE)
            return;
        double xlfP = avg(XLF, SIZE);
        double bondP = avg(BOND, SIZE);
        double gsP = avg(GS, SIZE);
        double msP = avg(MS, SIZE);
        double wfcP = avg(WFC, SIZE);
        double diff = (10 * xlfP) - (3 * bondP + 2 * gsP + 3 * msP + 2 * wfcP);

        if (diff > 35) {
            write.println("ADD " + orderid++ + " BOND BUY " + ((int) bondP + 1) + " " + 30);
            write.println("ADD " + orderid++ + " GS BUY " + ((int) gsP + 1) + " " + 20);
            write.println("ADD " + orderid++ + " MS BUY " + ((int) msP + 1) + " " + 30);
            write.println("ADD " + orderid++ + " WFC BUY " + ((int) wfcP + 1) + " " + 20);
            write.println("CONVERT " + orderid++ + " XLF BUY " + 100);
            write.println("ADD " + orderid++ + " XLF SELL " + ((int) xlfP - 1) + " " + 100);
        } else if (diff < -35) {
            write.println("ADD " + orderid++ + " XLF BUY " + ((int) xlfP + 1) + " " + 100);
            write.println("CONVERT " + orderid++ + " XLF SELL " + 100);
            write.println("ADD " + orderid++ + " BOND SELL " + ((int) bondP - 1) + " " + 30);
            write.println("ADD " + orderid++ + " GS SELL " + ((int) gsP - 1) + " " + 20);
            write.println("ADD " + orderid++ + " MS SELL " + ((int) msP - 1) + " " + 30);
            write.println("ADD " + orderid++ + " WFC SELL " + ((int) wfcP - 1) + " " + 20);
        }

    }

}

