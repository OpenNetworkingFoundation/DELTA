package org.deltaproject.channelagent.dummy;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Longs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.types.OFHelloElement;
import org.projectfloodlight.openflow.types.U16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by seungsoo on 11/07/2017.
 */
public class DummyController extends Thread {
    private static final Logger log = LoggerFactory.getLogger(DummyController.class);

    public static final int HANDSHAKE_DEFAULT = 0;
    public static final int HANDSHAKE_NO_HELLO = 1;
    public static final int HANDSHAKE_INCOMPATIBLE_HELLO = 2;
    public static final int NO_HANDSHAKE = 3;

    public static final int MINIMUM_LENGTH = 8;

    private Socket targetSock;
    private InputStream in;
    private OutputStream out;

    /* for handler controller */
    private int port = 0;

    /* for OF message */
    private OFVersion version;
    private OFFactory factory;
    private OFMessageReader<OFMessage> reader;
    private OFMessage res;

    private int handShakeType;

    private long requestXid = 0xeeeeeeeel;
    private long startXid = 0xffffffff;
    private long cntXid = 1;
    private boolean handshaked = false;
    private boolean synack = false;

    private OFFlowAdd backupFlowAdd;
    private ServerSocket serverSock;

    public DummyController(byte ver, int port) {
        res = null;

        if (ver == 1) {
            version = OFVersion.OF_10;
            factory = OFFactories.getFactory(OFVersion.OF_10);
        } else if (ver == 4) {
            version = OFVersion.OF_13;
            factory = OFFactories.getFactory(OFVersion.OF_13);
        }

        reader = factory.getReader();
        this.port = port;
    }

    public void setHandShakeType(int type) {
        this.handShakeType = type;
    }

    public OFFactory getFactory() {
        return this.factory;
    }

    public byte[] readBytes() throws IOException {
        // Again, probably better to store these objects references in the
        // support class
        InputStream in = targetSock.getInputStream();
        DataInputStream dis = new DataInputStream(in);

        int len = dis.readInt();
        byte[] data = new byte[len];
        if (len > 0) {
            dis.readFully(data);
        }
        return data;
    }


    public void printError(OFMessage msg) {
        System.out.println(msg.toString());
    }

    public void sendMsg(OFMessage msg, int len) {
        ByteBuf buf;

        if (len == -1) {
            buf = PooledByteBufAllocator.DEFAULT.directBuffer(1024);
        } else {
            buf = PooledByteBufAllocator.DEFAULT.directBuffer(len);
        }

        msg.writeTo(buf);

        int length = buf.readableBytes();
        byte[] bytes = new byte[length];
        buf.getBytes(buf.readerIndex(), bytes);

        try {
            this.out.write(bytes, 0, length);
        } catch (IOException e) {
            // TODO Auto-gaenerated catch block
            e.printStackTrace();
        }

        buf.clear();
        buf.release();
    }

    public void setReqXid(long xid) {
        requestXid = xid;
    }

    public OFMessage getResponse() {
        return res;
    }

    public void sendRawMsg(byte[] msg) {
        try {
            this.out.write(msg, 0, msg.length);
        } catch (IOException e) {
            // TODO Auto-gaenerated catch block
            e.printStackTrace();
        }
    }

    public void sendRawMsg(ByteBuf buf) {
        int length = buf.readableBytes();
        byte[] bytes = new byte[length];
        buf.getBytes(buf.readerIndex(), bytes);

        try {
            this.out.write(bytes, 0, length);
        } catch (IOException e) {
            // TODO Auto-gaenerated catch block
            e.printStackTrace();
        }

        buf.clear();
        buf.release();
    }

    public OFFlowAdd getBackupFlowAdd() {
        return backupFlowAdd;
    }

    public ByteBuf sendFlowRemoved() throws OFParseError {
        OFFactory factory = OFFactories.getFactory(OFVersion.OF_10);
        long r_xid = 0xeeeeeeeel;

        OFFlowRemoved.Builder fab = factory.buildFlowRemoved();
        fab.setXid(r_xid);
        OFFlowRemoved hello = fab.build();

        ByteBuf buf = null;
        buf = PooledByteBufAllocator.DEFAULT.directBuffer(88);
        hello.writeTo(buf);

        byte[] bytes;
        int length = buf.readableBytes();
        bytes = new byte[length];
        buf.getBytes(buf.readerIndex(), bytes);

        try {
            this.out.write(bytes, 0, length);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return buf;
    }

    public ByteBuf sendPortStatus() throws OFParseError {
        OFFactory factory = OFFactories.getFactory(OFVersion.OF_10);
        long r_xid = 0xeeeeeeeel;

        OFPortStatus.Builder fab = factory.buildPortStatus();
        fab.setXid(r_xid);
        OFPortStatus hello = fab.build();

        ByteBuf buf = null;
        buf = PooledByteBufAllocator.DEFAULT.directBuffer(64);
        hello.writeTo(buf);

        byte[] bytes;
        int length = buf.readableBytes();
        bytes = new byte[length];
        buf.getBytes(buf.readerIndex(), bytes);

        try {
            this.out.write(bytes, 0, length);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return buf;
    }

    public void setTargetSock(Socket socket) {
        this.targetSock = socket;

        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listeningSwitch() {
        log.info("[Channel Agent] Listening switches on " + this.port);

        try {
            serverSock = new ServerSocket(port);
            serverSock.setReuseAddress(true);
            Socket temp = serverSock.accept();
            log.info("[Channel Agent] Switch connected from  " + temp.toString());
            setTargetSock(temp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getHandshaked() {
        return this.handshaked;
    }

    public void sendExperimenter(long xid) {
        byte[] msg = DummyData.hexStringToByteArray(DummyData.VENDOR);
        byte[] xidbytes = Longs.toByteArray(xid);
        System.arraycopy(xidbytes, 4, msg, 4, 4);

        if (this.version == OFVersion.OF_13) {
            msg[0] = (byte) 0x04;
        }

        sendRawMsg(msg);
    }

    public void sendStatReq(long xid) throws OFParseError {
        if (this.version == OFVersion.OF_10) {
            byte[] msg = DummyData.hexStringToByteArray(DummyData.STATS_REQ);
            byte[] xidbytes = Longs.toByteArray(xid);
            System.arraycopy(xidbytes, 4, msg, 4, 4);

            sendRawMsg(msg);
            return;
        } else if (this.version == OFVersion.OF_13) {
            OFAggregateStatsRequest.Builder builder = factory.buildAggregateStatsRequest();
            Set<OFStatsRequestFlags> flagset = ImmutableSet.<OFStatsRequestFlags>of();

            builder.setFlags(flagset);
            builder.setXid(xid);

            sendMsg(builder.build(), -1);
        }
    }

    public void sendSetConfig(long xid) throws OFParseError {
        OFSetConfig.Builder builder = factory.buildSetConfig();
        builder.setMissSendLen(0xffff);
        builder.setXid(xid);

        sendMsg(builder.build(), -1);
    }

    public void sendGetConfigReq(long xid) throws OFParseError {
        OFGetConfigRequest.Builder builder = factory.buildGetConfigRequest();
        builder.setXid(xid);

        sendMsg(builder.build(), -1);
    }

    public void sendFeatureReq(long xid) throws OFParseError {
        OFFeaturesRequest.Builder builder = factory.buildFeaturesRequest();
        builder.setXid(xid);

        sendMsg(builder.build(), -1);
    }

    public void sendEchoReply(long xid) {
        OFEchoReply.Builder builder = factory.buildEchoReply();
        builder.setXid(xid);

        sendMsg(builder.build(), -1);
    }

    public void sendHello(long xid) throws OFParseError {
        if (handShakeType == HANDSHAKE_NO_HELLO) {
            sendFeatureReq(startXid - (cntXid++));
            return;
        } else if (handShakeType == HANDSHAKE_INCOMPATIBLE_HELLO) {
            if(version == OFVersion.OF_10) {
                byte[] rawPkt = new byte[8];
                rawPkt[0] = (byte) 0xee;    // unsupported version
                rawPkt[1] = 0x00;           // hello
                rawPkt[2] = 0x00;
                rawPkt[3] = 0x08;

                rawPkt[4] = (byte) 0xee;
                rawPkt[5] = (byte) 0xee;
                rawPkt[6] = (byte) 0xee;
                rawPkt[7] = (byte) 0xee;

                log.info("[Channel Agent] Send msg: OF_HELLO with unsupported version -> 0xee");
                this.sendRawMsg(rawPkt);
            } else if (version == OFVersion.OF_13) {
                byte[] rawPkt = new byte[16];
                rawPkt[0] = (byte) 0xee;    // unsupported version
                rawPkt[1] = 0x00;           // hello
                rawPkt[2] = 0x00;
                rawPkt[3] = 0x08;

                rawPkt[4] = (byte) 0xee;
                rawPkt[5] = (byte) 0xee;
                rawPkt[6] = (byte) 0xee;
                rawPkt[7] = (byte) 0xee;

                log.info("[Channel Agent] Send msg: OF_HELLO with unsupported version. -> 0xee");
                this.sendRawMsg(rawPkt);
            }
            return;
        } else if (handShakeType == NO_HANDSHAKE)
            return;

        OFHello.Builder fab = factory.buildHello();
        fab.setXid(xid);

        if (version != OFVersion.OF_10) {
            OFHelloElem.Builder heb = factory.buildHelloElemVersionbitmap();
            List<OFHelloElem> list = new ArrayList<OFHelloElem>();
            list.add(heb.build());
            fab.setElements(list);
        }

        OFHello hello = fab.build();
        sendMsg(hello, MINIMUM_LENGTH);
    }

    public boolean parseOFMsg(byte[] recv, int len) throws OFParseError {
        // for OpenFlow Message
        byte[] rawMsg = new byte[len];
        System.arraycopy(recv, 0, rawMsg, 0, len);
        ByteBuf bb = Unpooled.copiedBuffer(rawMsg);

        int totalLen = bb.readableBytes();
        int offset = bb.readerIndex();

        while (offset < totalLen) {
            bb.readerIndex(offset);

            byte version = bb.readByte();
            bb.readByte();
            int length = U16.f(bb.readShort());
            bb.readerIndex(offset);

            if (length < MINIMUM_LENGTH)
                throw new OFParseError("Wrong length: Expected to be >= " + MINIMUM_LENGTH + ", was: " + length);

            try {
                OFMessage message = reader.readFrom(bb);

                long xid = message.getXid();

                if (message.getType() == OFType.HELLO) {
                    sendHello(startXid);

                    if (handShakeType != HANDSHAKE_DEFAULT)
                        return true;

                    sendFeatureReq(startXid - (cntXid++));
                } else if (message.getType() == OFType.FEATURES_REPLY) {
                    sendSetConfig(startXid - (cntXid++));
                    sendGetConfigReq(startXid - (cntXid++));
                } else if (message.getType() == OFType.GET_CONFIG_REPLY) {
                    sendStatReq(startXid - (cntXid++));
                } else if (message.getType() == OFType.STATS_REPLY) {
                    // sendExperimenter(startXid - (cntXid++));

                    handshaked = true;
                    log.info("[Channel Agent] recieve STATS_REPLY");
                } else if (message.getType() == OFType.ECHO_REQUEST) {
                    sendEchoReply(xid);
                } else if (message.getType() == OFType.ERROR) {
                    if (xid == this.requestXid) {
                        res = message;
                    }
                }

                if (xid == this.requestXid) {
                    res = message;
                }

            } catch (OFParseError e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            offset += length;
        }

        bb.clear();
        return true;
    }

    public boolean isSockClosed() {
        return serverSock.isClosed();
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        byte[] recv;
        int readlen;
        boolean ack = false;

        try {
            while (!Thread.currentThread().isInterrupted()) {
                recv = new byte[2048];
                if ((readlen = in.read(recv, 0, recv.length)) != -1) {
                    parseOFMsg(recv, readlen);
                } else {
                    in.close();
                    out.close();
                    serverSock.close();
                    targetSock.close();
                    break; // end of connection
                }
            }
        } catch (Exception e) {
            // if any error occurs
            e.printStackTrace();

        } finally {
            try {
                in.close();
                out.close();
                serverSock.close();
                targetSock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
