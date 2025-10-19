package com.julienjungo.raftstore.err;

import org.apache.ratis.protocol.exceptions.StateMachineException;

import java.io.Serializable;

public class OpRequestUnmarshallingException extends StateMachineException implements Serializable {
  public OpRequestUnmarshallingException(String msg) {
    super(msg);
  }
}
