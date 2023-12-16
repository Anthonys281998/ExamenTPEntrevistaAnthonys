package com.example.examenentrevistas;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.examenentrevistas.Model.Entrevista;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String DATABASE_KEY = "entrevistas";
    private DatabaseReference databaseReference;
    private EditText editFecha;
    private EditText editDescripcion;
    private EditText editPeriodista;
    private TextView textViewDatosAlmacenados;
    private Button btnTomarFoto;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private String rutaFotoActual;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Habilitar la persistencia local (se debe hacer una vez)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Obtener la referencia de la base de datos
        databaseReference = FirebaseDatabase.getInstance().getReference(DATABASE_KEY);

        // Inicializar los elementos de la interfaz de usuario
        editFecha = findViewById(R.id.editFecha);
        editPeriodista = findViewById(R.id.editPeriodista);
        editDescripcion = findViewById(R.id.editDescripcion);
        textViewDatosAlmacenados = findViewById(R.id.textViewDatosAlmacenados);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);

        if (editFecha == null || editPeriodista == null || editDescripcion == null || textViewDatosAlmacenados == null || btnTomarFoto == null) {
            // Manejar la situación en la que alguna vista no se encuentre
            Toast.makeText(this, "Error al inicializar vistas", Toast.LENGTH_SHORT).show();
            finish(); // Puedes decidir cerrar la actividad en este caso
            return;
        }

        // Configurar el Listener al hacer clic en el campo de fecha
        editFecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDatePicker();
            }
        });

        // Configurar el Listener para guardar una nueva entrevista
        Button btnGuardar = findViewById(R.id.btnGuardar);
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                guardarNuevaEntrevista();
            }
        });

        // Configurar el Listener para tomar una foto
        btnTomarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tomarFoto();
            }
        });

        // Configurar el Listener para leer entrevistas
        leerEntrevistas();
    }

    private void tomarFoto() {
        // Verificar si la aplicación tiene el permiso de la cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // El permiso no ha sido concedido, solicitar permiso al usuario
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            // El permiso ya ha sido concedido, puedes proceder con la lógica para tomar la foto
            obtenerUriDeLaImagen();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permiso de cámara concedido, puedes proceder con la lógica
                    obtenerUriDeLaImagen();
                } else {
                    // Permiso de cámara denegado, toma medidas apropiadas (por ejemplo, muestra un mensaje)
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                }
                break;
            // Agrega más casos aquí si necesitas manejar otros permisos
        }
    }

    private void obtenerUriDeLaImagen() {
        Intent tomarFotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (tomarFotoIntent.resolveActivity(getPackageManager()) != null) {
            File fotoArchivo = null;
            try {
                fotoArchivo = crearArchivoDeImagen();
            } catch (IOException ex) {
                Log.e("MainActivity", "Error al crear el archivo de imagen", ex);
            }
            if (fotoArchivo != null) {
                Uri fotoUri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", fotoArchivo);
                tomarFotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
                startActivityForResult(tomarFotoIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File crearArchivoDeImagen() throws IOException {
        // Crea un nombre de archivo único basado en la fecha y hora actual
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String nombreArchivo = "JPEG_" + timeStamp + "_";
        File directorioAlmacenamiento = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File archivoImagen = File.createTempFile(nombreArchivo, ".jpg", directorioAlmacenamiento);
        rutaFotoActual = archivoImagen.getAbsolutePath();
        return archivoImagen;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // La foto se tomó con éxito, ahora puedes subirla a Firebase Storage
            subirImagenAFirebaseStorage();
        }
    }

    private void subirImagenAFirebaseStorage() {
        if (rutaFotoActual != null) {
            Uri fotoUri = Uri.fromFile(new File(rutaFotoActual));

            // Obtener la referencia al nodo en Firebase Storage donde se guardará la imagen
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("imagenes");

            // Obtener un nombre único para la imagen basado en la fecha y hora actual
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String nombreImagen = "JPEG_" + timeStamp + ".jpg";

            // Crear la referencia para la nueva imagen
            StorageReference imagenRef = storageRef.child(nombreImagen);

            // Subir la imagen al nodo correspondiente
            imagenRef.putFile(fotoUri)
                    .addOnCompleteListener(this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                // La imagen se ha subido exitosamente, ahora obtenemos la URL de descarga
                                obtenerUrlDeDescarga(task.getResult().getTask());
                            } else {
                                // Manejar el fallo en la subida de la imagen
                                Toast.makeText(MainActivity.this, "Error al subir la imagen", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void obtenerUrlDeDescarga(StorageTask<UploadTask.TaskSnapshot> task) {
        if (task != null && task.isSuccessful()) {
            Task<Uri> urlTask = task.getResult().getStorage().getDownloadUrl();
            urlTask.addOnCompleteListener(this, new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> uriTask) {
                    if (uriTask.isSuccessful()) {
                        // Aquí puedes obtener la URL de descarga
                        String imageUrl = uriTask.getResult().toString();
                        // Ahora puedes usar imageUrl como necesites (por ejemplo, guardarlo en Firebase Database)
                        almacenarUrlEnBaseDeDatos(imageUrl);
                    } else {
                        // Manejar el caso en el que no se pueda obtener la URL de descarga
                        Toast.makeText(MainActivity.this, "Error al obtener la URL de descarga", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }


    private void almacenarUrlEnBaseDeDatos(String urlDeDescarga) {
        // ... (lógica para almacenar la URL en tu base de datos)

        // Obtén la URL de descarga de la imagen y otros detalles
        String descripcion = editDescripcion.getText().toString();
        String periodista = editPeriodista.getText().toString();
        Date fecha = obtenerFechaSeleccionada();
        String imagenUrl = "URL_de_la_imagen";
        String audioUrl = "URL_del_audio";  // Puedes manejar esto de manera similar a la URL de la imagen

        // Crea un objeto Entrevista con los detalles
        Entrevista nuevaEntrevista = new Entrevista(descripcion, periodista, fecha, urlDeDescarga, audioUrl);

        // Guarda la nueva entrevista en la base de datos
        DatabaseReference nuevaEntrevistaRef = databaseReference.push();
        nuevaEntrevistaRef.setValue(nuevaEntrevista);

        // Limpia los campos después de guardar la entrevista
        limpiarCampos();
    }




    private void mostrarDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDayOfMonth) {
                        // Formatear la fecha seleccionada y establecer el texto en el EditText
                        String formattedDate = selectedDayOfMonth + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        editFecha.setText(formattedDate);
                    }
                },
                year,
                month,
                day
        );

        datePickerDialog.show();
    }

    private void guardarNuevaEntrevista() {
        String descripcion = editDescripcion.getText().toString();
        String periodista = editPeriodista.getText().toString();
        Date fecha = obtenerFechaSeleccionada();
        String imagenUrl = "URL_de_la_imagen";
        String audioUrl = "URL_del_audio";

        if (fecha != null) {
            Entrevista nuevaEntrevista = new Entrevista(descripcion, periodista, fecha, imagenUrl, audioUrl);
            DatabaseReference nuevaEntrevistaRef = databaseReference.push();
            nuevaEntrevistaRef.setValue(nuevaEntrevista);

            limpiarCampos();
        }
    }

    private void limpiarCampos() {
        editPeriodista.setText("");
        editDescripcion.setText("");
        editFecha.setText("");
    }

    private void leerEntrevistas() {
        // Leer los datos de Firebase y actualizar la interfaz
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                StringBuilder datos = new StringBuilder();
                for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Entrevista entrevista = snapshot.getValue(Entrevista.class);
                    if (entrevista != null) {
                        datos.append("Descripción: ").append(entrevista.getDescripcion()).append("\n");
                        datos.append("Periodista: ").append(entrevista.getPeriodista()).append("\n");
                        datos.append("Fecha: ").append(formatoFecha(entrevista.getFecha())).append("\n\n");
                    }
                }
                textViewDatosAlmacenados.setText(datos.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MainActivity", "Error al leer datos", databaseError.toException());
            }
        });
    }

    private Date obtenerFechaSeleccionada() {
        return new Date(); // Implementa la lógica para obtener la fecha seleccionada
    }

    private String formatoFecha(Date fecha) {
        // Formatear la fecha como desees, por ejemplo:
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(fecha);
    }

    

}
