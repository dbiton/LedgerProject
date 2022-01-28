package ledger;

import ledger.controller.LedgerController;

import java.io.Console;
import java.math.BigInteger;

public class ClientMain {
    public static void main(String[] args) {
        BigInteger address = new BigInteger(args[0]);
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        LedgerController client = new LedgerController(host, port);

        Console console = System.console();

        if (console == null) {
            System.out.println("No console available");
            return;
        }

        String in = console.readLine("Enter command (h for help): ");
        // this part is not very interesting and could be hardcoded (for now).
        // finish this up for the presentation so we can present the system nicely
    }
}