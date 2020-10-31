package com.ivysoft.BTmessenger;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;


public class ChatUtils {
    private Context context;
    private Handler handler;

    private final UUID APP_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;

    public ChatUtils(Context context, Handler handler){
        this.context = context;
        this. handler = handler;

        state = STATE_NONE;

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
    private synchronized void start(){

    }

    // Método que termina el chat
    private  synchronized void stop(){

    }
    // Crear el hilo de conexión (socket)
    private class ConnectThread extends Thread{
        private final BluetoothSocket socket;
        private final BluetoothDevice device;
        // Asignar hilo de conexión
        public ConnectThread(BluetoothDevice device){
            this.device = device;

            //Crear socket temporal
            BluetoothSocket tmp = null;
            try{
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            }catch (IOException e){
                Log.e("Connect->Constructor", e.toString());
            }

            socket = tmp;
        }

    }
}
