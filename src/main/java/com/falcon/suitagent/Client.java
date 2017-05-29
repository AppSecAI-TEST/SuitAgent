/*
 * www.yiji.com Inc.
 * Copyright (c) 2016 All Rights Reserved
 */
package com.falcon.suitagent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/*
 * 修订记录:
 * guqiu@yiji.com 2016-06-22 17:48 创建
 */

/**
 * Agent主服务通信类
 * @author guqiu@yiji.com
 */
public class Client {

    private SocketChannel socketChannel = null;
    private Selector selector;

    private final ByteBuffer sendBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);

    private Charset charset = Charset.forName("UTF-8");

    private boolean shutdown = false;

    /**
     * 启动client服务
     * @throws IOException
     */
    public void start(int port) throws IOException {
        //连接端口
        InetSocketAddress inetSocketAddress = new InetSocketAddress(port);
        System.out.println("Socket Connect To " + inetSocketAddress.getAddress().toString() + ":" + inetSocketAddress.getPort());
        socketChannel = SocketChannel.open(inetSocketAddress);

        //设置非阻塞模式
        socketChannel.configureBlocking(false);

        //写出客户端标识
        sendMessage("I am is Falcon Agent Client");
        if(selector == null){
            //与服务器的连接建立成功
            selector = Selector.open();
        }

    }

    /**
     * 发送关闭服务命令
     */
    public void sendCloseCommend(){
        sendMessage("exit\r\n");
    }

    /**
     * 发送信息
     */
    private void sendMessage(String message) {
        sendBuffer.put(encode(message + "\r\n"));
    }

    /**
     * 注册读写事件，轮训发生的事件
     * @throws IOException
     */
    public void talk() throws IOException {
        socketChannel.register(selector,SelectionKey.OP_READ|SelectionKey.OP_WRITE|SelectionKey.OP_READ);
        while (selector.select() > 0){
            Set readyKeys = selector.selectedKeys();
            Iterator it = readyKeys.iterator();
            while (it.hasNext()){
                SelectionKey key = (SelectionKey) it.next();
                it.remove();
                if(key.isReadable()){
                    receive(key);
                }
                if(shutdown){
                    key.cancel();
                    return;
                }
                if(key.isWritable()){
                    send(key);
                }
            }
        }
    }

    /**
     * 接受Server的相应数据，存放在receiveBuffer中。
     * 若receiveBuffer已经满一行数据，则打印该数据，并把此数据从receiveBuffer中删除
     * 若打印的字符串为bye，则关闭
     * @param key
     * @throws IOException
     */
    private void receive(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        socketChannel.read(receiveBuffer);
        receiveBuffer.flip();
        String receiveData = decode(receiveBuffer);
        if(receiveData.contains("\n")){
            String outputData = receiveData.substring(0,receiveData.indexOf("\n") + 1);
            //若服务器响应echo:exit（exit由此client发出）则关闭client
            if("echo:exit\r\n".equals(outputData)){
                key.cancel();
                socketChannel.close();
                Agent.OUT.println("Agent已关闭成功");
                closeClient();
            }

            ByteBuffer temp = encode(outputData);
            receiveBuffer.position(temp.limit());
            receiveBuffer.compact();//删除已经打印的数据
        }
    }

    /**
     * 关闭连接资源
     * @throws IOException
     */
    public void closeClient() throws IOException {
        socketChannel.close();
        selector.close();
        shutdown = true;
    }

    /**
     * 把sendBuffer中的数据发送给Server，然后删除已发送的数据
     * @param key
     * @throws IOException
     */
    private void send(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        synchronized (sendBuffer){
            sendBuffer.flip();//把极限设置为位置，把位置设置为0
            socketChannel.write(sendBuffer);//发送数据
            sendBuffer.compact();//删除已经发送的数据
        }
    }

    public String decode(ByteBuffer buffer){
        CharBuffer charBuffer = charset.decode(buffer);
        return charBuffer.toString();
    }

    public ByteBuffer encode(String str){
        return charset.encode(str);
    }


}
