package com.julienjungo.raftstore.err;

import org.apache.ratis.protocol.exceptions.StateMachineException;

import java.io.Serializable;

public class OpRequestMarshallingException extends StateMachineException implements Serializable {
  public OpRequestMarshallingException(String msg) {
    super(msg);
  }
}
