package com.android.correntometrobt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class ActivityLoad extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);      //DESACTIVAR CAMBIO DE ORIENTACIÓN
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        final String filename = "correntometro.txt";
        TextView IdBuffer;
        IdBuffer = findViewById(R.id.Idbufferload);
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
                Toast.makeText(this, "listo", Toast.LENGTH_LONG).show();
                IdBuffer.setText(todo);
            } catch (IOException e) {
            }

        else {
            Toast.makeText(this, "Archivo no existe", Toast.LENGTH_LONG).show();
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

    ///////////////////////////////////////////////////
    public void Volver (View v) {
        onBackPressed();        //Ir a la ventana anterior
    }
}

