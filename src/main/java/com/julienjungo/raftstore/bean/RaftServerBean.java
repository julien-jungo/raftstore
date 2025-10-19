package com.julienjungo.raftstore.bean;

import static org.apache.ratis.server.storage.RaftStorage.StartupOption;

import com.julienjungo.raftstore.RaftStoreFsm;
import jakarta.annotation.PreDestroy;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftPeerId;
import org.apache.ratis.server.RaftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RaftServerBean {

  private static final Logger LOG = LoggerFactory.getLogger(RaftServerBean.class);

  private final RaftServer raftServer;

  public RaftServerBean(RaftPeerId peerId, RaftProperties props, RaftGroup group, RaftStoreFsm fsm) throws IOException {
    LOG.atInfo().setMessage("Creating new RaftServer").addKeyValue("peerId", peerId).log();

    raftServer = RaftServer.newBuilder()
            .setOption(StartupOption.RECOVER)
            .setServerId(peerId)
            .setProperties(props)
            .setStateMachine(fsm)
            .setGroup(group)
            .build();

    raftServer.start();
  }

  @PreDestroy
  public void cleanup() throws IOException {
    LOG.atInfo().setMessage("Destroying RaftServer").log();
    raftServer.close();
  }

  public RaftServer getRaftServer() {
    return raftServer;
  }
}
