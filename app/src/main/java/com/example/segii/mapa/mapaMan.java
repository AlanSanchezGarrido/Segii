package com.example.segii.mapa;

// Importaciones necesarias para manejar mapas de Google, contexto y UI
import com.google.android.gms.maps.GoogleMap;
import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import java.util.List;

// Clase que administra el mapa de Google Maps y sus funcionalidades
public class mapaMan implements GoogleMap.OnMapClickListener {
    // Objeto que representa el mapa de Google
    private GoogleMap mMap;
    // Contexto de la aplicación
    private final Context context;
    // Límites geográficos de Huauchinango
    private final LatLngBounds huauchinangoBounds;

    // Constructor que inicializa el contexto y los límites de Huauchinango
    public mapaMan(Context context) {
        this.context = context; // Asigna el contexto recibido
        // Define los límites geográficos de Huauchinango
        this.huauchinangoBounds = new LatLngBounds(
                new LatLng(20.1, -98.1), // Esquina inferior izquierda
                new LatLng(20.25, -97.95) // Esquina superior derecha
        );
    }

    // Método para inicializar el mapa con configuraciones iniciales
    @SuppressLint("MissingPermission") // Suprime advertencia sobre permisos (se asume que se verifican previamente)
    public void initializeMap(@NonNull GoogleMap googleMap) {
        mMap = googleMap; // Asigna el objeto del mapa
        // Habilita los controles de zoom en la interfaz
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // Deshabilita el botón de "Mi ubicación" (se controla manualmente)
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        // Habilita la brújula en el mapa
        mMap.getUiSettings().setCompassEnabled(true);

        // Centra la cámara en los límites de Huauchinango con un margen de 100 píxeles
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(huauchinangoBounds, 100));
        // Restringe los movimientos de la cámara a los límites de Huauchinango
        mMap.setLatLngBoundsForCameraTarget(huauchinangoBounds);

        // Establece esta clase como listener para clics en el mapa
        mMap.setOnMapClickListener(this);
    }

    // Método para habilitar la capa de "Mi ubicación" en el mapa
    @SuppressLint("MissingPermission") // Suprime advertencia sobre permisos
    public void enableMyLocation() {
        if (mMap != null) {
            // Habilita la visualización de la ubicación del usuario en el mapa
            mMap.setMyLocationEnabled(true);
        }
    }

    // Método para centrar el mapa en una ubicación específica
    public void centerOnLocation(LatLng location, boolean isUserLocation) {
        if (mMap == null) return; // Sale si el mapa no está inicializado

        // Verifica si la ubicación está dentro de los límites de Huauchinango
        if (huauchinangoBounds.contains(location)) {
            // Centra la cámara en la ubicación con un nivel de zoom de 15
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
            // Agrega un marcador en la ubicación
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(isUserLocation ? "¡Estás aquí!" : "Destino"));
        } else {
            // Si está fuera de los límites, centra en el centro de Huauchinango
            LatLng huauchinangoCenter = new LatLng(20.1738, -98.0549);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(huauchinangoCenter, 15f));
            // Muestra un mensaje si es la ubicación del usuario
            if (isUserLocation) {
                        Toast.makeText(context, "Estás fuera de Huauchinango. Mostrando el centro de la ciudad.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Método para dibujar una ruta en el mapa
    public void drawRoute(List<LatLng> points, LatLng origin, LatLng destination, String destinationName) {
        if (mMap == null || points == null || points.isEmpty()) return; // Sale si el mapa o la lista de puntos no es válida

        // Limpia los marcadores y rutas previas del mapa
        mMap.clear();
        // Agrega un marcador en el punto de origen
        mMap.addMarker(new MarkerOptions().position(origin).title("Origen"));
        // Agrega un marcador en el destino
        mMap.addMarker(new MarkerOptions().position(destination).title(destinationName));
        // Dibuja una línea (ruta) con los puntos proporcionados
        mMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .color(0xFF2196F3) // Color azul
                .width(10)); // Grosor de la línea

        // Crea un constructor para los límites de la cámara
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(origin); // Incluye el punto de origen
        for (LatLng point : points) {
            builder.include(point); // Incluye cada punto de la ruta
        }
        LatLngBounds bounds = builder.build(); // Construye los límites

        // Anima la cámara para mostrar toda la ruta con un margen de 100 píxeles
        mMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, 100),
                2000, // Duración de la animación en milisegundos
                null
        );
    }

    // Método que se ejecuta cuando el usuario toca el mapa
    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        // Puedes implementar lógica si el usuario toca el mapa
        // Actualmente no realiza ninguna acción
    }
}