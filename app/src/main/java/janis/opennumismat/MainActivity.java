package janis.opennumismat;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    private static final String PREF_LAST_PATH = "last_path";
    private static final int REQUEST_CHOOSER = 1;
    private static final int REQUEST_DOWNLOADER = 2;

    private static final String UPDATE_URL = "https://raw.githubusercontent.com/OpenNumismat/catalogues-mobile/master/update.json";

    public final static String EXTRA_COIN_ID = "org.janis.opennumismat.COIN_ID";
    public final static String EXTRA_COIN_IMAGE = "org.janis.opennumismat.COIN_IMAGE";

    private DrawerLayout drawerLayout;
    private ListView listView;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private Toolbar toolbar;
    private CharSequence title;

    private String[] navigationDrawerItems;

    private SharedPreferences pref;
    SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    private SqlAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        navigationDrawerItems = getResources().getStringArray(R.array.navigation_drawer_items);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        listView = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        listView.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, navigationDrawerItems));
        listView.setOnItemClickListener(new DrawerItemClickListener());

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name) {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                setTitle(R.string.app_name);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (savedInstanceState == null) {
            selectItem(0);
        }

        // Set default density
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (!pref.contains("density")) {
            String density;
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            if (metrics.densityDpi <= metrics.DENSITY_MEDIUM)
                density = "MDPI";
            else if (metrics.densityDpi <= metrics.DENSITY_HIGH)
                density = "HDPI";
            else if (metrics.densityDpi <= metrics.DENSITY_XHIGH)
                density = "XHDPI";
            else if (metrics.densityDpi <= metrics.DENSITY_XXHIGH)
                density = "XXHDPI";
            else
                density = "XXXHDPI";

            SharedPreferences.Editor ed = pref.edit();
            ed.putString("density", density);
            ed.apply();
        }

        // Load latest collection
        String path = pref.getString(PREF_LAST_PATH, "");
        if (!path.isEmpty()) {
            openFile(path, true);
        }
        else {
            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            ad.setMessage(R.string.where_first);
            ad.setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    openDownloadDialog();
                }
            });
            ad.setNeutralButton(R.string.open, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    openFileDialog();
                }
            });
            ad.setCancelable(true);
            ad.show();
        }

        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs,
                                                  String key) {
                if (adapter != null) {
                    if (key.equals("sort_order")) {
                        adapter.refresh();
                    } else if (key.equals("filter_field")) {
                        adapter.setFilterField(prefs.getString(key, adapter.DEFAULT_FILTER));
                        adapter.refresh();

                        title = adapter.getFilter() + " ▼";
                        setTitle(title);
                    }
                }
            }
        };
        pref.registerOnSharedPreferenceChangeListener(prefListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!drawerLayout.isDrawerOpen(findViewById(R.id.left_drawer))) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main, menu);
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_open) {
            openFileDialog();
            return true;
        }
        else if (id == R.id.action_download) {
            openDownloadDialog();
            return true;
        }
        else if (id == R.id.action_update) {
            new DownloadListTask().execute(UPDATE_URL);
            return true;
        }
        else if (id == R.id.action_preferences) {
            startActivity(new Intent(this, PreferencesActivity.class));
            return true;
        }
        else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        else if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openFileDialog() {
        Intent getContentIntent = FileUtils.createGetContentIntent();

        Intent intent = Intent.createChooser(getContentIntent, getString(R.string.file_chooser));
        startActivityForResult(intent, REQUEST_CHOOSER);
    }

    private void openDownloadDialog() {
        Intent intent = new Intent(this, DownloadActivity.class);
        startActivityForResult(intent, REQUEST_DOWNLOADER);
    }

    ProgressDialog pd;
    Handler h;
    private void downloadUpdate(DownloadEntry entry) {
        File targetDirectory = new File(Environment.getExternalStorageDirectory() + File.separator + DownloadActivity.TARGET_DIR);
        if(!targetDirectory.exists()){
            targetDirectory.mkdir();
        }
        entry.file = new File(targetDirectory, entry.file_name);

        pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.downloading));
        // меняем стиль на индикатор
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        // включаем анимацию ожидания
        pd.setIndeterminate(true);
        pd.show();
        h = new Handler() {
            public void handleMessage(Message msg) {
                // выключаем анимацию ожидания
                pd.setIndeterminate(false);
                if (msg.what < pd.getMax()) {
                    pd.setProgress(msg.what);
                } else {
                    pd.dismiss();
                }
            }
        };

        new DownloadFileTask().execute(entry);

        entry.file.delete();
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        Fragment fragment;
        Bundle args = new Bundle();
        TextView text = (TextView) findViewById(R.id.toolbar_title);

        switch (position) {
            case 0:
                fragment = new MainFragment();
                ((MainFragment)fragment).setAdapter(adapter);
                if (adapter != null) {
                    text.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            openContextMenu(v);
                        }
                    });
                    title = adapter.getFilter() + " ▼";
                    setTitle(title);
                }
                else {
                    text.setOnClickListener(null);
                    setTitle(R.string.app_name);
                }
                break;

            case 1:
                fragment = new StatisticsFragment();
                ((StatisticsFragment)fragment).setAdapter(adapter);
                title = navigationDrawerItems[position];
                setTitle(title);
                text.setOnClickListener(null);
                break;

            default:
                return;
        }

        // update the main content by replacing fragments
        fragment.setArguments(args);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        // update selected item and title, then close the drawer
        listView.setItemChecked(position, true);
        drawerLayout.closeDrawer(listView);
    }

    @Override
    public void setTitle(CharSequence title) {
//        getSupportActionBar().setTitle(title);
        TextView text = (TextView) findViewById(R.id.toolbar_title);
        text.setText(title);
        registerForContextMenu(text);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        switch (v.getId()) {
            case R.id.toolbar_title:
                List<String> list = adapter.getFilters();
                for (int i = 0; i < list.size(); i++) {
                    menu.add(0, i, 0, list.get(i));
                }
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        String filter = adapter.getFilters().get(item.getItemId());
        adapter.setFilter(filter);
        title = adapter.getFilter() + " ▼";
        setTitle(title);
        return super.onContextItemSelected(item);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onDestroy() {
        if (adapter != null) {
            adapter.close();
            adapter = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHOOSER:
            case REQUEST_DOWNLOADER:
                if (resultCode == RESULT_OK) {

                    final Uri uri = data.getData();

                    // Get the File path from the Uri
                    String path = FileUtils.getPath(this, uri);
                    if (path != null) {
                        // TODO handle non-primary volumes
                        if (path.equals("TODO")) {
                            Toast toast = Toast.makeText(
                                    getApplicationContext(), getString(R.string.could_not_open_sd), Toast.LENGTH_LONG
                            );
                            toast.show();
                        }
                        else if (FileUtils.isLocal(path)) {
                            openFile(path, false);
                        }
                    }
                }
                break;
        }
    }

    private void openFile(String path, boolean first) {
        try {
            if (adapter != null) {
                adapter.close();
                adapter = null;
            }

            adapter = new SqlAdapter(this, path);
        } catch (SQLiteException e) {
            Toast toast = Toast.makeText(
                    getApplicationContext(), getString(R.string.could_not_open_database) + '\n' + path, Toast.LENGTH_LONG
            );
            toast.show();
        }

        selectItem(0);

        if (adapter != null) {
            if (!first) {
                SharedPreferences.Editor ed = pref.edit();
                ed.putString(PREF_LAST_PATH, path);
                ed.apply();
            }
        }
        else {
            if (first) {
                SharedPreferences.Editor ed = pref.edit();
                ed.remove(PREF_LAST_PATH);
                ed.apply();
            }
        }
    }

    public static class MainFragment extends Fragment {
        private SqlAdapter adapter;

        public MainFragment() {
            // Empty constructor required for fragment subclasses
        }

        public void setAdapter(SqlAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.list_fragment, container, false);

            if (adapter != null) {
                ListView lView = (ListView) rootView.findViewById(R.id.lview);
                lView.setAdapter(adapter);
                lView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                        Coin coin = adapter.getFullItem(pos);

                        Intent intent = new Intent(getActivity().getApplicationContext(), CoinActivity.class);
                        intent.putExtra(EXTRA_COIN_ID, coin);
                        startActivity(intent);
                    }
                });
            }

            return rootView;
        }
    }

    public static class StatisticsFragment extends Fragment {
        private SqlAdapter adapter;

        public StatisticsFragment() {
            // Empty constructor required for fragment subclasses
        }

        public void setAdapter(SqlAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            String text;
            View rootView = inflater.inflate(R.layout.statistics_fragment, container, false);

            text = getString(R.string.total_count) + ": " + adapter.getTotalCount();
            ((TextView) rootView.findViewById(R.id.total_count)).setText(text);
            text = getString(R.string.collected_count) + ": " + adapter.getCollectedCount();
            ((TextView) rootView.findViewById(R.id.collected_count)).setText(text);
            text = getString(R.string.coins_count) + ": " + adapter.getCoinsCount();
            ((TextView) rootView.findViewById(R.id.coins_count)).setText(text);

            return rootView;
        }
    }

    private static class DownloadEntry {
        private final String title;
        private final String date;
        private final String size;
        private final String file_name;
        private final String url;
        public File file;

        private DownloadEntry(String title, String date, String size, String file, String url) {
            this.title = title;
            this.date = date;
            this.size = size;
            this.file_name = file;
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public File getFile() {
            return file;
        }

        public String getDescription() {
            return file_name + ", " + size + ", " + date;
        }
    }

    private class DownloadFileTask extends AsyncTask<DownloadEntry, Void, String> {
        private DownloadEntry entry;

        @Override
        protected String doInBackground(DownloadEntry... entries) {
            entry = entries[0];
            return downloadData();
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                File file = new File(result);
                Uri uri = Uri.fromFile(file);
                setResult(RESULT_OK, new Intent().setData(uri));

                adapter.update(result, entry.title);
                adapter.refresh();
            }
            else {
                Toast toast = Toast.makeText(
                        MainActivity.this, getString(R.string.could_not_download_file) + '\n' + entry.getUrl(), Toast.LENGTH_LONG
                );
                toast.show();

                setResult(RESULT_CANCELED);

                return;
            }
        }

        private String downloadData(){
            try{
                URL url  = new URL(entry.getUrl());
                URLConnection connection = url.openConnection();
                connection.connect();

                int lenghtOfFile = connection.getContentLength();

                InputStream is = url.openStream();

                FileOutputStream fos = new FileOutputStream(entry.getFile());

                byte data[] = new byte[1024];

                int count;
                int total = 0;
                int progress = 0;

                pd.setMax(lenghtOfFile);
                h.sendEmptyMessage(progress);
                while ((count=is.read(data)) != -1)
                {
                    total += count;
                    int temp_progress = (int)total*100/lenghtOfFile;
                    if (temp_progress != progress) {
                        progress = temp_progress;
                        h.sendEmptyMessage(total);
                    }

                    fos.write(data, 0, count);
                }
                h.sendEmptyMessage(lenghtOfFile);

                is.close();
                fos.close();

                return entry.getFile().getPath();

            }catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }
    }

    private class DownloadListTask extends DownloadJsonTask {
        private final Integer LIST_VERSION = 1;

        @Override
        protected void onPostExecute(JSONObject json) {
            if (json == null) {
                Toast toast = Toast.makeText(
                        MainActivity.this, getString(R.string.could_not_download_list) + '\n' + url, Toast.LENGTH_LONG
                );
                toast.show();

                return;
            }

            try {
                if (json.getInt("version") != LIST_VERSION)
                    return;

                String density = pref.getString("density", "XHDPI");
                String catalog = adapter.getCatalogTitle();

                JSONArray cats = json.getJSONArray("catalogues");
                for (int i = 0; i < cats.length(); i++) {
                    JSONObject cat = cats.getJSONObject(i);
                    String file = cat.getString("file");
                    if (file.equals(catalog)) {
                        JSONArray upds = cat.getJSONArray("updates");
                        for (int j = 0; j < upds.length(); j++) {
                            JSONObject upd = upds.getJSONObject(j);

                            if (!adapter.checkUpdate(upd.getString("title"))) {
                                final DownloadEntry entry = new DownloadEntry(upd.getString("title"),
                                        upd.getString("date"), upd.getJSONObject("size").getString(density),
                                        upd.getString("file"), upd.getJSONObject("url").getString(density));

                                AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                                ad.setMessage(R.string.apply_update);
                                ad.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int arg1) {
                                        downloadUpdate(entry);
                                    }
                                });
                                ad.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int arg1) {
                                        dialog.dismiss();
                                    }
                                });
                                ad.setCancelable(true);
                                ad.show();

                                return;
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Toast toast = Toast.makeText(
                    MainActivity.this, getString(R.string.no_updates_available), Toast.LENGTH_LONG
            );
            toast.show();
        }
    }
}
