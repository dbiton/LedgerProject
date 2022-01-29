package ledger.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SendCoinsRequest {
    @JsonProperty("request_id")
    public String request_id;
    @JsonProperty("from_address")
    public String from_address;
    @JsonProperty("to_address")
    public String to_address;
    @JsonProperty("coins")
    public long coins;
}