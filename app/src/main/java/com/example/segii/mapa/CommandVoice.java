package com.example.segii.mapa;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

// Clase que maneja el reconocimiento de voz para comandos de navegación
public class CommandVoice {
    // Variable para almacenar la actividad que invoca esta clase
    private final Activity activity;
    // Código constante para identificar la solicitud de reconocimiento de voz
    private static final int VOICE_REQUEST_CODE = 5;

    // Constructor que recibe la actividad actual
    public CommandVoice(Activity activity) {
        this.activity = activity; // Asigna la actividad recibida
    }

    // Método para iniciar el reconocimiento de voz
    public void startVoiceRecognition() {
        // Crea un Intent para la acción de reconocimiento de voz
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // Configura el modelo de lenguaje libre para el reconocimiento
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Establece el idioma predeterminado del dispositivo
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        // Muestra un mensaje en la interfaz para guiar al usuario
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Di 'Navega a [destino]' (por ejemplo, Plaza Principal en Huauchinango)");
        try {
            // Inicia la actividad de reconocimiento de voz y espera un resultado
            activity.startActivityForResult(intent, VOICE_REQUEST_CODE);
        } catch (Exception e) {
            // Muestra un mensaje si el reconocimiento de voz no está disponible
            Toast.makeText(activity, "Reconocimiento de voz no disponible", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para procesar el resultado del reconocimiento de voz
    public void processVoiceResult(int requestCode, int resultCode, Intent data, VoiceCommandCallback callback) {
        // Verifica si el código de solicitud y el resultado son válidos, y si hay datos
        if (requestCode == VOICE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            // Obtiene la lista de resultados del reconocimiento de voz
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            // Verifica si hay resultados válidos
            if (results != null && !results.isEmpty()) {
                // Toma el primer resultado y lo convierte a minúsculas
                String command = results.get(0).toLowerCase();
                // Registra el comando reconocido en el log
                Log.d("VoiceCommand", "Comando reconocido: " + command);
                // Verifica si el comando comienza con "navega a "
                if (command.startsWith("navega a ")) {
                    // Extrae el destino eliminando "navega a " y espacios sobrantes
                    String destination = command.replace("navega a ", "").trim();
                    // Limpia el destino eliminando caracteres no alfanuméricos
                    destination = destination.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
                    // Registra el destino extraído en el log
                    Log.d("VoiceCommand", "Destino extraído: " + destination);

                    // Verifica si el destino no está vacío
                    if (!destination.isEmpty()) {
                        // Extrae la primera palabra significativa del destino
                        String firstWord = destination.split("\\s+")[0];
                        // Registra la primera palabra en el log
                        Log.d("VoiceCommand", "Primer palabra: " + firstWord);
                        // Llama al callback con el destino procesado
                        callback.onDestinationReceived(firstWord + " " + destination);
                    } else {
                        // Muestra un mensaje si el destino no es válido
                        Toast.makeText(activity, "Destino no válido", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Muestra un mensaje si el comando no sigue el formato esperado
                    Toast.makeText(activity, "Di 'Navega a [destino]'", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Interfaz para manejar el callback cuando se recibe un destino válido
    public interface VoiceCommandCallback {
        // Método que se llama cuando se procesa un destino
        void onDestinationReceived(String destination);
    }
}