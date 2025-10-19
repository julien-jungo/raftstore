package com.julienjungo.raftstore.op;

public record SetResponse(String op, String val) implements OpResponse {

  public static SetResponse from(String val) {
    return new SetResponse("set", val);
  }
}
