package com.example.segii.mapa;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
// Clase que calcula rutas entre un origen y un destino usando las APIs de Google
public class Route {
    // Callback para notificar los resultados del cálculo de la ruta
    private final RouteCallback callback;
    // Interfaz para manejar los resultados del cálculo de rutas
    public interface RouteCallback {
        // Llamado cuando la ruta se calcula correctamente
        void onRouteCalculated(List<LatLng> points, LatLng destination, String destinationName);
        // Llamado cuando falla el cálculo de la ruta
        void onRouteFailed(String errorMessage);
        // Llamado cuando se encuentran múltiples destinos posibles
        void onMultipleDestinationsFound(List<DestinationOption> options);
    }
    // Clase interna para representar opciones de destinos
    public static class DestinationOption {
        public String name; // Nombre del destino
        public LatLng latLng; // Coordenadas del destino

        public DestinationOption(String name, LatLng latLng) {
            this.name = name;
            this.latLng = latLng;
        }
    }
    // Constructor que recibe el callback
    public Route(RouteCallback callback) {
        this.callback = callback; // Asigna el callback recibido
    }
    // Método para calcular una ruta desde un origen a un destino
    public void calculateRoute(LatLng origin, String destinationName) {
        // Crea un executor para tareas en segundo plano
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // Crea un handler para ejecutar en el hilo principal
        Handler handler = new Handler(Looper.getMainLooper());

        // Ejecuta la tarea en segundo plano
        executor.execute(() -> {
            List<LatLng> points = null; // Lista de puntos de la ruta
            String errorMessage = null; // Mensaje de error
            LatLng destinationLatLng = null; // Coordenadas del destino
            String finalDestinationName = destinationName; // Nombre final del destino

            try {
                // Registra las coordenadas del origen
                Log.d("RouteCalculator", "Origen: (" + origin.latitude + ", " + origin.longitude + ")");

                // Paso 1: Geocodificación del destino
                // Codifica el nombre del destino con la localidad
                String encodedDestination = URLEncoder.encode(destinationName + ", Huauchinango, Puebla", StandardCharsets.UTF_8.toString());
                // Construye la URL para la API de Geocoding
                String geocodingUrl = "https://maps.googleapis.com/maps/api/geocode/json?" +
                        "address=" + encodedDestination +
                        "&region=mx" +
                        "&key=AIzaSyDJ2FEO8IFEzc0LAsEiYCR5cqGR7kzAzwk";

                Log.d("GeocodingAPI", "URL: " + geocodingUrl);
                OkHttpClient client = new OkHttpClient();
                // Crea y ejecuta la solicitud HTTP
                Request geocodingRequest = new Request.Builder().url(geocodingUrl).build();
                Response geocodingResponse = client.newCall(geocodingRequest).execute();
                // Verifica si la respuesta es exitosa
                if (!geocodingResponse.isSuccessful()) {
                    errorMessage = "Error en Geocoding: " + geocodingResponse.code() + " " + geocodingResponse.message();
                    Log.e("GeocodingAPI", errorMessage);
                } else {
                    // Procesa la respuesta JSON
                    String geocodingJson = geocodingResponse.body().string();
                    Log.d("GeocodingAPI", "Respuesta JSON: " + geocodingJson);
                    JSONObject geocodingResult = new JSONObject(geocodingJson);

                    String geocodingStatus = geocodingResult.getString("status");
                    // Verifica el estado de la respuesta
                    if (!geocodingStatus.equals("OK")) {
                        errorMessage = "Error en Geocoding API: " + geocodingStatus;
                        if (geocodingResult.has("error_message")) {
                            errorMessage += " - " + geocodingResult.getString("error_message");
                        }
                    } else {
                        JSONArray results = geocodingResult.getJSONArray("results");
                        // Verifica si no se encontraron resultados
                        if (results.length() == 0) {
                            errorMessage = "No se encontró el destino: " + destinationName;
                            Log.e("GeocodingAPI", errorMessage);
                        } else if (results.length() > 1) {
                            // Múltiples destinos encontrados
                            List<DestinationOption> options = new ArrayList<>();
                            // Define límites de Huauchinango
                            LatLngBounds huauchinangoBounds = new LatLngBounds(
                                    new LatLng(20.1, -98.1),
                                    new LatLng(20.25, -97.95)
                            );
                            // Filtra los resultados dentro de los límites
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject result = results.getJSONObject(i);
                                JSONObject location = result.getJSONObject("geometry").getJSONObject("location");
                                double destLat = location.getDouble("lat");
                                double destLng = location.getDouble("lng");
                                LatLng latLng = new LatLng(destLat, destLng);

                                if (huauchinangoBounds.contains(latLng)) {
                                    String formattedAddress = result.getString("formatted_address");
                                    options.add(new DestinationOption(formattedAddress, latLng));
                                }
                            }

                            if (!options.isEmpty()) {
                                // Notifica múltiples opciones al callback
                                handler.post(() -> callback.onMultipleDestinationsFound(options));
                                return; // Sale para que el usuario elija
                            } else {
                                errorMessage = "Ningún destino válido encontrado en Huauchinango";
                                Log.e("GeocodingAPI", errorMessage);
                            }
                        } else {
                            // Un solo resultado encontrado
                            JSONObject location = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                            double destLat = location.getDouble("lat");
                            double destLng = location.getDouble("lng");
                            destinationLatLng = new LatLng(destLat, destLng);
                            finalDestinationName = results.getJSONObject(0).getString("formatted_address");
                            Log.d("GeocodingAPI", "Destino encontrado: (" + destLat + ", " + destLng + ")");
                            // Verifica si el destino está dentro de los límites de Huauchinango
                            LatLngBounds huauchinangoBounds = new LatLngBounds(
                                    new LatLng(20.1, -98.1),
                                    new LatLng(20.25, -97.95)
                            );
                            if (!huauchinangoBounds.contains(destinationLatLng)) {
                                errorMessage = "El destino está fuera de Huauchinango: " + destinationName;
                                Log.e("DirectionsAPI", errorMessage);
                            } else {
                                // Paso 2: API de Direcciones
                                // Construye la URL para la API de Directions
                                String directionsUrl = "https://maps.googleapis.com/maps/api/directions/json?" +
                                        "origin=" + origin.latitude + "," + origin.longitude +
                                        "&destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude +
                                        "&region=mx" +
                                        "&key=AIzaSyDJ2FEO8IFEzc0LAsEiYCR5cqGR7kzAzwk";

                                Log.d("DirectionsAPI", "URL: " + directionsUrl);
                                Request directionsRequest = new Request.Builder().url(directionsUrl).build();
                                Response directionsResponse = client.newCall(directionsRequest).execute();
                                // Verifica si la respuesta es exitosa
                                if (!directionsResponse.isSuccessful()) {
                                    errorMessage = "Error en la solicitud: " + directionsResponse.code() + " " + directionsResponse.message();
                                    Log.e("DirectionsAPI", errorMessage);
                                } else {
                                    String jsonData = directionsResponse.body().string();
                                    Log.d("DirectionsAPI", "Respuesta JSON: " + jsonData);
                                    JSONObject json = new JSONObject(jsonData);

                                    String status = json.getString("status");
                                    // Verifica el estado de la respuesta
                                    if (!status.equals("OK")) {
                                        errorMessage = "Error en la API: " + status;
                                        if (json.has("error_message")) {
                                            errorMessage += " - " + json.getString("error_message");
                                        }
                                        Log.e("DirectionsAPI", errorMessage);
                                    } else {
                                        // Procesa las rutas
                                        JSONArray routes = json.getJSONArray("routes");
                                        if (routes.length() > 0) {
                                            JSONArray legs = routes.getJSONObject(0).getJSONArray("legs");
                                            JSONArray steps = legs.getJSONObject(0).getJSONArray("steps");
                                            points = new ArrayList<>();
                                            // Decodifica los puntos de la ruta
                                            for (int i = 0; i < steps.length(); i++) {
                                                String polyline = steps.getJSONObject(i).getJSONObject("polyline").getString("points");
                                                points.addAll(decodePolyline(polyline));
                                            }
                                            Log.d("DirectionsAPI", "Ruta calculada con " + points.size() + " puntos");
                                        } else {
                                            errorMessage = "No se encontraron rutas para el destino: " + destinationName;
                                            Log.e("DirectionsAPI", errorMessage);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Maneja excepciones generales
                errorMessage = "Excepción: " + e.getMessage();
                Log.e("DirectionsAPI", errorMessage, e);
            }
            // Variables finales para usar en el hilo principal
            List<LatLng> finalPoints = points;
            String finalErrorMessage = errorMessage;
            LatLng finalDestinationLatLng = destinationLatLng;
            // Ejecuta en el hilo principal
            handler.post(() -> {
                if (finalPoints != null && !finalPoints.isEmpty()) {
                    // Notifica que la ruta se calculó correctamente
                    callback.onRouteCalculated(finalPoints, finalDestinationLatLng, destinationName);

                } else {
                    // Notifica el fallo con un mensaje personalizado
                    String message = finalErrorMessage != null ? finalErrorMessage : "No se pudo calcular la ruta";
                    if (finalErrorMessage != null && finalErrorMessage.contains("NOT_FOUND")) {
                        message = "No se encontró el destino: " + destinationName + ". Intenta con otro nombre.";
                    }
                    callback.onRouteFailed(message);
                }
            });
        });
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            // Decodifica la latitud
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            // Decodifica la longitud
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            // Crea un punto LatLng y lo agrega a la lista
            LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
            poly.add(p);
        }
        return poly;
    }
}