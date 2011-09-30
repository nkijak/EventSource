package com.kinnack;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import android.util.Log;

public class EventSource
{
    private static final int EVENTSOURCE_PORT = 3000;
    private Selector selector; 
    private boolean alive = true;
    private boolean headerSent;
    private Thread thread;
    private EventSourceListener listener;
    
    public EventSource(String host_, EventSourceListener listener_) throws IOException{
        selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        
        socketChannel.connect(new InetSocketAddress(host_, EVENTSOURCE_PORT));
        SelectionKey selectionKey = socketChannel.register(selector, socketChannel.validOps());
        listener = listener_;
        start();
    }
    
    public void kill() {
        alive = false;
        //thread.destroy();
    }
    
    private void start() {
        new Thread(new Runnable()
        {
            private ByteBuffer buffer;
            
            @Override
            public void run()
            {
                buffer = ByteBuffer.allocateDirect(1024);
                while (alive) {
                    try
                    {
                        selector.select();
                    } catch (IOException e)
                    {
                        Log.e("EventSource","Error selecting from selector", e);
                        break;
                    }
                    
                    for(SelectionKey key : selector.selectedKeys()) {
                        if (!key.isValid()) {
                            Log.i("EventSource:start", "Got an invalid key (wtf does that mean?)");
                            continue;
                        }
                        SocketChannel channel = (SocketChannel)key.channel();

                        if (key.isConnectable() && !headerSent) {
                            Log.d("EventSource.run", "CONNECTION ESTABLISHED");
                            try
                            {
                                
                                if (channel.finishConnect()) {
                                    listener.onStart();
                                } else {
                                    Log.e("EventSource", "Error connecting to server");
                                    key.cancel();
                                }
                            } catch (Exception e)
                            {
                                Log.e("EventSource.run@connect", "Unknown error during connection", e);
                                key.cancel();
                            }
                            
                        }
                        
                        if (key.isReadable()) {
                            Log.d("EventSource.run", "DATA AVAILABLE");
                            buffer.clear();
                            try
                            {
                                int numberRead = channel.read(buffer);
                                if (numberRead == -1) { 
                                    channel.close();
                                    continue;
                                }
                                
                                if (numberRead == 0) continue;
                                Log.d("EventSource.run@read", "There are "+numberRead+" bytes available");
                                
                                buffer.flip();
                                byte[] dump = new byte[numberRead];
                                buffer.get(dump);
                                String value = new String(dump);
                                Log.d("EventSource:run@message", "VALUE ["+value+"]");
                                listener.onMessage(value);
                            } catch (IOException e)
                            {
                                Log.e("EventSoure:run", "Error reading from channel", e);
                            }
                        }
                        
                        if (key.isWritable() && !headerSent) {
                            buffer.clear();
                            Log.d("EventSource.run", "AVAILABLE TO WRITE");
                            String header = "GET /events HTTP/1.0\r\n\r\n";
                            Log.d("EventSource.run", "Buffer can handle " + buffer.capacity()+ ", header ["+header+"] is "+header.getBytes().length);
                            try
                            {
                                buffer.put(header.getBytes("UTF-8"));
                                buffer.flip();
                                channel.write(buffer);
                                headerSent = true;
                            } catch (IOException e)
                            {
                                Log.e("EventSource:run@header", "Error writing header", e);
                            }
                        }
                    }
                }
                
            }
        }).start();
    }
    
    public interface EventSourceListener {
        abstract public void onMessage(String message);
        abstract public void onStart();
        abstract public void onClose();
    }
    
}
