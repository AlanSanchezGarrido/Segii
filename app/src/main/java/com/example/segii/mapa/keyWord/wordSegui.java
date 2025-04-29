package com.example.segii.mapa.keyWord;

// Importaciones necesarias para manejar permisos, contexto, logs y la biblioteca de detección de palabras clave (Porcupine)
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineManager;
import ai.picovoice.porcupine.PorcupineManagerCallback;

// Clase que maneja la detección de una palabra clave ("Okey Segui") usando la biblioteca Porcupine
public class wordSegui {
    // Variable para el administrador de Porcupine, que detecta la palabra clave
    private PorcupineManager porcupineManager;
    // Bandera que indica si está escuchando la palabra clave
    private boolean isListening = false;
    // Contexto de la aplicación
    private final Context context;
    // Código constante para la solicitud de permiso de audio
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 3;
    // Etiqueta para logs
    private static final String TAG = "wordSegui";

    // Constructor que recibe el contexto de la aplicación
    public wordSegui(Context context) {
        this.context = context; // Asigna el contexto recibido
    }

    // Interfaz para notificar cuando se detecta la palabra clave
    public interface OnHotwordDetectedListener {
        void onDetected(); // Método que se llama al detectar la palabra clave
    }

    // Método para inicializar y comenzar a escuchar la palabra clave
    public void initializeAndStartListening(String accessKey, OnHotwordDetectedListener listener) {
        // Verifica si ya está escuchando
        if (isListening) {
            Log.d(TAG, "Ya está escuchando, ignorando solicitud");
            Toast.makeText(context, "Ya está escuchando", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verifica si se tiene permiso de grabación de audio
        if (!checkAudioPermission()) {
            Log.d(TAG, "Permiso de audio no otorgado, solicitando...");
            requestAudioPermission(); // Solicita permiso si no está otorgado
            return;
        }

        try {
            // Registra en el log el inicio de la inicialización de Porcupine
            Log.d(TAG, "Inicializando PorcupineManager con AccessKey: " + accessKey.substring(0, 5) + "...");
            // Configura y construye el PorcupineManager
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey(accessKey) // Establece la clave de acceso de Picovoice
                    .setKeywordPath("okey-segui_es_android_v3_0_0.ppn") // Archivo de la palabra clave en español
                    .setModelPath("porcupine_params_es.pv") // Modelo de lenguaje en español (comentar si no se usa)
                    .setSensitivity(0.7f) // Sensibilidad para la detección (0.0 a 1.0)
                    .build(context, new PorcupineManagerCallback() {
                        // Callback que se ejecuta cuando se detecta la palabra clave
                        @Override
                        public void invoke(int keywordIndex) {
                            Log.d(TAG, "Hotword 'Okey Segui' detectado, índice: " + keywordIndex);
                            Toast.makeText(context, "¡Hotword 'Okey Segui' detectado!", Toast.LENGTH_SHORT).show();
                            listener.onDetected(); // Notifica al listener que se detectó la palabra
                        }
                    });

            // Inicia la escucha de la palabra clave
            Log.d(TAG, "Iniciando escucha de Porcupine...");
            porcupineManager.start();
            isListening = true; // Actualiza la bandera de estado
            Toast.makeText(context, "Escuchando 'Okey Segui'...", Toast.LENGTH_SHORT).show();
        } catch (PorcupineException e) {
            // Maneja errores específicos de Porcupine
            String errorMessage = "Error al iniciar Porcupine: " + e.getMessage();
            Log.e(TAG, errorMessage, e);
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
            isListening = false; // Actualiza la bandera de estado
        } catch (Exception e) {
            // Maneja errores inesperados
            String errorMessage = "Error inesperado al iniciar Porcupine: " + e.getMessage();
            Log.e(TAG, errorMessage, e);
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
            isListening = false; // Actualiza la bandera de estado
        }
    }

    // Método para detener la escucha de la palabra clave
    public void stopListening() {
        // Verifica si no está escuchando o si el administrador es nulo
        if (!isListening || porcupineManager == null) {
            Log.d(TAG, "No se está escuchando o PorcupineManager es null, ignorando stop");
            return;
        }

        try {
            // Detiene y libera los recursos de Porcupine
            Log.d(TAG, "Deteniendo PorcupineManager...");
            porcupineManager.stop(); // Detiene la escucha
            porcupineManager.delete(); // Libera el administrador
            porcupineManager = null; // Establece el administrador a nulo
            isListening = false; // Actualiza la bandera de estado
            Toast.makeText(context, "Escucha detenida", Toast.LENGTH_SHORT).show();
        } catch (PorcupineException e) {
            // Maneja errores al detener Porcupine
            Log.e(TAG, "Error al detener Porcupine: " + e.getMessage(), e);
            Toast.makeText(context, "Error al detener hotword: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Verifica si se tiene permiso de grabación de audio
    private boolean checkAudioPermission() {
        boolean granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "Permiso RECORD_AUDIO: " + (granted ? "Otorgado" : "No otorgado"));
        return granted;
    }

    // Solicita permiso de grabación de audio
    private void requestAudioPermission() {
        // Verifica si el contexto es una actividad
        if (context instanceof Activity) {
            Log.d(TAG, "Solicitando permiso RECORD_AUDIO...");
            // Solicita el permiso de grabación de audio
            ActivityCompat.requestPermissions(
                    (Activity) context,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_PERMISSION_REQUEST_CODE
            );
        } else {
            // Registra un error si el contexto no es una actividad
            Log.e(TAG, "Contexto no es Activity, no se pueden solicitar permisos");
            Toast.makeText(context, "No se pueden solicitar permisos", Toast.LENGTH_SHORT).show();
        }
    }

    // Devuelve el estado de la escucha
    public boolean isListening() {
        return isListening;
    }

    // Libera los recursos de Porcupine
    public void cleanup() {
        Log.d(TAG, "Limpiando recursos de Porcupine...");
        stopListening(); // Detiene la escucha y libera recursos
    }
}