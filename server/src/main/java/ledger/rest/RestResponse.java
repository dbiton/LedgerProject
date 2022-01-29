package ledger.rest;

import org.springframework.http.HttpStatus;
import ledger.repository.model.*;

import java.util.List;


public class RestResponse {

    public HttpStatus status;
    public String message;

    public RestResponse(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public static class ResponseTransaction extends RestResponse {
        public Transaction transaction;
        public ResponseTransaction(HttpStatus statusCode, String reason, Transaction transaction) {
            super(statusCode, reason);
            this.transaction = transaction;
        }
    }

    public static class ResponseTransactionList extends RestResponse {
        public List<Transaction> transactions;
        public ResponseTransactionList(HttpStatus status, String message, List<Transaction> transactions) {
            super(status, message);
            this.transactions = transactions;
        }
    }

    public static class ResponseUTxOs extends RestResponse {
        public List<UTxO> utxos;
        public ResponseUTxOs(HttpStatus status, String message, List<UTxO> utxos) {
            super(status, message);
            this.utxos = utxos;
        }
    }

}