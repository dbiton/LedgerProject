package ledger.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import javassist.bytecode.stackmap.TypeData;
import ledger.service.LedgerServer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.web.bind.annotation.*;

import ledger.repository.model.Transaction;
import ledger.repository.model.UTxO;
import ledger.repository.model.Transfer;

import javax.persistence.criteria.CriteriaBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


@RestController
@EntityScan("model")
public class LedgerRestController {
    LedgerServer ledger;

    LedgerRestController(){
    }

    private static final String limit_null = "-1";

    @PostMapping("/setup/{host}/{port}/{num_shards}/{zookeeper_host}")
    public @ResponseBody boolean setup(@PathVariable String host, @PathVariable String port,
                                       @PathVariable String num_shards, @PathVariable String zookeeper_host) {
        try {
            this.ledger = new LedgerServer(host, Integer.parseInt(port), Integer.parseInt(num_shards), zookeeper_host);
            return true;
        }
        catch (Exception e){
            System.out.println("On LedgerRestController setup:" + e);
            return false;
        }
    }

    @PostMapping("/transactions")
    public @ResponseBody boolean submitTransactions(@RequestBody List<Transaction> transactions) {
        System.out.println("submitTransactions");
        if (transactions.size() > 1) {
            return ledger.submitTransaction(transactions.get(0));
        } else {
            return ledger.submitTransactions(transactions);
        }
    }

    @PostMapping("/send_coins")
    public @ResponseBody boolean sendCoins(@RequestBody SendCoinsRequest body) {
        System.out.println("sendCoins");
        return ledger.sendCoins(new BigInteger(body.from_address), new BigInteger(body.to_address), body.coins);
    }

    @GetMapping("/transactions/{address}")
    public @ResponseBody List<Transaction> getTransactions(@PathVariable String address,
                                                                     @RequestParam(required = false, defaultValue = limit_null) int limit) {
        System.out.println("getTransactions");
        return ledger.getTransactions(new BigInteger(address), limit);
    }

    @GetMapping("/transactions")
    public @ResponseBody List<Transaction> getAllTransactions(@RequestParam(required = false, defaultValue = limit_null) int limit) {
        System.out.println("getAllTransactions");
        return ledger.getAllTransactions(limit);
    }

    @GetMapping("/utxos/{address}")
    public @ResponseBody List<UTxO> getUTxOs(@PathVariable String address) {
        System.out.println("getUTxOs");
        return ledger.getUTxOs(new BigInteger(address));
    }
}