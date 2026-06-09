package com.jam2in.arcusdb.client;

import java.nio.ByteBuffer;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.ByteBufNIO;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.ByteBufferBsonInput;

/**
 *
 */
final class BsonDocuments {

  private static final DocumentCodec DOCUMENT_CODEC = new DocumentCodec();

  private BsonDocuments() {
  }

  public static byte[] encode(Document document) {
    try (BasicOutputBuffer output = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(output)) {
      DOCUMENT_CODEC.encode(writer, document, EncoderContext.builder().build());
      return output.toByteArray();
    }
  }

  public static Document decode(byte[] bytes) {
    ByteBufNIO byteBufNIO = new ByteBufNIO(ByteBuffer.wrap(bytes));
    try (ByteBufferBsonInput bsonInput = new ByteBufferBsonInput(byteBufNIO);
        BsonBinaryReader reader = new BsonBinaryReader(bsonInput)) {
      return DOCUMENT_CODEC.decode(reader, DecoderContext.builder().build());
    }
  }
}
