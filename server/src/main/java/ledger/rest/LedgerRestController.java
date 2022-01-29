package ledger.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import javassist.bytecode.stackmap.TypeData;
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

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for REST API endpoints
 */
@RestController
@EntityScan("model")
public class LedgerRestController {
    LedgerRestController(){
    }

    private static final String limit_null = "-1";

    public void throwErrors(RestResponse resp) {
        if (resp.status != HttpStatus.OK && resp.status != HttpStatus.CREATED){
            throw new RuntimeException("HTTP ERROR:" + resp.status + ":" + resp.message);
        }
    }

    @PostMapping("/transactions")
    public @ResponseBody List<Transaction> submitTransactions(@RequestBody List<TransactionRequest> transactions) {
        if (transactions.size() > 1) {
        } else {
        }
        return new ArrayList<>();
    }

    @PostMapping("/send_coins")
    public @ResponseBody Transaction sendCoins(@RequestBody SendCoinsRequest body) {
        return new Transaction(new BigInteger("0"), new ArrayList<>(), new ArrayList<>());
    }

    @GetMapping("/transactions/{address}")
    public @ResponseBody List<Transaction> getTransactions(@PathVariable String address,
                                                                     @RequestParam(required = false, defaultValue = limit_null) int limit) {
        return new ArrayList<>();
    }

    @GetMapping("/transactions")
    public @ResponseBody List<Transaction> getAllTransactions(@RequestParam(required = false, defaultValue = limit_null) int limit) {
        return new ArrayList<>();
    }

    @GetMapping("/utxos/{address}")
    public @ResponseBody List<UTxO> getUTxOs(@PathVariable String address) {
        return new ArrayList<>();
    }
}