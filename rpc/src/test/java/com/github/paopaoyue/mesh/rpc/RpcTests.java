package com.github.paopaoyue.mesh.rpc;

import com.github.paopaoyue.mesh.rpc.config.Properties;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

@SpringBootTest(classes = RpcAutoConfiguration.class)
class RpcTests {

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
        System.out.println(Arrays.toString(convertFixedInt32(1024 * 1024 * 532 + 135 + 149)));

        Protocol.Packet packet = Protocol.Packet.newBuilder()
                .setHeader(Protocol.PacketHeader.newBuilder().setLength(1024 * 1024 * 532 + 135 + 149).setFlag(1).build())
                .setTraceInfo(Protocol.TraceInfo.newBuilder())
                .buildPartial();
        System.out.println(packet.toString());
        byte[] bytes = packet.toByteArray();
        int len = bytes.length;
        byte[] pre = new byte[len / 2 + 1];
        byte[] suf = new byte[len - len / 2 - 1];
        System.arraycopy(bytes, 0, pre, 0, len / 2 + 1);
        System.arraycopy(bytes, len / 2 + 1, suf, 0, len - len / 2 - 1);
        buffer.put(bytes);
        buffer.put(pre);
        buffer.put(suf);
        buffer.flip();
        buffer.limit(len);

        System.out.println(Arrays.toString(bytes));

//        Protocol.PacketHeader header2 = Protocol.PacketHeader.parseFrom(buffer);
//        System.out.println(header2.toString());
//
//        buffer.rewind();

        Protocol.Packet packet2 = Protocol.Packet.parseFrom(buffer);
        System.out.println(packet2.toString());

        buffer.position(len);

        buffer.limit(len + len);

        packet2 = Protocol.Packet.parseFrom(buffer);
        System.out.println(packet2.toString());

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
