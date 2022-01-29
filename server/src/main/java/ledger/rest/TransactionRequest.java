package ledger.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Embeddable;
import javax.persistence.OneToMany;
import java.util.List;

import ledger.repository.model.*;

@Embeddable
public class TransactionRequest {
        @OneToMany
        @JsonProperty("inputs")
        public List<UTxO> inputs;
        @OneToMany
        @JsonProperty("outputs")
        public List<Transfer> outputs;
        public TransactionRequest(List<UTxO> inputs, List<Transfer> outputs) {
            this.inputs = inputs;
            this.outputs = outputs;
        }
        public TransactionRequest() {
        }
}