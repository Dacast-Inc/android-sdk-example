package com.example.test;


import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class MainActivity extends AppCompatActivity {

    DacastPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        player = new DacastPlayer(this, "3b67c4a9-3886-4eb1-d0eb-39b23b14bef3-vod-d77759bd-6167-724b-0d13-9323a27d4f66"/*, "https://cdn.theoplayer.com/demos/preroll.xml"*/);
        ConstraintLayout layout = findViewById(R.id.mainLayout);
        player.getView().setLayoutParams(
            new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        );
        layout.addView(player.getView());
    }

    @Override
    protected void onPause() {
        super.onPause();
        player.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        player.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.onDestroy();
    }
}
