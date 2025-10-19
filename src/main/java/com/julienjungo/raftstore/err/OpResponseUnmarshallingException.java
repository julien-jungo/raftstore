package com.julienjungo.raftstore.err;

import org.apache.ratis.protocol.exceptions.StateMachineException;

import java.io.Serializable;

public class OpResponseUnmarshallingException extends StateMachineException implements Serializable {
  public OpResponseUnmarshallingException(String msg) {
    super(msg);
  }
}
