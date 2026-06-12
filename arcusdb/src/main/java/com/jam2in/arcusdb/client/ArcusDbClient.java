package com.jam2in.arcusdb.client;

import com.google.protobuf.ByteString;
import com.jam2in.arcusdb.proto.ArcusDbProto.CreateCollectionRequest;
import com.jam2in.arcusdb.proto.ArcusDbProto.FilterExpression;
import com.jam2in.arcusdb.proto.ArcusDbProto.FindRequest;
import com.jam2in.arcusdb.proto.ArcusDbProto.FindResponse;
import com.jam2in.arcusdb.proto.ArcusDbProto.InsertRequest;
import com.jam2in.arcusdb.proto.ArcusDbProto.Request;
import com.jam2in.arcusdb.proto.ArcusDbProto.Response;
import com.jam2in.arcusdb.proto.ArcusDbProto.Response.KindCase;
import com.jam2in.arcusdb.proto.ArcusDbProto.StatusCode;
import com.jam2in.arcusdb.proto.ArcusDbProto.WriteResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bson.Document;

/**
 * Thin TCP client for the ArcusDB protobuf protocol.
 */
public final class ArcusDbClient implements AutoCloseable {

  private final ArcusDbConnection connection;

  public ArcusDbClient(ArcusDbClientConfig config) {
    this.connection = new ArcusDbConnection(Objects.requireNonNull(config, "config"));
  }

  public void createCollection(String name, Document catalog) {
    ByteString bytes = ByteString.copyFrom(BsonDocuments.encode(catalog));

    Request request = Request.newBuilder()
        .setCreateCollection(
            CreateCollectionRequest.newBuilder()
                .setName(name)
                .setCatalog(bytes))
        .build();

    Response response = connection.send(request);
    if (response.getKindCase() != KindCase.WRITE) {
      throw new ArcusDbException(
          "createCollection failed: expected WRITE response but got " + response.getKindCase());
    }

    WriteResponse write = response.getWrite();
    if (write.getStatus() != StatusCode.STATUS_CODE_OK) {
      throw new ArcusDbStatusException("createCollection", write.getStatus());
    }
  }

  public void insert(String collection, Document document) {
    ByteString bytes = ByteString.copyFrom(BsonDocuments.encode(document));

    Request request = Request.newBuilder()
        .setInsert(InsertRequest.newBuilder()
            .setCollection(requireCollection(collection))
            .setDocument(bytes))
        .build();

    Response response = connection.send(request);
    if (response.getKindCase() != KindCase.WRITE) {
      throw new ArcusDbException(
          "insert failed: expected WRITE response but got " + response.getKindCase());
    }

    WriteResponse write = response.getWrite();
    if (write.getStatus() != StatusCode.STATUS_CODE_OK) {
      throw new ArcusDbStatusException("insert", write.getStatus());
    }
  }

  public List<Document> find(String collection, FilterExpression filter) {
    Request request = Request.newBuilder()
        .setFind(FindRequest.newBuilder()
            .setCollection(requireCollection(collection))
            .setFilter(Objects.requireNonNull(filter, "filter")))
        .build();

    Response response = connection.send(request);
    if (response.getKindCase() != KindCase.FIND) {
      throw new ArcusDbException(
          "find failed: expected FIND response but got " + response.getKindCase());
    }

    FindResponse find = response.getFind();
    if (find.getStatus() != StatusCode.STATUS_CODE_OK) {
      throw new ArcusDbStatusException("find", find.getStatus());
    }

    List<Document> documents = new ArrayList<>(find.getDocumentsCount());
    for (ByteString document : find.getDocumentsList()) {
      documents.add(BsonDocuments.decode(document.toByteArray()));
    }
    return documents;
  }

  @Override
  public void close() {
    connection.close();
  }

  private static String requireCollection(String collection) {
    String value = Objects.requireNonNull(collection, "collection").trim();
    if (value.isEmpty()) {
      throw new IllegalArgumentException("collection must not be blank");
    }
    return value;
  }
}
