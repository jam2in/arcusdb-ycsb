package com.jam2in.arcusdb.client;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
final class FrameCodec {

  static final int MAX_FRAME_BYTES = 64 * 1024 * 1024;

  private FrameCodec() {
  }

  static byte[] readFrame(InputStream input) throws IOException {
    int b1 = input.read();
    if (b1 == -1) {
      throw new EOFException("end of stream");
    }
    int b2 = readByte(input);
    int b3 = readByte(input);
    int b4 = readByte(input);
    int length = ((b1 & 0xff) << 24) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 8) | (b4 & 0xff);
    if (length < 0 || length > MAX_FRAME_BYTES) {
      throw new IOException("invalid frame length: " + length);
    }
    byte[] payload = new byte[length];
    readFully(input, payload);
    return payload;
  }

  static void writeFrame(OutputStream output, byte[] payload) throws IOException {
    if (payload.length > MAX_FRAME_BYTES) {
      throw new IOException("frame too large: " + payload.length);
    }
    output.write((payload.length >>> 24) & 0xff);
    output.write((payload.length >>> 16) & 0xff);
    output.write((payload.length >>> 8) & 0xff);
    output.write(payload.length & 0xff);
    output.write(payload);
    output.flush();
  }

  private static int readByte(InputStream input) throws IOException {
    int value = input.read();
    if (value == -1) {
      throw new EOFException("truncated frame header");
    }
    return value;
  }

  private static void readFully(InputStream input, byte[] payload) throws IOException {
    int offset = 0;
    while (offset < payload.length) {
      int count = input.read(payload, offset, payload.length - offset);
      if (count == -1) {
        throw new EOFException("truncated frame payload");
      }
      offset += count;
    }
  }
}
