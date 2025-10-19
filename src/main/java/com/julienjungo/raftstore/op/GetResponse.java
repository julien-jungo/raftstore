package com.julienjungo.raftstore.op;

public record GetResponse(String op, String val) implements OpResponse {

  public static GetResponse from(String val) {
    return new GetResponse("get", val);
  }
}
