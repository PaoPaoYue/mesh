package com.github.paopaoyue.mesh.rpc;

import com.github.paopaoyue.mesh.rpc.config.Properties;
import com.github.paopaoyue.mesh.rpc.config.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.proto.Base;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import com.google.protobuf.Any;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@SpringBootTest(classes = RpcAutoConfiguration.class)
class RpcTests {

    Logger logger = LoggerFactory.getLogger(RpcTests.class);

    @Autowired
    ApplicationContext context;

    @Autowired
    Properties prop;

    private static byte[] convertFixedInt32(int val) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) (val >> (i * 8) & 0xff);
        }
        return bytes;
    }

    @Test
    public void testBufferRead() throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        Protocol.Packet packet = Protocol.Packet.newBuilder()
                .setHeader(Protocol.PacketHeader.newBuilder().setLength(1024 * 1024 * 532 + 135 + 149).setFlag(1).build())
                .setTraceInfo(Protocol.TraceInfo.newBuilder())
                .buildPartial();
        byte[] bytes = packet.toByteArray();
        int len = bytes.length;
        byte[] pre = new byte[len / 2 + 1];
        byte[] suf = new byte[len - len / 2 - 1];

        java.lang.System.arraycopy(bytes, 0, pre, 0, len / 2 + 1);
        java.lang.System.arraycopy(bytes, len / 2 + 1, suf, 0, len - len / 2 - 1);
        buffer.put(bytes);
        buffer.put(pre);
        buffer.put(suf);
        buffer.flip();
        buffer.limit(len);


//        Protocol.PacketHeader header2 = Protocol.PacketHeader.parseFrom(buffer);
//        System.out.println(header2.toString());
//
//        buffer.rewind();

        Protocol.Packet packet2 = Protocol.Packet.parseFrom(buffer);

        buffer.position(len);

        buffer.limit(len + len);

        packet2 = Protocol.Packet.parseFrom(buffer);

        buffer.compact();
//        buffer.put(suf);
//        buffer.flip();

//        header2 = Protocol.PacketHeader.parseFrom(buffer);
//        System.out.println(header2.toString());
//
//        buffer.rewind();

//        packet2 = Protocol.Packet.parseFrom(buffer);
//        System.out.println(packet2.toString());
    }

    @Test
    public void testByteRead() throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        Protocol.Packet packet = Protocol.Packet.newBuilder().setBody(Any.pack(RespBaseUtil.ErrorRespBase(Base.StatusCode.NETWORK_ERROR_VALUE, "test"))).build();

        byte[] bytes = packet.toByteArray();
        Protocol.Packet packet1 = Protocol.Packet.parseFrom(bytes);

        Object response = packet1.getBody().unpack(Base.RespBase.class);
        logger.info("response: {}", packet.getClass().getSimpleName());
    }

    public class ByteBufferBackedInputStream extends InputStream {

        ByteBuffer buf;

        public ByteBufferBackedInputStream(ByteBuffer buf, int length) {
            this.buf = buf;
        }

        public int read() throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        public int read(byte[] bytes, int off, int len)
                throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }

            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }
    }

}
