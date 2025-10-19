package com.julienjungo.raftstore.bean;

import com.julienjungo.raftstore.RaftStoreFsm;
import org.apache.ratis.client.RaftClientRpc;
import org.apache.ratis.conf.Parameters;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcConfigKeys;
import org.apache.ratis.grpc.GrpcFactory;
import org.apache.ratis.protocol.*;
import org.apache.ratis.server.RaftServerConfigKeys;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;
import org.apache.ratis.util.NetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Configuration
public class RaftConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(RaftConfiguration.class);

  @Value("${group.id}")
  private String groupId;

  @Value("${peer.id}")
  private String rawPeerId;

  @Value("${peers.ids}")
  private String rawPeerIds;

  @Value("${peers.addrs}")
  private String rawPeerAddrs;

  @Value("${data.dir}")
  private String dataDir;

  @Bean
  public RaftStoreFsm newRaftStoreFsm() {
    LOG.atInfo().setMessage("Creating new RaftStoreFsm").log();
    return new RaftStoreFsm();
  }

  @Bean
  public RaftPeerId newRaftPeerId() {
    LOG.atInfo().setMessage("Creating new RaftPeerId").log();
    return RaftPeerId.valueOf(rawPeerId);
  }

  @Bean
  public RaftGroup newRaftGroup() {
    List<String> peerIds = List.of(rawPeerIds.split(","));
    List<String> peerAddrs = List.of(rawPeerAddrs.split(","));

    LOG.atInfo().setMessage("Creating new RaftGroup")
            .addKeyValue("peerIds", peerIds)
            .addKeyValue("peerAddrs", peerAddrs)
            .log();

    if (peerIds.isEmpty() || peerIds.size() != peerAddrs.size() || !peerIds.contains(rawPeerId)) {
      IllegalArgumentException e = new IllegalArgumentException("Invalid RaftGroup config");
      LOG.atError().setCause(e).setMessage(e.getMessage()).log();
      throw e;
    }

    List<RaftPeer> peers = IntStream.range(0, peerIds.size())
            .mapToObj(i -> RaftPeer.newBuilder()
                    .setAddress(peerAddrs.get(i))
                    .setId(peerIds.get(i))
                    .build())
            .toList();

    return RaftGroup.valueOf(RaftGroupId.valueOf(ByteString.copyFromUtf8(groupId)), peers);
  }

  @Bean
  public RaftProperties newRaftProperties(RaftGroup group, RaftPeerId peerId) {
    LOG.atInfo().setMessage("Creating new RaftProperties").log();

    RaftProperties props = new RaftProperties();

    LOG.atDebug().setMessage("Setting storage directory").addKeyValue("dir", dataDir).log();
    RaftServerConfigKeys.setStorageDir(props, List.of(new File(dataDir)));

    int serverPort = NetUtils.createSocketAddr(group.getPeer(peerId).getAddress()).getPort();
    LOG.atDebug().setMessage("Setting server port").addKeyValue("port", serverPort).log();
    GrpcConfigKeys.Server.setPort(props, serverPort);

    Optional.ofNullable(group.getPeer(peerId).getClientAddress()).ifPresent(addr -> {
      int clientPort = NetUtils.createSocketAddr(addr).getPort();
      LOG.atDebug().setMessage("Setting client port").addKeyValue("port", clientPort).log();
      GrpcConfigKeys.Client.setPort(props, clientPort);
    });

    Optional.ofNullable(group.getPeer(peerId).getAdminAddress()).ifPresent(addr -> {
      int adminPort = NetUtils.createSocketAddr(addr).getPort();
      LOG.atDebug().setMessage("Setting admin port").addKeyValue("port", adminPort).log();
      GrpcConfigKeys.Admin.setPort(props, adminPort);
    });

    return props;
  }

  @Bean
  public RaftClientRpc newRaftClientRpc(RaftProperties props) {
    LOG.atInfo().setMessage("Creating new RaftClientRpc").log();
    return new GrpcFactory(new Parameters()).newRaftClientRpc(ClientId.randomId(), props);
  }
}
