package ledger.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import javassist.bytecode.stackmap.TypeData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.web.bind.annotation.*;
import rest_api.exception.BadRequestException;
import rest_api.exception.ConflictException;
import rest_api.exception.NotFoundException;
import transactionmanager.TransactionManager;

import ledger.repository.model.Transaction;
import ledger.repository.model.UTxO;
import ledger.repository.model.Transfer;

import java.io.IOException;
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

    /** Handle error responses */
    public void throwErrors(RestResponse resp) {
        switch (resp.status) {
            case OK: break;
            case CREATED: break;
            case BAD_REQUEST: throw new BadRequestException(resp.reason);
            case CONFLICT: throw new ConflictException(resp.reason);
            case NOT_FOUND: throw new NotFoundException(resp.reason);
            default: throw new RuntimeException(String.format("Unknown exception type %s", resp.statusCode.toString()));
        }
    }

    @PostMapping("/transactions")
    public @ResponseBody List<Transaction> createTransactions(@RequestBody List<TransactionRequest> transactions) {
        if (transactions.size() == 1) {
            TransactionRequest transactionReq = transactions.get(0);
            //RestResponse.ResponseTransaction resp = transactionManager.handleTransaction(transactionReq);
            //throwErrors(resp);
            //return List.of(resp.transaction);
        } else {
            //Response.TransactionListResp resp = transactionManager.handleAtomicTxList(transactions);
            //handleErrors(resp);
            //return resp.transactionsList;
        }
    }

    @PostMapping("/send_coins")
    public @ResponseBody Transaction sendCoins(@RequestBody SendCoinsRequestBody body) {
        //Response.TransactionResp resp = transactionManager.handleCoinTransfer(body.sourceAddress, body.targetAddress, body.coins, body.reqId);
        //handleErrors(resp);
        //return resp.transaction;
    }

    @GetMapping("/transactions/{address}")
    public @ResponseBody List<Transaction> getAllTransactionsForUser(@PathVariable String address,
                                                                     @RequestParam(required = false, defaultValue = limitParamDefault) int limit) {
        //return transactionManager.handleListAddrTransactions(address, limit).transactionsList;
    }

    @GetMapping("/transactions")
    public @ResponseBody List<Transaction> getAllTransactions(@RequestParam(required = false, defaultValue = limitParamDefault) int limit) {
        //return transactionManager.handleListEntireHistory(limit).transactionsList;
    }

    @GetMapping("/utxos/{address}")
    public @ResponseBody List<UTxO> getAllUtxosForUser(@PathVariable String address) {
        //return transactionManager.handleListAddrUTxO(address).unusedUtxoList;
    }
}