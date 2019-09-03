package com.example.dogwalk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker currentLocationMarker;
    private LatLng currentLocationLatLng;
    private boolean canGetLocation;

    LatLng pet = null;
    private DatabaseReference mDataBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        startGettingLocations();
        mDataBase = FirebaseDatabase.getInstance().getReference().child("localization");
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        long tempo = 1000; //5 minutos
        float minDistancia = 1; // 30 metros

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER , tempo , minDistancia,  new LocationListener() {

            @Override
            public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
                Toast.makeText(getApplicationContext(), "Status alterado", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProviderEnabled(String arg0) {
                Toast.makeText(getApplicationContext(), "Provider Habilitado", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProviderDisabled(String arg0) {
                Toast.makeText(getApplicationContext(), "Provider Desabilitado", Toast.LENGTH_LONG).show();
            }

            @Override
            //dispara quando a posição for alterada
            public void onLocationChanged(Location location) {

                if (currentLocationMarker != null) {
                    currentLocationMarker.remove();
                }

                //cria-se um objeto com a nova localização
                LocalizationPet loc = new LocalizationPet(location.getLatitude(), location.getLongitude());
                pet = new LatLng(loc.getLatitude(), loc.getLongitude());
                //cria-se um novo morcador para nova posição
                criaMarcador();

                //persiste a nova posição no firebase na base "localization"
                mDataBase.child("1").setValue(loc);

                CameraPosition cameraPosition = new CameraPosition.Builder().zoom(15).target(pet).build();
                currentLocationMarker = mMap.addMarker(new MarkerOptions().position(pet).title("Sua posição"));
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            }
        }, null );

    }


    private void criaMarcador() {
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        currentLocationLatLng = new LatLng(pet.latitude, pet.longitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLocationLatLng);
        markerOptions.title("Posição PET");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        currentLocationMarker = mMap.addMarker(markerOptions);
        CameraPosition cameraPosition = new CameraPosition.Builder().zoom(15).target(currentLocationLatLng).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private ArrayList findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList result = new ArrayList();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canAskPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canAskPermission() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("GPS desativado!");
        alertDialog.setMessage("Ativar GPS?");
        alertDialog.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        alertDialog.setNegativeButton("Não", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }


    private void startGettingLocations() {

        //gerenciador de localização
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //verifica se o GPS está ativo
        boolean isGPS = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        //verifica se a rede está ativa
        boolean isNetwork = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        int ALL_PERMISSIONS_RESULT = 101;

        ArrayList<String> permissions = new ArrayList<>();
        ArrayList<String> permissionsToRequest;

        //adiciona as permissões necessárias a serem solicitadas
        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionsToRequest = findUnAskedPermissions(permissions);

        //se nem o gps nem a rede estirem ativos, pergunta se deseja ativar
        if (!isGPS && !isNetwork) {
            showSettingsAlert();
        } else {
            //verifica as permissões para as últimas versões
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (permissionsToRequest.size() > 0) {
                    requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                            ALL_PERMISSIONS_RESULT);
                    canGetLocation = false;
                }
            }
        }


        //Checks if FINE LOCATION and COARSE Location were granted
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show();
            return;
        }
    }

}
