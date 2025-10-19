package com.julienjungo.raftstore.op;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.julienjungo.raftstore.err.OpRequestMarshallingException;
import com.julienjungo.raftstore.err.OpRequestUnmarshallingException;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "op"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GetRequest.class, name = "get"),
        @JsonSubTypes.Type(value = SetRequest.class, name = "set"),
})
public sealed interface OpRequest extends Operation permits GetRequest, SetRequest {

  static OpRequest fromJson(String json) throws OpRequestUnmarshallingException {
    try {
      return MAPPER.readValue(json, OpRequest.class);
    } catch (JsonProcessingException e) {
      throw new OpRequestUnmarshallingException(e.getMessage());
    }
  }

  @Override
  default String toJson() throws OpRequestMarshallingException {
    try {
      return MAPPER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new OpRequestMarshallingException(e.getMessage());
    }
  }
}
