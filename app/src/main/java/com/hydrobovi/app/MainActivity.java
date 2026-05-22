package com.hydrobovi.app;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final UUID HC05_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_PERMISSIONS = 1;

    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private InputStream inputStream;
    private Thread readThread;
    private boolean isConnected = false;

    private TextView lblEstado, lblBajo, lblAlto, lblCaudal, lblValvula, lblConexion;
    private Button btnConectar, btnDesconectar;

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private StringBuilder dataBuffer = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lblEstado = findViewById(R.id.lblEstado);
        lblBajo = findViewById(R.id.lblBajo);
        lblAlto = findViewById(R.id.lblAlto);
        lblCaudal = findViewById(R.id.lblCaudal);
        lblValvula = findViewById(R.id.lblValvula);
        lblConexion = findViewById(R.id.lblConexion);
        btnConectar = findViewById(R.id.btnConectar);
        btnDesconectar = findViewById(R.id.btnDesconectar);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth no disponible", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        requestPermissions();

        btnConectar.setOnClickListener(v -> mostrarDispositivos());
        btnDesconectar.setOnClickListener(v -> desconectar());
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, REQUEST_PERMISSIONS);
    }

    private void mostrarDispositivos() {
        if (!btAdapter.isEnabled()) {
            Toast.makeText(this, "Activa el Bluetooth primero", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Set<BluetoothDevice> dispositivos = btAdapter.getBondedDevices();
            if (dispositivos.isEmpty()) {
                Toast.makeText(this, "No hay dispositivos vinculados", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<String> nombres = new ArrayList<>();
            ArrayList<BluetoothDevice> lista = new ArrayList<>();
            for (BluetoothDevice d : dispositivos) {
                nombres.add(d.getName() + "\n" + d.getAddress());
                lista.add(d);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Selecciona el HC-05");
            builder.setItems(nombres.toArray(new String[0]), (dialog, which) -> {
                conectar(lista.get(which));
            });
            builder.show();
        } catch (SecurityException e) {
            Toast.makeText(this, "Permisos Bluetooth denegados", Toast.LENGTH_SHORT).show();
        }
    }

    private void conectar(BluetoothDevice device) {
        new Thread(() -> {
            try {
                btSocket = device.createRfcommSocketToServiceRecord(HC05_UUID);
                btAdapter.cancelDiscovery();
                btSocket.connect();
                inputStream = btSocket.getInputStream();
                isConnected = true;

                mainHandler.post(() -> {
                    lblConexion.setText("✅ Conectado");
                    lblConexion.setTextColor(Color.parseColor("#2A9D8F"));
                    Toast.makeText(this, "Conectado a " + device.getName(), Toast.LENGTH_SHORT).show();
                });

                iniciarLectura();
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void iniciarLectura() {
        readThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (isConnected) {
                try {
                    int bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String chunk = new String(buffer, 0, bytes);
                        dataBuffer.append(chunk);

                        int newlineIndex;
                        while ((newlineIndex = dataBuffer.indexOf("\n")) != -1) {
                            String linea = dataBuffer.substring(0, newlineIndex).trim();
                            dataBuffer.delete(0, newlineIndex + 1);
                            mainHandler.post(() -> procesarLinea(linea));
                        }
                    }
                } catch (IOException e) {
                    isConnected = false;
                    mainHandler.post(() -> {
                        lblConexion.setText("❌ Desconectado");
                        lblConexion.setTextColor(Color.parseColor("#E63946"));
                    });
                    break;
                }
            }
        });
        readThread.start();
    }

    private void procesarLinea(String linea) {
        // ESTADO
        if (linea.contains("Estado:VACIO") || linea.contains("VACIO")) {
            lblEstado.setText("⚠️ Estado: VACÍO");
            lblEstado.setBackgroundColor(Color.parseColor("#E63946"));
        } else if (linea.contains("LLENANDO")) {
            lblEstado.setText("💧 Estado: LLENANDO");
            lblEstado.setBackgroundColor(Color.parseColor("#0077B6"));
        } else if (linea.contains("LLENO")) {
            lblEstado.setText("✅ Estado: LLENO");
            lblEstado.setBackgroundColor(Color.parseColor("#2A9D8F"));
        }

        // FLOTADOR BAJO
        if (linea.contains("Bajo:ALTO")) {
            lblBajo.setText("🟢 Flotador Bajo: ALTO");
            lblBajo.setTextColor(Color.parseColor("#2A9D8F"));
        } else if (linea.contains("Bajo:BAJO")) {
            lblBajo.setText("🔴 Flotador Bajo: BAJO");
            lblBajo.setTextColor(Color.parseColor("#E63946"));
        }

        // FLOTADOR ALTO
        if (linea.contains("Alto:ALTO")) {
            lblAlto.setText("🟢 Flotador Alto: ALTO");
            lblAlto.setTextColor(Color.parseColor("#2A9D8F"));
        } else if (linea.contains("Alto:BAJO")) {
            lblAlto.setText("🔴 Flotador Alto: BAJO");
            lblAlto.setTextColor(Color.parseColor("#E63946"));
        }

        // VALVULA
        if (linea.contains("Valvula:ACTIVA")) {
            lblValvula.setText("🔧 Válvula: ACTIVA");
            lblValvula.setTextColor(Color.parseColor("#2A9D8F"));
        } else if (linea.contains("Valvula:INACTIVA")) {
            lblValvula.setText("🔧 Válvula: INACTIVA");
            lblValvula.setTextColor(Color.parseColor("#E63946"));
        }

        // CAUDAL
        int idx = linea.indexOf("Caudal:");
        if (idx != -1) {
            String resto = linea.substring(idx + 7);
            int fin = resto.indexOf("L/m");
            if (fin != -1) {
                String valor = resto.substring(0, fin).trim();
                lblCaudal.setText("💧 Caudal: " + valor + " L/m");
            }
        }
    }

    private void desconectar() {
        try {
            isConnected = false;
            if (btSocket != null) btSocket.close();
            if (inputStream != null) inputStream.close();
            lblConexion.setText("❌ Desconectado");
            lblConexion.setTextColor(Color.parseColor("#E63946"));
            lblEstado.setText("Estado: --");
            lblEstado.setBackgroundColor(Color.parseColor("#0F3460"));
            lblBajo.setText("Flotador Bajo: --");
            lblBajo.setTextColor(Color.WHITE);
            lblAlto.setText("Flotador Alto: --");
            lblAlto.setTextColor(Color.WHITE);
            lblCaudal.setText("Caudal: --");
            lblValvula.setText("Válvula: --");
            lblValvula.setTextColor(Color.WHITE);
            Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        desconectar();
    }
}
