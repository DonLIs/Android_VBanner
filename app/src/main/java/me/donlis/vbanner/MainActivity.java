package me.donlis.vbanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {
    private BannerAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        VBannerView<String> recyclerView = findViewById(R.id.recyclerView);

        mAdapter = new BannerAdapter<String>(){

            @Override
            protected void onBindView(ImageView imageView, String data, int position, int pageSize) {

            }

            @Override
            protected int getLayoutId(int viewType) {
                return R.layout.item_img;
            }
        };
        recyclerView.setAdapter(mAdapter);
    }
}