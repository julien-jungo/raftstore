package com.julienjungo.raftstore;

import com.julienjungo.raftstore.bean.RaftClientBean;
import com.julienjungo.raftstore.err.OpRequestUnmarshallingException;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.protocol.exceptions.StateMachineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/")
public class RaftStoreServer {

  private static final Logger LOG = LoggerFactory.getLogger(RaftStoreServer.class);

  private final RaftClient client;

  public RaftStoreServer(RaftClientBean bean) {
    this.client = bean.getRaftClient();
  }

  @PostMapping(
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Mono<ResponseEntity<String>> handleOperation(@RequestBody String json) {
    LOG.atInfo().setMessage("Handling operation").addKeyValue("request", json).log();

    CompletableFuture<ResponseEntity<String>> future =
            client.async().send(Message.valueOf(json))
                    .thenApply(this::handleSuccess)
                    .exceptionally(this::handleFailure);

    return Mono.fromFuture(future);
  }

  private ResponseEntity<String> handleSuccess(RaftClientReply reply) {
    String json = reply.getMessage().getContent().toStringUtf8();
    LOG.atInfo().setMessage("Handling success case").addKeyValue("response", json).log();
    return ResponseEntity.ok(json);
  }

  private ResponseEntity<String> handleFailure(Throwable t) {
    LOG.atError().setCause(t).setMessage("Handling failure case").log();

    if (!(t.getCause() instanceof StateMachineException e)) {
      LOG.atInfo().setMessage("Returning status code 500").log();
      return ResponseEntity.internalServerError().build();
    }

    if (!(e.getCause() instanceof OpRequestUnmarshallingException)) {
      LOG.atInfo().setMessage("Returning status code 500").log();
      return ResponseEntity.internalServerError().build();
    }

    LOG.atInfo().setMessage("Returning status code 400").log();
    return ResponseEntity.badRequest().build();
  }
}
