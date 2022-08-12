package com.aniketjain.weatherapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import es.dmoral.toasty.Toasty;

public class HomeActivity extends AppCompatActivity {

    private final int WEATHER_FORECAST_APP_UPDATE_REQ_CODE = 101;
    private final String API_KEY = "01ad5963c7c41c09fe755a3efe316736";
    private String URL;
    private final int PERMISSION_CODE = 1;
    private String latitude, longitude;

    private SwipeRefreshLayout main_SRL;
    private SpinKitView progress_bar;
    private RelativeLayout main_layout_RL, main_container_RL;
    private EditText city_et;
    private ImageView search_bar_iv;

    private TextView name_tv, updateAt_tv, desc_tv, temp_tv, min_temp_tv, max_temp_tv, pressure_tv, windSpeed_tv, humidity_tv;
    private String name, updated_at, description, temperature, min_temperature, max_temperature, pressure, wind_speed, humidity;
    private ImageView condition_iv;
    private int condition;
    private long update_time, sunset, sunrise;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set status bar color
        setStatusBarColor();

        //set navigation bar color
        setNavigationBarColor();

        //check for new app update
        checkUpdate();

        setContentView(R.layout.activity_home);

        findViews();
        setRandomlyBG();
        setRefreshLayoutColor();
        listeners();

        //check internet connection & get location
        checkConnection();

    }

    private void findViews() {
        main_layout_RL = findViewById(R.id.main_layout);
        main_SRL = findViewById(R.id.main_refresh_layout);
        progress_bar = findViewById(R.id.progress_bar);
        main_container_RL = findViewById(R.id.main_container);
        city_et = findViewById(R.id.editText_city);
        search_bar_iv = findViewById(R.id.imageView_search_bar);
        name_tv = findViewById(R.id.textView_name);
        updateAt_tv = findViewById(R.id.textView_updated_at);
        condition_iv = findViewById(R.id.imageView_condition);
        desc_tv = findViewById(R.id.textView_description);
        temp_tv = findViewById(R.id.textView_temp);
        min_temp_tv = findViewById(R.id.textView_min_temp);
        max_temp_tv = findViewById(R.id.textView_max_temp);
        pressure_tv = findViewById(R.id.textView_pressure);
        windSpeed_tv = findViewById(R.id.textView_wind_speed);
        humidity_tv = findViewById(R.id.textView_humidity);
    }

    @SuppressLint("SetTextI18n")
    private void updateUIData() {
        name_tv.setText(name);
        updateAt_tv.setText(updated_at);
        condition_iv.setImageResource(getResources().getIdentifier(getIconID(condition), "drawable", getPackageName()));
        desc_tv.setText(description);
        temp_tv.setText(temperature + "°C");
        min_temp_tv.setText(min_temperature + "°C");
        max_temp_tv.setText(max_temperature + "°C");
        pressure_tv.setText(pressure + " mb");
        windSpeed_tv.setText(wind_speed + " km/h");
        humidity_tv.setText(humidity + "%");
    }

    private String getIconID(int condition) {
        if (condition >= 200 && condition <= 232)
            return "thunderstorm";
        else if (condition >= 300 && condition <= 321)
            return "drizzle";
        else if (condition >= 500 && condition <= 531)
            return "rain";
        else if (condition >= 600 && condition <= 622)
            return "snow";
        else if (condition >= 701 && condition <= 781)
            return "wind";
        else if (condition == 800) {
            if (update_time >= sunrise && update_time <= sunset)
                return "clear_day";
            else
                return "clear_night";
        } else if (condition == 801) {
            if (update_time >= sunrise && update_time <= sunset)
                return "few_clouds_day";

            else
                return "few_clouds_night";
        } else if (condition == 802)
            return "scattered_clouds";
        else if (condition == 803 || condition == 804)
            return "broken_clouds";
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void listeners() {
        main_layout_RL.setOnTouchListener((view, motionEvent) -> {
            hideKeyboard(view);
            return false;
        });
        main_SRL.setOnRefreshListener(() -> {
            checkConnection();
            main_SRL.setRefreshing(false);  //for the next time
        });
        city_et.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_GO) {
                SearchCity(city_et.getText().toString());
                hideKeyboard(textView);
                return true;
            }
            return false;
        });
        city_et.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                hideKeyboard(view);
            }
        });
        search_bar_iv.setOnClickListener(l -> SearchCity(city_et.getText().toString()));
        search_bar_iv.setOnTouchListener((view, motionEvent) -> {
            hideKeyboard(view);
            return false;
        });
    }

    private void setRandomlyBG() {
        Random rand = new Random();
        int randomNum = rand.nextInt(3) + 1;
        main_layout_RL.setBackgroundResource(getResources().getIdentifier("main_bg_" + randomNum, "drawable", getPackageName()));
    }

    public void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.statusBarColor));
        }
    }

    private void setNavigationBarColor() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.navBarColor));
        }
    }

    private void setRefreshLayoutColor() {
        main_SRL.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.textColor));
        main_SRL.setColorSchemeColors(getResources().getColor(R.color.editTextToastColor));
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setURL(String latitude, String longitude) {
        URL = "https://api.openweathermap.org/data/2.5/onecall?exclude=hourly,minutely&lat=" + latitude + "&lon=" + longitude + "&appid=" + API_KEY;
    }

    private void setURL(String city) {
        URL = "https://api.openweathermap.org/data/2.5/weather?&q=" + city + "&appid=" + API_KEY;
    }

    private String getCityName(Location location) {
        String city = null;
        try {
            Geocoder geocoder = new Geocoder(HomeActivity.this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            city = addresses.get(0).getLocality();
        } catch (Exception e) {
            Log.d("city", "Error to find the city.");
        }
        return city;
    }

    private void getLocation() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(HomeActivity.this);
        //check permission
        if (ActivityCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(HomeActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        } else {
            client.getLastLocation().addOnSuccessListener(location -> {
                setLongitudeLatitude(location);
                String cityName = getCityName(location);
                getWeatherInfoUsingLocation(latitude, longitude, cityName);
            });
        }
    }

    private void setLongitudeLatitude(Location location) {
        try {
            latitude = String.valueOf(location.getLatitude());
            longitude = String.valueOf(location.getLongitude());
            Log.d("location_lat", latitude);
            Log.d("location_lon", longitude);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void getWeatherInfoUsingLocation(String latitude, String longitude, String cityName) {
        setURL(latitude, longitude);
        RequestQueue requestQueue = Volley.newRequestQueue(HomeActivity.this);
        @SuppressLint("DefaultLocale") JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL, null, response -> {
            try {
                name = cityName;
                update_time = response.getJSONObject("current").getLong("dt");
                sunset = response.getJSONObject("current").getLong("sunset");
                sunrise = response.getJSONObject("current").getLong("sunrise");
                updated_at = new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date(update_time * 1000));
                condition = response.getJSONObject("current").getJSONArray("weather").getJSONObject(0).getInt("id");
                description = response.getJSONObject("current").getJSONArray("weather").getJSONObject(0).getString("main");
                temperature = String.valueOf(Math.round(response.getJSONObject("current").getDouble("temp") - 273.15));
                min_temperature = String.format("%.1f", response.getJSONArray("daily").getJSONObject(0).getJSONObject("temp").getDouble("min") - 273.15);
                max_temperature = String.format("%.1f", response.getJSONArray("daily").getJSONObject(0).getJSONObject("temp").getDouble("max") - 273.15);
                pressure = response.getJSONObject("current").getString("pressure");
                wind_speed = response.getJSONObject("current").getString("wind_speed");
                humidity = response.getJSONObject("current").getString("humidity");
                updateUIData();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> errorToast("JSON Not Working"));
        requestQueue.add(jsonObjectRequest);
    }

    private void SearchCity(String city) {
        if (city == null || city.isEmpty()) {
            errorToast("Please enter the city name");
        } else {
            getWeatherInfoUsingCity(city);
        }
    }

    private void errorToast(String msg) {
        Toasty.custom(this,
                msg,
                R.drawable.error_outline_icon,
                R.color.editTextToastColor,
                Toast.LENGTH_SHORT,
                true, true).show();
    }

    private void getWeatherInfoUsingCity(String city) {
        setURL(city);
        RequestQueue requestQueue = Volley.newRequestQueue(HomeActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL, null, response -> {
            try {
                String lat = response.getJSONObject("coord").getString("lat");
                String lon = response.getJSONObject("coord").getString("lon");
                getWeatherInfoUsingLocation(lat, lon, city);
                city_et.setText("");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> errorToast("Please enter the city name correctly"));
        requestQueue.add(jsonObjectRequest);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toasty.custom(HomeActivity.this,
                        "Permission Granted",
                        R.drawable.granted_icon,
                        R.color.editTextToastColor,
                        Toast.LENGTH_SHORT,
                        true, true).show();
                getLocation();
            } else {
                Toasty.custom(HomeActivity.this,
                        "Permission Denied",
                        R.drawable.denied_icon,
                        R.color.editTextToastColor,
                        Toast.LENGTH_SHORT,
                        true, true).show();
                finish();
            }
        }
    }

    private boolean isInternetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean connection_flag = false;
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            connection_flag = true;
        }
        return connection_flag;
    }

    private void checkConnection() {
        if (!isInternetConnected()) {
            hideRelativeLayout();
            errorToast("Please check your internet connection");
        } else {
            checkUpdate();
            hideProgressBar();
            getLocation();
        }
    }

    private void hideProgressBar() {
        progress_bar.setVisibility(View.GONE);
        main_container_RL.setVisibility(View.VISIBLE);
    }

    private void hideRelativeLayout() {
        progress_bar.setVisibility(View.VISIBLE);
        main_container_RL.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkConnection();
    }

    private void checkUpdate() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(HomeActivity.this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, HomeActivity.this, WEATHER_FORECAST_APP_UPDATE_REQ_CODE);
                } catch (IntentSender.SendIntentException exception) {
                    errorToast("Update Failed");
                }
            }
        });
    }

}