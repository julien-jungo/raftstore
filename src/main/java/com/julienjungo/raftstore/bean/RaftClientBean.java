package com.julienjungo.raftstore.bean;

import jakarta.annotation.PreDestroy;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.client.RaftClientRpc;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.protocol.RaftGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@DependsOn("raftServerBean")
public class RaftClientBean {

  private static final Logger LOG = LoggerFactory.getLogger(RaftClientBean.class);

  private final RaftClient raftClient;

  public RaftClientBean(RaftProperties props, RaftGroup group, RaftClientRpc rpc) {
    LOG.atInfo().setMessage("Creating new RaftClient").log();

    this.raftClient = RaftClient.newBuilder()
            .setProperties(props)
            .setRaftGroup(group)
            .setClientRpc(rpc)
            .build();
  }

  @PreDestroy
  public void cleanup() throws IOException {
    LOG.atInfo().setMessage("Destroying RaftClient").log();
    raftClient.close();
  }

  public RaftClient getRaftClient() {
    return raftClient;
  }
}
