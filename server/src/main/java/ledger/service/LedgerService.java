package ledger.service;

import com.google.protobuf.Empty;
import cs236351.ledger.*;
import io.grpc.stub.StreamObserver;
import ledger.repository.LedgerRepository;

import java.math.BigInteger;
import java.util.*;

import ledger.repository.model.Transaction;
import ledger.repository.model.Transfer;
import ledger.repository.model.UTxO;
import ledger.repository.util.proto;


public class LedgerService extends LedgerServiceGrpc.LedgerServiceImplBase {
    LedgerServer ledger;

    @Override
    public void submitTransactionInternal(rpcTransaction request,
                                  io.grpc.stub.StreamObserver<cs236351.ledger.Res> responseObserver){
        Transaction transaction = proto.fromMessage(request);
        ledger.repository.submitTransaction(transaction);
        responseObserver.onNext(Res.newBuilder().setRes(ResCode.SUCCESS).build());
        responseObserver.onCompleted();
    }

    @Override
    public void submitTransaction(rpcTransaction request,
                                          io.grpc.stub.StreamObserver<cs236351.ledger.Res> responseObserver){
        Transaction transaction = proto.fromMessage(request);
        ledger.submitTransaction(transaction);
        responseObserver.onNext(Res.newBuilder().setRes(ResCode.SUCCESS).build());
        responseObserver.onCompleted();
    }

    @Override
    public void submitTransfer(cs236351.ledger.TransferAndTransactionID request,
                               io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        Transfer transfer = proto.fromMessage(request.getTransfer());
        BigInteger tid = proto.toBigInteger(request.getTransactionId());
        ledger.repository.submitTransfer(transfer, tid);
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendCoins(cs236351.ledger.AddressesAndAmount request,
                          io.grpc.stub.StreamObserver<cs236351.ledger.Res> responseObserver) {
        BigInteger address_from = proto.toBigInteger(request.getAddressFrom());
        BigInteger address_to = proto.toBigInteger(request.getAddressTo());
        long amount = request.getAmount();
        ledger.sendCoins(address_from, address_to, amount);
        responseObserver.onNext(Res.newBuilder().setRes(ResCode.SUCCESS).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUTxOs(cs236351.ledger.uint128 request,
                         io.grpc.stub.StreamObserver<rpcUTxO> responseObserver) {
        BigInteger address = proto.toBigInteger(request);
        List<UTxO> us = ledger.getUTxOs(address);
        for (ledger.repository.model.UTxO u : us){
            responseObserver.onNext(proto.toMessage(u));
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getTransactions(cs236351.ledger.AddressAndMax request,
                                io.grpc.stub.StreamObserver<rpcTransaction> responseObserver) {
        BigInteger address = proto.toBigInteger(request.getAddress());
        int max = request.getMax();
        List<Transaction> transactions = ledger.getTransactions(address, max);
        for(Transaction t : transactions){
            responseObserver.onNext(proto.toMessage(t));
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getAllTransactions(cs236351.ledger.Max request,
                                   io.grpc.stub.StreamObserver<rpcTransaction> responseObserver) {
        int max = request.getMax();
        List<Transaction> transactions = ledger.getAllTransactions(max);
        for(Transaction t : transactions){
            responseObserver.onNext(proto.toMessage(t));
        }
        responseObserver.onCompleted();
    }
}
