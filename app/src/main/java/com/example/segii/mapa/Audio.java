package com.example.segii.mapa;


import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaPlayer;

import com.example.segii.R;
// Clase que maneja la reproducción de un archivo de audio
@SuppressLint("NotConstructor") // Suprime advertencia sobre el método que parece un constructor
public class Audio {
    // Variable para el reproductor de medios
    private MediaPlayer mediaPlayer;

    // Constructor que inicializa el reproductor con un archivo de audio
    public Audio(Context context) {
        // Crea un MediaPlayer con el archivo de audio "bienvenido" desde los recursos
        mediaPlayer = MediaPlayer.create(context, R.raw.bienvenido);
    }

    // Método para reproducir el archivo de audio
    public void reproducir() {
        // Verifica si el MediaPlayer no es nulo
        if (mediaPlayer != null) {
            // Inicia la reproducción del audio
            mediaPlayer.start();
        }
    }
}
