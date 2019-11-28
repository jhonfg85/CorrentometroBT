package com.android.correntometrobt;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Handler;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import android.view.Menu;

import org.w3c.dom.Text;

import androidx.appcompat.app.AppCompatActivity;

public class UserInterface extends AppCompatActivity {

    Button IdDesconectar;       //DESCONEXION DE BLUETOOTH
    TextView IdBufferIn;        //MUESTRA EL DATO ACTUAL
    TextView IdBuffer;          //SCROLL: HISTORIAL DE DATOS
    Button Reiniciar;           //BOTÓN REINICIAR
    TextView Ind_conexion;      //TEXTO QUE INDICA ESTADO DE CONEXION
    //-------------------------------------------
    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    // Identificador unico de servicio - SPP UUID
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String para la direccion MAC
    private static String address = null;
    private static final String filename = "correntometro.txt";
    private static int pause=0;
    public int estado=0;
    //-------------------------------------------

/////////////////////////////ONCREATE//////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);      //DESACTIVAR CAMBIO DE ORIENTACIÓN
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_interface);
        /////////CONEXIÓN DE ELEMENTOS DE INTERFAZ AL CÓDIGO//////////////////////
        IdDesconectar = (Button) findViewById(R.id.IdDesconectar);      //DESHACER CONEXIÓN
        IdBufferIn = (TextView) findViewById(R.id.IdBufferIn);      //SALIDA PARA DATO ACTUAL
        IdBuffer = (TextView) findViewById(R.id.Idbuffer);      //SALIDA PARA HISTORIAL DE DATOS
        Reiniciar = (Button) findViewById(R.id.reset);
        Ind_conexion = (TextView) findViewById(R.id.ind_conexion);
        ///////////////////////////////////////////////////////////////////////////
        IdBuffer.setMovementMethod(new ScrollingMovementMethod());
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    DataStringIN.append(readMessage);

                    int endOfLineIndex = DataStringIN.indexOf("#");
                    verificarConexion();
                    if (endOfLineIndex > 0) {
                        String dataInPrint = DataStringIN.substring(0, endOfLineIndex);
                        if(pause == 0) {
                            IdBufferIn.setText(dataInPrint);        //IMPRIME EN PANTALLA EL DATO ACTUAL
                            IdBuffer.append(dataInPrint);           //IMPRIME EN EL BUFFER DE HISTORIAL EL DATO ACTUAL
                        }
                            DataStringIN.delete(0, DataStringIN.length());      //LIBERA PARA RECIBIR OTRO DATO
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth adapter
        VerificarEstadoBT();            //VERIFICAR SI BT DEL CELULAR ESTÁ ACTIVADO
        //verificarConexion();            //VERIFICAR SI HAY CONEXION ACTIVA

        //AL PRESIONAR EL BOTÓN DESCONECTAR
        IdDesconectar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (btSocket!=null)
                {
                    try {btSocket.close();}
                    catch (IOException e)
                    { Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_SHORT).show();;}
                }
                finish();
            }
        });
        //ALERTA QUE SE ACTIVA AL PRESIONAR EL BOTÓN REINICIAR
        Reiniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset(v);
            }
        });
    }

 ///FUNCIONES PARA GUARDAR DATOS//////////////////
 @Override
 public boolean onCreateOptionsMenu(Menu menu) {
     // Inflate the menu; this adds items to the action bar if it is present.
       return false;
 }
/////////////////////////////////////////////////////
//////////////////////Pause/////////////////

    public void pause(View v) {
        //pause=1: recepción pausada. Pause = 0 se continua con la recepción
        pause += 1;
        if(pause == 1){
            Toast.makeText(this, "Recepción pausada", Toast.LENGTH_LONG).show();
            Ind_conexion.setText("PAUSADO");
        }
        else if(pause > 1){
            Toast.makeText(this, "Recibiendo datos", Toast.LENGTH_LONG).show();
            verificarConexion();
            pause = 0;
        }           //pause = 2:  Se presionó pausa 2 veces.
    }

/////////////////////////////////////////////////////////
//////////////////Reset///////////////////////////////////

    public void reset(View v){
        AlertDialog.Builder builder=new AlertDialog.Builder(UserInterface.this);

        builder.setIcon(R.mipmap.alerta).              //ICONO DE ALERTA
                setTitle("Reiniciar").
                setMessage("¿Limpiar Historial o borrar mediciones anteriores?").
                setPositiveButton("Limpiar Historial", new DialogInterface.OnClickListener() {
                    //LIMPIA EL HISTORIAL DE MEDICIONES ACTUAL
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        IdBuffer.setText("");
                    }
                }).
                setNegativeButton("Eliminar todo", new DialogInterface.OnClickListener() {
                    //SE ELIMINAN TODAS LAS MEDICIONES QUE SE HAN GUARDADO EN EL ARCHIVO CORRENTOMETRO.TXT
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        try{
                            String todo = "";
                            FileOutputStream archivo2 = null;
                            archivo2 = openFileOutput(filename, MODE_PRIVATE);
                            archivo2.write(todo.getBytes());
                            archivo2.flush();        //LIMPIA LOS DATOS DEL BUFFER LOS CUALES FUERON ESCRITOS EN ARCHIVO
                            archivo2.close();        //CERRAR ARCHIVO
                            IdBuffer.setText("");
                        }catch (IOException e){

                        }
                    }
                });
        AlertDialog alerta = builder.create();
        alerta.show();

    }

///////////////////////LOAD///////////////////////////////////////

    public void Load(View v) {
      /*  Intent load = new Intent(this, ActivityLoad.class);     //Activity para cargar datos
        if(pause == 0){ pause(v); }
        startActivity(load);        //Ir a la ventana para cargar archivo
    */
        String archivos[] = fileList();         //filelist() retorna los ficheros creados por la aplicación.

        if (existe(archivos, filename))
            try {
                //INPUTSTREAREADER PERMITE ABRIR UN ARCHIVO PARA LEERLO UTILIZANDO BUFFEREDREADER
                InputStreamReader archivo = new InputStreamReader(
                        openFileInput(filename));
                BufferedReader br = new BufferedReader(archivo);
                String linea = br.readLine();           //LEE CADA LINEA DEL ARCHIVO EXISTENTE
                String todo = "";
                while (linea != null) {
                    todo = todo + linea + "\n";
                    linea = br.readLine();
                }
                br.close();
                archivo.close();
                Toast.makeText(this, "Archivo cargado", Toast.LENGTH_LONG).show();
                IdBuffer.setText(todo);
            } catch (IOException e) {
            }

        else {
            Toast.makeText(this, "Archivo no existe", Toast.LENGTH_LONG).show();
        }

    }


    ////////////Save////////////////////////////////////////////
    public void Save(View v) {
        ////////////////EXPORTAR DATOS A UN ARCHIVO///////////////////////

        String archivos [] = fileList();         //filelist() retorna los ficheros creados por la aplicación.

        if (existe(archivos, filename))
            try {
                //INPUTSTREAREADER PERMITE ABRIR UN ARCHIVO PARA LEERLO UTILIZANDO BUFFEREDREADER
                InputStreamReader archivo = new InputStreamReader(
                        openFileInput(filename));
                BufferedReader br = new BufferedReader(archivo);
                String linea = br.readLine();           //LEE CADA LINEA DEL ARCHIVO EXISTENTE
                String todo = "";
                while (linea != null) {
                    todo = todo + linea + "\n";
                    linea = br.readLine();
                }
                String mediciones_actuales = IdBuffer.getText().toString();     //Mediciones nuevas que serán guardadas
                todo = todo + mediciones_actuales + "\n" + "MEDICIÓN GUARDADA" + "\n";
                br.close();
                archivo.close();
                //IdBuffer.setText(todo);

                FileOutputStream archivo2 = null;
                archivo2 = openFileOutput(filename, MODE_PRIVATE);
                archivo2.write(todo.getBytes());
                archivo2.flush();        //LIMPIA LOS DATOS DEL BUFFER LOS CUALES FUERON ESCRITOS EN ARCHIVO
                archivo2.close();        //CERRAR ARCHIVO

            } catch (IOException e) {
            }

        else {
            try {
                //CREACIÓN DE UN NUEVO ARCHIVO
                OutputStreamWriter archivo = new OutputStreamWriter(openFileOutput(
                        filename, Activity.MODE_PRIVATE));
                archivo.write(IdBuffer.getText().toString());
                archivo.flush();        //LIMPIA LOS DATOS DEL BUFFER LOS CUALES FUERON ESCRITOS EN ARCHIVO
                archivo.close();        //CERRAR ARCHIVO
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
            Toast.makeText(this, "Guardado en " + getFilesDir() + "/" + filename,
                    Toast.LENGTH_LONG).show();
    }

    public void verificarConexion() {
        /*BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (BluetoothHeadset.STATE_CONNECTED == 1) {
            Ind_conexion.setText("Conectado");     //MUESTRA ESTADO CONECTADO EN PANTALLA
        }
        else {
                Ind_conexion.setText("Desconectado");       //MUESTRA ESTADO DESCONECTADO EN PANTALLA
                Ind_conexion.setTextColor(Color.BLACK);
        }*/
        if(estado == 1){ Ind_conexion.setText("Conectado"); }     //
        else {
            Ind_conexion.setText("Desconectado");
            Ind_conexion.setTextColor(Color.BLACK);
        }
    }
    /////////BUSCA EL ARCHIVO EN ALMACENAMIENTO INTERNO///////////////
    private boolean existe(String archivos [], String archbusca) {
        for (int f = 0; f < archivos.length; f++)
            if (archbusca.equals(archivos[f])) {
                return true;
            }
        return false;
    }

//////////////////////////////////////////////////////////////////////
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        //crea un conexion de salida segura para el dispositivo
        //usando el servicio UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //Consigue la direccion MAC desde DeviceListActivity via intent
        Intent intent = getIntent();
        //Consigue la direccion MAC desde DeviceListActivity via EXTRA
        address = intent.getStringExtra(DispositivosBT.EXTRA_DEVICE_ADDRESS);
        //Setea la direccion MAC
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try
        {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        // Establece la conexión con el socket Bluetooth.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {}
        }
        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        { // Cuando se sale de la aplicación esta parte permite
            // que no se deje abierto el socket
            btSocket.close();
        } catch (IOException e2) {}
    }

    //Comprueba que el dispositivo Bluetooth Bluetooth está disponible y solicita que se active si está desactivado
    private void VerificarEstadoBT() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
                estado = 1;
            } else {
                estado = 0;
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
  //              Toast.makeText(getBaseContext(), "Conectado", Toast.LENGTH_LONG).show();

            }
        }
    }

    //Crea la clase que permite crear el evento de conexion
    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;

            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
    }
}

