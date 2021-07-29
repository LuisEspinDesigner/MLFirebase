package com.example.mlfirebase;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.TextAnnotation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextDetection extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    Button btncarga, btnprocesa;
    Vision visio;
    ImageView imagen;
    String message;
    TextView Resultado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_detection);
        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(), new AndroidJsonFactory(), null);
        visionBuilder.setVisionRequestInitializer(new VisionRequestInitializer("AIzaSyB5MkIB5lNnQH1kC1tZ3ATeEsv7z66moKs"));
        visio = visionBuilder.build();
        btncarga = findViewById(R.id.btncarga);
        btnprocesa = findViewById(R.id.btnprocesa);
        imagen = (ImageView) findViewById(R.id.Imagen);
        Resultado = findViewById(R.id.editTextTextMultiLine);
        btncarga.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openArchivos();
            }
        });
        btnprocesa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ejecutarOpcion();
            }
        });
    }

    public void openArchivos() {
        final CharSequence[] opciones = {"Tomar Foto", "Buscar Imagen"};
        final AlertDialog.Builder alertOpciones = new AlertDialog.Builder(this);
        alertOpciones.setTitle("Seleccione...");
        alertOpciones.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (opciones[which].toString()) {
                    case "Tomar Foto":
                        tomarFoto();
                        break;
                    case "Buscar Imagen":
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/");
                        startActivityForResult(intent.createChooser(intent, "Seleccione la app"), 10);
                        break;
                }
            }
        });
        alertOpciones.show();
    }

    public void ejecutarOpcion() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                bitmap = scaleBitmapDown(bitmap, 1200);
                ByteArrayOutputStream stream = new ByteArrayOutputStream(); //2da de la api
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                byte[] imageInByte = stream.toByteArray();
                Image inputImage = new Image(); //googlevision
                inputImage.encodeContent(imageInByte);
                //Armo mi listado de solicitudes
                List<Feature> desiredFeaturelst = new ArrayList<>();
                //Realizar la solicitud de cualquier tipo de los servicio que ofrece la API
                Feature desiredFeatureitem= new Feature();
                desiredFeatureitem.setType("TEXT_DETECTION");
                //Cargo a mi lista la solicitud
                desiredFeaturelst.add(desiredFeatureitem);
                //Armamos la solicitud o las solicitudes .- FaceDetection solo o facedeteccion,textdetection,etc..
                AnnotateImageRequest request = new AnnotateImageRequest();
                request.setImage(inputImage);
                request.setFeatures(desiredFeaturelst);
                BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
                batchRequest.setRequests(Arrays.asList(request));
                //Asignamos al control VisionBuilder la solicitud
                BatchAnnotateImagesResponse batchResponse = null;
                try {
                    Vision.Images.Annotate annotateRequest =
                            visio.images().annotate(batchRequest);
                    //Enviamos la solicitud
                    annotateRequest.setDisableGZipContent(true);
                    batchResponse = annotateRequest.execute();
                } catch (IOException ex) {
                    Toast.makeText(TextDetection.this, ex.getMessage().toString(), Toast.LENGTH_SHORT).show();
                }
                if (batchResponse != null) {
                    TextAnnotation text = batchResponse.getResponses().get(0).getFullTextAnnotation();
                    if (text != null) {
                        message = text.getText();

                    } else {
                        Toast.makeText(TextDetection.this, "No hay texto", Toast.LENGTH_SHORT).show();
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Resultado.setText(message.trim());
                    }
                });
            }
        });
    }

    private void tomarFoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 10:
                    Uri MIpath = data.getData();
                    imagen.setImageURI(MIpath);
                    break;
                case 1:
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    imagen.setImageBitmap(imageBitmap);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + requestCode);
            }
        }
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }
}