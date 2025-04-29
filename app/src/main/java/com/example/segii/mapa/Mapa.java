package com.example.segii.mapa;

// Importaciones necesarias para manejar mapas, permisos, conectividad, interfaz y más
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.segii.R;
import com.example.segii.mapa.keyWord.wordSegui;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

// Clase principal de la actividad que muestra un mapa y maneja comandos de voz para navegación
public class Mapa extends AppCompatActivity implements OnMapReadyCallback {
    // Constantes para los códigos de solicitud de permisos
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 3;
    // Clave de acceso para el servicio de detección de palabras clave (hotword)
    private static final String PICOVOICE_ACCESS_KEY = "9JCv0f6pgDQ7+JX3QVWR3ZgsyygHaIHHQnl00sfYx5KUvyaW5CSs9A=="; // Verifica que sea válido
    // Etiqueta para logs
    private static final String TAG = "Mapa";

    // Variables para manejar mapa, ubicación, comandos de voz, rutas y audio
    private mapaMan mapaManager; // Administra el mapa
    private Location locationService; // Maneja la obtención de la ubicación
    private CommandVoice voiceCommandHandler; // Procesa comandos de voz
    private Route routeCalculator; // Calcula rutas
    private Audio audio; // Reproduce audio
    private wordSegui hotwordDetector; // Detecta palabras clave para activar comandos de voz

    // Método que se ejecuta al crear la actividad
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Establece el layout de la actividad
        setContentView(R.layout.activity_mapa);
        // Inicializa y reproduce un audio
        audio = new Audio(this);
        audio.reproducir();

        // Inicializa los servicios
        mapaManager = new mapaMan(this); // Inicializa el administrador del mapa
        locationService = new Location(this); // Inicializa el servicio de ubicación
        voiceCommandHandler = new CommandVoice(this); // Inicializa el manejador de comandos de voz
        // Inicializa el calculador de rutas con un callback para manejar resultados
        routeCalculator = new Route(new Route.RouteCallback() {
            // Cuando la ruta se calcula correctamente
            @Override
            public void onRouteCalculated(List<LatLng> points, LatLng destination, String destinationName) {
                // Dibuja la ruta en el mapa desde la ubicación actual al destino
                mapaManager.drawRoute(points, locationService.getCurrentLocation(), destination, destinationName);
            }

            // Cuando falla el cálculo de la ruta
            @Override
            public void onRouteFailed(String errorMessage) {
                // Muestra un mensaje de error
                Toast.makeText(Mapa.this, errorMessage, Toast.LENGTH_LONG).show();
            }

            // Cuando se encuentran múltiples destinos posibles
            @Override
            public void onMultipleDestinationsFound(List<Route.DestinationOption> options) {
                // Convierte las opciones de destino a un arreglo de nombres
                String[] destinationNames = options.stream().map(opt -> opt.name).toArray(String[]::new);
                // Muestra un diálogo para que el usuario seleccione un destino
                new AlertDialog.Builder(Mapa.this)
                        .setTitle("Múltiples destinos encontrados")
                        .setItems(destinationNames, (dialog, which) -> {
                            Route.DestinationOption selected = options.get(which);
                            // Obtiene la ubicación actual del dispositivo
                            locationService.getDeviceLocation(new Location.LocationCallback() {
                                @Override
                                public void onLocationReceived(LatLng location) {
                                    // Verifica si hay conexión a internet
                                    if (isNetworkAvailable()) {
                                        // Calcula la ruta al destino seleccionado
                                        routeCalculator.calculateRoute(location, selected.name);
                                    } else {
                                        // Muestra un mensaje si no hay conexión
                                        Toast.makeText(Mapa.this, "Sin conexión a internet", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onLocationFailed() {
                                    // Muestra un mensaje si no se pudo obtener la ubicación
                                    Toast.makeText(Mapa.this, "No se pudo obtener tu ubicación", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });

        // Inicializa el detector de palabras clave
        hotwordDetector = new wordSegui(this);
        // Configura el detector para iniciar el reconocimiento de voz al detectar la palabra clave
        hotwordDetector.initializeAndStartListening(PICOVOICE_ACCESS_KEY, () -> {
            Log.d(TAG, "Hotword detectado, iniciando reconocimiento de voz...");
            voiceCommandHandler.startVoiceRecognition();
        });

        // Obtiene el fragmento del mapa y lo configura para cargar asíncronamente
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Configura el botón flotante para centrar el mapa en la ubicación actual
        FloatingActionButton fab = findViewById(R.id.fab_center_location);
        fab.setOnClickListener(v -> {
            // Verifica si se tiene permiso de ubicación
            if (checkLocationPermission()) {
                // Obtiene la ubicación actual
                locationService.getDeviceLocation(new Location.LocationCallback() {
                    @Override
                    public void onLocationReceived(LatLng location) {
                        // Centra el mapa en la ubicación
                        mapaManager.centerOnLocation(location, true);
                    }

                    @Override
                    public void onLocationFailed() {
                        // Muestra un mensaje si no se pudo obtener la ubicación
                        Toast.makeText(Mapa.this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Solicita permiso de ubicación si no está otorgado
                requestLocationPermission();
            }
        });

        // Configura el botón flotante para comandos de voz
        FloatingActionButton fabVoice = findViewById(R.id.fab_voice_command);
        fabVoice.setOnClickListener(view -> {
            Log.d(TAG, "Botón de voz presionado, isListening: " + hotwordDetector.isListening());
            // Verifica si se tiene permiso de audio
            if (checkAudioPermission()) {
                // Alterna el estado del detector de palabras clave
                if (hotwordDetector.isListening()) {
                    hotwordDetector.stopListening(); // Detiene la escucha
                } else {
                    // Inicia la escucha de la palabra clave
                    hotwordDetector.initializeAndStartListening(PICOVOICE_ACCESS_KEY, () -> {
                        Log.d(TAG, "Hotword detectado, iniciando reconocimiento de voz...");
                        voiceCommandHandler.startVoiceRecognition();
                    });
                }
            } else {
                Log.d(TAG, "Solicitando permiso de audio...");
                // Solicita permiso de audio si no está otorgado
                requestAudioPermission();
            }
        });
    }

    // Método que se ejecuta cuando el mapa está listo
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // Inicializa el mapa con el objeto GoogleMap
        mapaManager.initializeMap(googleMap);

        // Verifica si se tiene permiso de ubicación
        if (checkLocationPermission()) {
            // Habilita la capa de "Mi ubicación" en el mapa
            mapaManager.enableMyLocation();
            // Obtiene la ubicación actual
            locationService.getDeviceLocation(new Location.LocationCallback() {
                @Override
                public void onLocationReceived(LatLng location) {
                    // Centra el mapa en la ubicación
                    mapaManager.centerOnLocation(location, true);
                }

                @Override
                public void onLocationFailed() {
                    // Muestra un mensaje si no se pudo obtener la ubicación
                    Toast.makeText(Mapa.this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Solicita permiso de ubicación si no está otorgado
            requestLocationPermission();
        }
    }

    // Verifica si se tiene permiso de ubicación
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Verifica si se tiene permiso de grabación de audio
    private boolean checkAudioPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Solicita permiso de ubicación
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    // Solicita permiso de grabación de audio
    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.RECORD_AUDIO},
                AUDIO_PERMISSION_REQUEST_CODE);
    }

    // Maneja los resultados de las solicitudes de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Si se otorgó el permiso de ubicación
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Habilita la capa de "Mi ubicación" en el mapa
                mapaManager.enableMyLocation();
                // Obtiene la ubicación actual
                locationService.getDeviceLocation(new Location.LocationCallback() {
                    @Override
                    public void onLocationReceived(LatLng location) {
                        // Centra el mapa en la ubicación
                        mapaManager.centerOnLocation(location, true);
                    }

                    @Override
                    public void onLocationFailed() {
                        // Muestra un mensaje si no se pudo obtener la ubicación
                        Toast.makeText(Mapa.this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Muestra un mensaje si se denegó el permiso
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            // Si se otorgó el permiso de audio
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permiso de audio otorgado, iniciando hotwordDetector...");
                // Inicia la escucha de la palabra clave
                hotwordDetector.initializeAndStartListening(PICOVOICE_ACCESS_KEY, () -> {
                    Log.d(TAG, "Hotword detectado, iniciando reconocimiento de voz...");
                    voiceCommandHandler.startVoiceRecognition();
                });
            } else {
                Log.d(TAG, "Permiso de audio denegado");
                // Muestra un mensaje si se denegó el permiso
                Toast.makeText(this, "Permiso de audio denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Procesa los resultados de actividades (como el reconocimiento de voz)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Procesa el resultado del reconocimiento de voz
        voiceCommandHandler.processVoiceResult(requestCode, resultCode, data, destination -> {
            // Verifica si se tiene permiso de ubicación
            if (checkLocationPermission()) {
                // Obtiene la ubicación actual
                locationService.getDeviceLocation(new Location.LocationCallback() {
                    @Override
                    public void onLocationReceived(LatLng location) {
                        Log.d("Mapa", "Ubicación actual antes de calcular ruta: (" + location.latitude + ", " + location.longitude + ")");
                        // Verifica si hay conexión a internet
                        if (isNetworkAvailable()) {
                            // Calcula la ruta al destino especificado
                            routeCalculator.calculateRoute(location, destination);
                        } else {
                            // Muestra un mensaje si no hay conexión
                            Toast.makeText(Mapa.this, "Sin conexión a internet", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onLocationFailed() {
                        // Muestra un mensaje si no se pudo obtener la ubicación
                        Toast.makeText(Mapa.this, "No se pudo obtener tu ubicación", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Muestra un mensaje y solicita permiso de ubicación
                Toast.makeText(this, "Primero obtén tu ubicación", Toast.LENGTH_SHORT).show();
                requestLocationPermission();
            }
        });
    }

    // Verifica si hay conexión a internet
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Método que se ejecuta al destruir la actividad
    @Override
    protected void onDestroy() {super.onDestroy();
        Log.d(TAG, "Limpiando recursos en onDestroy...");
        // Libera los recursos del detector de palabras clave
        hotwordDetector.cleanup();
    }
}