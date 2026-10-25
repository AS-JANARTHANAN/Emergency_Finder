package com.emergencyroutefinder.ui.user;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.emergencyroutefinder.R;
import com.emergencyroutefinder.model.TrafficCamera;
import com.emergencyroutefinder.network.ApiClient;
import com.emergencyroutefinder.network.ApiService;
import com.emergencyroutefinder.ui.BaseActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Traffic Camera Activity — displays prerecorded camera footage and switches between cameras.
 *
 * [SIMULATION] Data is prerecorded footage, NOT live CCTV.
 * This is clearly labelled throughout the UI.
 *
 * Demonstrates: ExoPlayer video, RecyclerView, Network calls
 */
public class TrafficCameraActivity extends BaseActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private TextView tvCameraName, tvRoadName, tvTrafficLevel, tvSimulationNote;
    private Spinner spinnerCamera;
    private List<TrafficCamera> cameras = new ArrayList<>();
    private TrafficCamera currentCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic_camera);

        playerView     = findViewById(R.id.player_view);
        tvCameraName   = findViewById(R.id.tv_camera_name);
        tvRoadName     = findViewById(R.id.tv_road_name);
        tvTrafficLevel = findViewById(R.id.tv_traffic_level);
        tvSimulationNote = findViewById(R.id.tv_simulation_note);
        spinnerCamera  = findViewById(R.id.spinner_camera);

        // Always show simulation disclaimer
        tvSimulationNote.setText("⚠️ [SIMULATION] Traffic footage is prerecorded demo data. NOT live CCTV.");
        tvSimulationNote.setVisibility(View.VISIBLE);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        loadCameras();
    }

    private void loadCameras() {
        ApiClient.createService(ApiService.class).getCameras()
                .enqueue(new Callback<List<TrafficCamera>>() {
                    @Override
                    public void onResponse(Call<List<TrafficCamera>> call,
                                           Response<List<TrafficCamera>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            cameras = response.body();
                            setupCameraSpinner();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<TrafficCamera>> call, Throwable t) {
                        Toast.makeText(TrafficCameraActivity.this,
                                "Could not load cameras: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupCameraSpinner() {
        List<String> cameraNames = new ArrayList<>();
        for (TrafficCamera cam : cameras) {
            cameraNames.add(cam.getCameraName() + " — " + cam.getRoadName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cameraNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCamera.setAdapter(adapter);

        spinnerCamera.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectCamera(cameras.get(pos));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (!cameras.isEmpty()) selectCamera(cameras.get(0));
    }

    private void selectCamera(TrafficCamera camera) {
        currentCamera = camera;
        tvCameraName.setText("Camera: " + camera.getCameraName());
        tvRoadName.setText("Road: " + camera.getRoadName());
        tvTrafficLevel.setText("Traffic Level: " + camera.getTrafficLevel());

        // Load prerecorded video if path is set
        if (camera.getVideoFilePath() != null && !camera.getVideoFilePath().isEmpty()) {
            String videoPath = ApiClient.getBaseUrl() + "/videos/" + camera.getVideoFilePath();
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoPath));
            player.setMediaItem(mediaItem);
            player.prepare();
            player.setPlayWhenReady(true);
        } else {
            // Show placeholder
            player.stop();
            Toast.makeText(this, "[DEMO] No video available for this camera", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }
}
