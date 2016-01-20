package org.opennumismat.uscommemoratives;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.widget.ImageView;

/**
 * Created by v.ignatov on 28.10.2014.
 */
public class ImageActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int maxSize = metrics.widthPixels > metrics.heightPixels ? metrics.heightPixels : metrics.widthPixels;

        Intent intent = getIntent();
        Boolean obverse = intent.getExtras().getBoolean(MainActivity.EXTRA_COIN_IMAGE);
        Coin coin = intent.getParcelableExtra(MainActivity.EXTRA_COIN_ID);

        ImageView coin_image = (ImageView) findViewById(R.id.coin_image);
        if (obverse) {
            coin_image.setImageBitmap(coin.getObverseImageBitmap(maxSize));
        }
        else {
            coin_image.setImageBitmap(coin.getReverseImageBitmap(maxSize));
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        actionBar.setTitle(coin.getTitle());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
