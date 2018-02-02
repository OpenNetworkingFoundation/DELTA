# Decoy Agent Manager to remotely test Agents
import socket
import time
import struct
import sys
import threading
import signal
import urwid

conn_list = {}

def writeUTF(sock, msg):
    size = len(msg)
    sock.send(struct.pack("!H", size) + msg)

def socket_handler(s):
    while True:
        conn, addr = s.accept()
        print '[Agent-Manager Tester] Connected by', addr
        data = conn.recv(1024).decode('utf-8')
        print '[Agent-Manager Tester] Received: ', data
        writeUTF(conn, "OK")
        if "ChannelAgent" in data:
            conn_list['c'] = conn
            cfg = 'config,version:1.3,nic:lo,port:6633,controller_ip:127.0.0.1,switch_ip:127.0.0.1,handler:dummy,cbench:/home/vagrant/oflops/cbench/'
            writeUTF(conn, cfg)
        elif "AppAgent" in data:
            conn_list['a'] = conn
            print "[Agent-Manager Tester] AppAgent connected" 

def input_handler():
    while True:
        try:
            cmd = raw_input("Enter Attack Code: \n")
            print "cmd : ", cmd
            cmd_arr = cmd.split(" ")
            thd_num = cmd_arr[0]
            cmd_num = cmd_arr[1]
            print thd_num
            print cmd_num
            conn = conn_list[thd_num]
            print conn
            writeUTF(conn, cmd_num)
            data = conn.recv(1024).decode('utf-8')
            print '[AgentManger Tester] Received: ', data
        except Exception as e:
            print '[ERROR] not avialable input\n'

def has_live_threads(threads):
    return True in [t.isAlive() for t in threads]

if __name__ == "__main__":
    HOST = ''
    PORT = 3366
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind((HOST, PORT))
    s.listen(1)
    threads = []

    handler_thd = threading.Thread(target=socket_handler, args=(s,))
    handler_thd.daemon = True
    handler_thd.start()
    threads.append(handler_thd)
    
    input_thd = threading.Thread(target=input_handler)
    input_thd.daemon = True
    input_thd.start()
    threads.append(input_thd)

    while has_live_threads(threads):
        try:
            [t.join(100) for t in threads if t is not None and t.isAlive()]
        except KeyboardInterrupt:
            print "Sending kill to threads..."
            sys.exit(1)
