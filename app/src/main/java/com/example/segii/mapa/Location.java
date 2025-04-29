package com.example.segii.mapa;

// Importaciones necesarias para manejar la ubicación, contexto y mapas de Google
import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

// Clase que maneja la obtención y gestión de la ubicación del dispositivo
public class Location {
    // Cliente para obtener la ubicación del dispositivo
    private final FusedLocationProviderClient fusedLocationClient;
    // Contexto de la aplicación
    private final Context context;
    // Ubicación actual del dispositivo
    private LatLng currentLocation;
    // Límites geográficos de Huauchinango
    private final LatLngBounds huauchinangoBounds;
    // Centro geográfico de Huauchinango
    private final LatLng huauchinangoCenter;

    // Constructor que inicializa los valores por defecto
    public Location(Context context) {
        this.context = context; // Asigna el contexto recibido
        // Inicializa el cliente de ubicación
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        // Define los límites geográficos de Huauchinango
        this.huauchinangoBounds = new LatLngBounds(
                new LatLng(20.1, -98.1), // Esquina inferior izquierda
                new LatLng(20.25, -97.95) // Esquina superior derecha
        );
        // Define el centro de Huauchinango
        this.huauchinangoCenter = new LatLng(-20.1782935, -98.0719694);
        // Establece la ubicación inicial como el centro de Huauchinango
        this.currentLocation = huauchinangoCenter;
    }

    // Constructor alternativo para inyección de dependencias
    public Location(FusedLocationProviderClient fusedLocationClient, Context context, LatLngBounds huauchinangoBounds, LatLng huauchinangoCenter) {
        this.fusedLocationClient = fusedLocationClient; // Asigna el cliente de ubicación
        this.context = context; // Asigna el contexto
        this.huauchinangoBounds = huauchinangoBounds; // Asigna los límites
        this.huauchinangoCenter = huauchinangoCenter; // Asigna el centro
        // Establece la ubicación inicial como el centro de Huauchinango
        this.currentLocation = huauchinangoCenter;
    }

    // Método para obtener la ubicación actual del dispositivo
    @SuppressLint("MissingPermission") // Suprime advertencia sobre permisos (se asume que se verifican previamente)
    public void getDeviceLocation(LocationCallback callback) {
        // Obtiene la última ubicación conocida del dispositivo
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        // Actualiza la ubicación actual con las coordenadas obtenidas
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        // Verifica si la ubicación está dentro de los límites de Huauchinango
                        if (!huauchinangoBounds.contains(currentLocation)) {
                            // Muestra un mensaje si está fuera de los límites
                            Toast.makeText(context, "Estás fuera de Huauchinango. Calculando ruta desde tu ubicación actual.", Toast.LENGTH_LONG).show();
                        }
                        // Llama al callback con la ubicación obtenida
                        callback.onLocationReceived(currentLocation);
                    } else {
                        // Si no se obtiene la ubicación, usa el centro de Huauchinango
                        Toast.makeText(context, "No se pudo obtener la ubicación. Usando el centro de Huauchinango como origen.", Toast.LENGTH_LONG).show();
                        currentLocation = huauchinangoCenter;
                        callback.onLocationReceived(currentLocation);
                    }
                })
                .addOnFailureListener(e -> {
                    // En caso de error, usa el centro de Huauchinango
                    Toast.makeText(context, "Error al obtener la ubicación. Usando el centro de Huauchinango como origen.", Toast.LENGTH_LONG).show();
                    currentLocation = huauchinangoCenter;
                    callback.onLocationReceived(currentLocation);
                });
    }

    // Método para obtener la ubicación actual almacenada
    public LatLng getCurrentLocation() {
        return currentLocation;
    }

    // Interfaz para manejar los resultados de la obtención de ubicación
    public interface LocationCallback {
        void onLocationReceived(LatLng location); // Llamado cuando se obtiene la ubicación
        void onLocationFailed(); // Llamado cuando falla la obtención de la ubicación
    }
}