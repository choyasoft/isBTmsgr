package com.ivysoft.BTmessenger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.icu.util.Output;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class ChatUtils {
    private Context context;
    private Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private AcceptThread acceptThread;

    private final UUID APP_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private final String APP_NAME = "ivySoft BTmsgr";


    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;

    public ChatUtils(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;

        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    // Obtiene el estado del chat
    public int getState() {
        return state;
    }

    // Setter que asigna un estado al chat (changed, connected, connecting, etc)
    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget();
    }

    // Método que comienza el chat
    private synchronized void start() {
    if (connectThread!=null){
        connectThread.cancel();
        connectThread = null;
    }

    if (acceptThread == null){
        acceptThread = new AcceptThread();
        acceptThread.start();
    }
        setState(STATE_LISTEN);
    }

    // Método que termina el chat
    public synchronized void stop() {
    if(connectThread != null){
        connectThread.cancel();
        connectThread = null;
    }
    if (acceptThread != null){
        acceptThread.cancel();
        acceptThread = null;
    }
        setState(STATE_NONE);
    }

    // Método de conexión del hilo
    public void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            connectThread.cancel();
            connectThread = null;
        }
        connectThread = new ConnectThread(device);
        connectThread.start();

        setState(STATE_CONNECTING);
    }
        // Clase hilo que acepta como server
    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;
                    try{
                        tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
                    }catch (IOException e){
                        Log.e("Accept->Constructor", e.toString());
                    }
                    serverSocket = tmp;

        }
        // Método que acepta/arranca/cierra el socket servidor
        public void run(){
            BluetoothSocket socket = null;
            try{
            socket = serverSocket.accept();
            }catch (IOException e){
                Log.e("Accept->Run", e.toString());
                try{
                    serverSocket.close();
                }catch (IOException e1){
                    Log.e("Accept->Close", e.toString());
                }
            }

            if (socket != null){
                switch (state){
                    case STATE_LISTEN:
                        break;
                    case STATE_CONNECTING:
                        connect(socket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                       break;
                    case STATE_CONNECTED:
                        try{
                            socket.close();
                        }catch (IOException e){
                            Log.e("Accept->CloseSocket", e.toString());
                        }
                        break;
                }
            }

        }

        public void cancel(){
            try{
                serverSocket.close();
            }catch (IOException e){
                Log.e("Accept->CloseServer", e.toString());
            }
        }
    }
    // Clase hilo conectado
    private class ConnectedThread extends Thread{
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket){
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }catch (IOException e){
                Log.e("Connected->d", e.toString());
            }
            inputStream = tmpIn;
            outputStream = tmpOut;
        }
        // Método iniciar connected
        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            try{
                bytes= inputStream.read(buffer);
                handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
            }catch (IOException e){
                connectionLost(){

                }
            }
        }
        private void connectionLost(){
            Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(MainActivity.TOAST, "Se ha perdido la conexión");
            message.setData(bundle);
            handler.sendMessage(message);

            ChatUtils.this.start();
        }

    }


    // Crear el hilo de conexión cliente (socket)
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        // Asignar hilo de conexión
        public ConnectThread(BluetoothDevice device) {
            this.device = device;

            //Crear socket temporal
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("Connect->Constructor", e.toString());
            }

            socket = tmp;
        }

        // Método que conecta y desconecta el socket
        public void run() {
            try {
                socket.connect();
            } catch (IOException e) {
                Log.e("Connect->AbreSocket", e.toString());
                try {
                    socket.close();
                } catch (IOException e1) {
                    Log.e("Connect->CierraSocket", e.toString());

                }
                connectionFailed();
                return;

            }
                synchronized (ChatUtils.this) {
                    connectThread = null;
                }

            connect(device);
        }

        // Método que cancela la conexión
        public void cancel() {
            try {
                socket.close();

            } catch (IOException e) {
                Log.e("Connect->Cancel", e.toString());
            }
        }


        private synchronized void connectionFailed() {
            Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(MainActivity.TOAST, "No se puede conectar");
            message.setData(bundle);
            handler.sendMessage(message);

            ChatUtils.this.start();
        }

        // Método que realiza la conexión al dispositivo y cambia su estado a connected
        private synchronized void connect(BluetoothDevice device) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
            Message message = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(MainActivity.DEVICE_NAME, device.getName());
            message.setData(bundle);
            handler.sendMessage(message);

            setState(STATE_CONNECTED);
        }
    }
}
