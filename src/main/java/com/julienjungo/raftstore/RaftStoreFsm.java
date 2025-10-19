package com.julienjungo.raftstore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.julienjungo.raftstore.err.OpRequestUnmarshallingException;
import com.julienjungo.raftstore.err.OpResponseMarshallingException;
import com.julienjungo.raftstore.op.*;
import org.apache.ratis.io.MD5Hash;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.protocol.TermIndex;
import org.apache.ratis.server.raftlog.RaftLog;
import org.apache.ratis.server.storage.FileInfo;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.BaseStateMachine;
import org.apache.ratis.statemachine.impl.SimpleStateMachineStorage;
import org.apache.ratis.statemachine.impl.SingleFileSnapshotInfo;
import org.apache.ratis.util.MD5FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.ratis.proto.RaftProtos.LogEntryProto;

public class RaftStoreFsm extends BaseStateMachine {

  private static final Logger LOG = LoggerFactory.getLogger(RaftStoreFsm.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final SimpleStateMachineStorage storage = new SimpleStateMachineStorage();

  private final Map<String, String> store = new HashMap<>();

  private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

  @Override
  public SimpleStateMachineStorage getStateMachineStorage() {
    return storage;
  }

  @Override
  public void initialize(RaftServer raftServer, RaftGroupId raftGroupId, RaftStorage raftStorage) throws IOException {
    LOG.atInfo().setMessage("Initializing").log();
    super.initialize(raftServer, raftGroupId, raftStorage);
    this.storage.init(raftStorage);
    loadSnapshot();
  }

  @Override
  public void reinitialize() throws IOException {
    LOG.atInfo().setMessage("Reinitializing").log();
    loadSnapshot();
  }

  @Override
  public long takeSnapshot() throws IOException {
    LOG.atDebug().setMessage("Trying to take snapshot of store").log();

    Map<String, String> copy;
    TermIndex last;
    try {
      lock.readLock().lock();
      copy = new HashMap<>(store);
      last = getLastAppliedTermIndex();
    } finally {
      lock.readLock().unlock();
    }

    File snapshotFile = storage.getSnapshotFile(last.getTerm(), last.getIndex());
    LOG.atInfo().setMessage("Writing store data to snapshot file")
            .addKeyValue("size", copy.size())
            .addKeyValue("term", last.getTerm())
            .addKeyValue("index", last.getIndex())
            .log();

    MAPPER.writeValue(snapshotFile, copy);

    MD5Hash md5 = MD5FileUtil.computeAndSaveMd5ForFile(snapshotFile);
    FileInfo info = new FileInfo(snapshotFile.toPath(), md5);

    LOG.atDebug().setMessage("Updating latest snapshot").log();
    storage.updateLatestSnapshot(new SingleFileSnapshotInfo(info, last));

    return last.getIndex();
  }

  public long loadSnapshot() throws IOException {
    LOG.atInfo().setMessage("Trying to load snapshot from file").log();

    SingleFileSnapshotInfo snapshot = storage.getLatestSnapshot();
    if (snapshot == null) {
      LOG.atWarn().setMessage("Snapshot is null").log();
      return RaftLog.INVALID_LOG_INDEX;
    }

    File snapshotFile = snapshot.getFile().getPath().toFile();
    if (!snapshotFile.exists()) {
      LOG.atWarn().setMessage("Snapshot file does not exist").log();
      return RaftLog.INVALID_LOG_INDEX;
    }

    MD5Hash md5 = snapshot.getFile().getFileDigest();
    if (md5 != null) {
      LOG.atDebug().setMessage("Verifying MD5 hash").log();
      MD5FileUtil.verifySavedMD5(snapshotFile, md5);
    }

    TermIndex last = SimpleStateMachineStorage.getTermIndexFromSnapshotFile(snapshotFile);
    LOG.atInfo().setMessage("Loading snapshot from file")
            .addKeyValue("term", last.getTerm())
            .addKeyValue("index", last.getIndex())
            .log();

    try {
      lock.writeLock().lock();
      Map<String, String> copy = MAPPER.readValue(snapshotFile, new TypeReference<>(){});
      LOG.atDebug().setMessage("Writing snapshot data to store").addKeyValue("size", copy.size()).log();
      store.clear();
      store.putAll(copy);
      setLastAppliedTermIndex(last);
    } catch (IOException e) {
      LOG.atError().setCause(e).setMessage("Failed to snapshot").log();
      throw new IllegalStateException(e);
    } finally {
      lock.writeLock().unlock();
    }

    return last.getIndex();
  }

  @Override
  public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
    LogEntryProto entry = trx.getLogEntry();

    String json = entry.getStateMachineLogEntry().getLogData().toStringUtf8();
    LOG.atDebug().setMessage("Applying log entry").addKeyValue("request", json).log();

    OpRequest req;
    try {
      req = OpRequest.fromJson(json);
    } catch (OpRequestUnmarshallingException e) {
      LOG.atError().setCause(e).setMessage("Failed to unmarshal request").addKeyValue("request", json).log();
      return CompletableFuture.failedFuture(e);
    }

    OpResponse res;
    try {
      lock.writeLock().lock();
      res = handleRequest(req);
      updateLastAppliedTermIndex(entry.getTerm(), entry.getIndex());
    } finally {
      lock.writeLock().unlock();
    }

    try {
      return CompletableFuture.completedFuture(Message.valueOf(res.toJson()));
    } catch (OpResponseMarshallingException e) {
      LOG.atError().setCause(e).setMessage("Failed to marshal response").addKeyValue("response", res).log();
      return CompletableFuture.failedFuture(e);
    }
  }

  private OpResponse handleRequest(OpRequest req) {
    LOG.atDebug().setMessage("Handling request").addKeyValue("request", req).log();

    OpResponse res = switch (req) {
      case GetRequest getReq -> handleGetRequest(getReq);
      case SetRequest setReq -> handleSetRequest(setReq);
    };

    LOG.atDebug().setMessage("Returning response").addKeyValue("response", res).log();

    return res;
  }

  private GetResponse handleGetRequest(GetRequest req) {
    return GetResponse.from(store.get(req.key()));
  }

  private SetResponse handleSetRequest(SetRequest req) {
    return SetResponse.from(store.put(req.key(), req.val()));
  }
}
