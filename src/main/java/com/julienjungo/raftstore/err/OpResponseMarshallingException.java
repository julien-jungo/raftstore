package com.julienjungo.raftstore.err;

import org.apache.ratis.protocol.exceptions.StateMachineException;

import java.io.Serializable;

public class OpResponseMarshallingException extends StateMachineException implements Serializable {
  public OpResponseMarshallingException(String msg) {
    super(msg);
  }
}
