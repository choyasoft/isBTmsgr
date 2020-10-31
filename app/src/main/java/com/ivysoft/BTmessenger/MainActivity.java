package com.ivysoft.BTmessenger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
private Context context;
private BluetoothAdapter bluetoothAdapter;
private ChatUtils chatUtils;

private final int LOCATION_PERMISSION_REQUEST = 101;
private final int SELECT_DEVICE = 102;


public static final int MESSAGE_STATE_CHANGED = 0;
public static final int MESSAGE_READ = 1;
public static final int MESSAGE_WRITE = 2;
public static final int MESSAGE_DEVICE_NAME = 3;
public static final int MESSAGE_TOAST = 4;


public static final String TOAST = "toast";
public static final String DEVICE_NAME = "devicename";
private String connectedDevice;

private Handler handler = new Handler(new Handler.Callback() {
    @Override
    public boolean handleMessage(@NonNull Message message) {
        switch (message.what){
            case MESSAGE_STATE_CHANGED:
                switch (message.arg1){
                    case ChatUtils.STATE_NONE:
                        setState("Desconectado");
                        break;
                    case ChatUtils.STATE_LISTEN:
                        setState("Desconectado");
                        break;
                    case ChatUtils.STATE_CONNECTING:
                        setState("Conectando...");
                        break;
                    case ChatUtils.STATE_CONNECTED:
                        setState("Conectado: "+ connectedDevice);
                        break;
                }
                break;
            case MESSAGE_READ:
                break;
            case MESSAGE_WRITE:
                break;
            case MESSAGE_DEVICE_NAME:
                connectedDevice = message.getData().getString(DEVICE_NAME);
                Toast.makeText(context, connectedDevice,Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                break;

        }
        return false;
    }
});

private void setState(CharSequence subTitle){
    getSupportActionBar().setSubtitle(subTitle);
}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        chatUtils = new ChatUtils(context, handler);
        initBluetooth();
    }
    // Método para iniciar Bluetooth
    private void initBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Toast.makeText(context, "No se ha encontrado Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }

    // Crear un Item de menú
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }


     // Opciones del menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){

            case R.id.menu_search_devices:
                checkPermissions();
                return true;

            case R.id.menu_enable_bluetooth:
                enableBluetooth();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    // Método para comprobar los permisos necesarios (de Bluetooth)
    private void checkPermissions(){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST );

        }else{
            Intent intent = new Intent(context, DeviceListActivity.class);
            startActivityForResult(intent, SELECT_DEVICE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == SELECT_DEVICE && resultCode == RESULT_OK){
            String address = data.getStringExtra("Dirección MAC");
            Toast.  makeText(context, "Dirección MAC: "+ address, Toast.LENGTH_SHORT).show();

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(context, DeviceListActivity.class);
                startActivityForResult(intent, SELECT_DEVICE);
            } else {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("El permiso de localización es requerido.\n Por favor acepta el permiso.")
                        .setPositiveButton("Permitir", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                checkPermissions();
                            }
                        })
                        .setNegativeButton("Denegar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                MainActivity.this.finish();
                            }
                        })
                        .show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
         }
        }
        private void enableBluetooth () {
        // Si bluetooth no está activado, activarlo y mostrar un mensaje:
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
                Toast.makeText(context, "Activando Bluetooth...", Toast.LENGTH_SHORT).show();
            }
            // Buscar dispositivos
            if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
                Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoveryIntent);
            }
        }

    }