package com.mangu.testing;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int UPLOAD_CODE = 101;
    private static final String TAG = "MainActivity";
    private static final String NPE_OR_INVALID_VALUE = "NPE or Invalid Value";
    protected LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            //mLocationManager.removeUpdates(locationListener);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };
    private LocationManager mLocationManager;
    private RequestQueue mRequestQueue;
    private Intent openChooser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        ChooserTask chooserTask = new ChooserTask();
        chooserTask.execute();
    }

    public void onClickNoise(View view) {
        Intent intent = new Intent(getApplicationContext(), NoiseActivity.class);
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showAlert();
        }
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(intent);
        }
    }

    public void onClickRecord(View view) {
        Intent intent = new Intent(getApplicationContext(), RecordActivity.class);
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showAlert();
        }
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivityForResult(intent, UPLOAD_CODE);
        }
    }

    private void showAlert() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Debe activar el GPS para continuar")
                .setCancelable(false)
                .setPositiveButton("Activar", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No activar", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UPLOAD_CODE) {
            if (resultCode == 1) {
                try {
                    double value = data.getDoubleExtra("value", -1);
                    List<String> providers = mLocationManager.getProviders(true);
                    Location bestLocation = null;
                    for (String provider : providers) {
                        Location l = mLocationManager.getLastKnownLocation(provider);
                        if (l == null) {
                            continue;
                        }
                        if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                            bestLocation = l;
                        }
                    }
                    if (bestLocation == null) {
                        //Intentando conseguir localizacion por 3G/ WiFi
                        Criteria criteria = new Criteria();
                        criteria.setAccuracy(Criteria.ACCURACY_FINE);
                        criteria.setPowerRequirement(Criteria.POWER_HIGH);
                        LocationListener lL = locationListener;
                        String provider = mLocationManager.getBestProvider(criteria, true);
                        mLocationManager.requestLocationUpdates(provider, 0, 0, lL, getMainLooper());
                        bestLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                    //TO-DO TODO bestLocation to String may cause nullPointerException
                    if (value != -1 && bestLocation != null) {
                        UploadTask uploadTask = new UploadTask(getApplicationContext());
                        String lat_long = bestLocation.getLatitude() + "," + bestLocation.getLongitude();
                        uploadTask.execute(String.valueOf(value), lat_long);
                    } else {
                        Log.e(NPE_OR_INVALID_VALUE, "Value:" + value);
                        if (bestLocation == null) {
                            Log.e(NPE_OR_INVALID_VALUE, "BestLocation is null");
                        }
                    }
                    //Toast.makeText(this.getApplicationContext(),String.valueOf(value), Toast.LENGTH_LONG).show();
                    Log.i("onActivityResult", String.valueOf(value));
                } catch (SecurityException e) {
                    Log.e("SecurityException", e.getLocalizedMessage());
                    Toast.makeText(this.getApplicationContext(), "SecurityException", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void onClickInformation(View view) {
        Intent intent = new Intent(getApplicationContext(), InformationActivity.class);
        startActivity(intent);
    }

    public void onClickShare(View view) {
        if (this.openChooser != null) { //Evitar NPE
            startActivity(openChooser);
        } else {
            startActivity(generateIntent());
        }
    }

    public void setOpenChooser(Intent openChooser) {
        this.openChooser = openChooser;
    }

    public Intent generateIntent() {
        Resources resources = getResources();
        Intent emailIntent = new Intent();
        emailIntent.setAction(Intent.ACTION_SEND);
        // Native email client doesn't currently support HTML, but it doesn't hurt to try in case they fix it
        emailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(resources.getString(R.string.share_email_native)));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.share_email_subject));
        emailIntent.setType("message/rfc822");
        PackageManager pm = getPackageManager();
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        Intent openInChooser = Intent.createChooser(emailIntent, resources.getString(R.string.share_chooser_text));
        List<ResolveInfo> resInfo = pm.queryIntentActivities(sendIntent, 0);
        List<LabeledIntent> intentList = new ArrayList<>();
        for (int i = 0; i < resInfo.size(); i++) {
            // Extract the label, append it, and repackage it in a LabeledIntent
            ResolveInfo ri = resInfo.get(i);
            String packageName = ri.activityInfo.packageName;
            if (packageName.contains("android.email")) {
                emailIntent.setPackage(packageName);
            } else if (packageName.contains("twitter") || packageName.contains("facebook") || packageName.contains("mms") || packageName.contains("android.gm")) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("text/plain");
                if (packageName.contains("twitter")) {
                    intent.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.share_twitter));
                } else if (packageName.contains("facebook")) {
                    // Warning: Facebook IGNORES our text. They say "These fields are intended for users to express themselves. Pre-filling these fields erodes the authenticity of the user voice."
                    // One workaround is to use the Facebook SDK to post, but that doesn't allow the user to choose how they want to share. We can also make a custom landing page, and the link
                    // will show the <meta content ="..."> text from that page with our link in Facebook.
                    intent.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.share_facebook));
                } else if (packageName.contains("android.gm")) { // If Gmail shows up twice, try removing this else-if clause and the reference to "android.gm" above
                    intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(resources.getString(R.string.share_email_gmail)));
                    intent.putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.share_email_subject));
                    intent.setType("message/rfc822");
                }
                intentList.add(new LabeledIntent(intent, packageName, ri.loadLabel(pm), ri.icon));
            }
        }
        // convert intentList to array
        LabeledIntent[] extraIntents = intentList.toArray(new LabeledIntent[intentList.size()]);
        openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
        return openInChooser;
    }

    private class UploadTask extends AsyncTask<String, Integer, Void> {
        private Context context;

        public UploadTask(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(String... params) {

            //POST al server de ANTONIO, TRABAJA CABRON
            //ES DOS DE MAYO Y AUN NO HE PODIDO HACER ESTA PARTE POR TI

            JSONObject jsonObject = new JSONObject();
            JSONObject jsonArray = new JSONObject();
            JSONObject jsonRequest = new JSONObject();
            try {
                jsonObject.put("value", params[0]);
                jsonObject.put("localization", params[1]);
                jsonArray.put("marker", jsonObject);
                jsonRequest = new JSONObject(jsonArray.toString());
            } catch (JSONException e) {
                Log.e("JSONException", e.getMessage());
            } catch (NullPointerException e) {
                Log.e("NullPointerException", e.getMessage());
            }
            final String url = "http://150.214.108.91:8000";
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, jsonRequest, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        VolleyLog.v("Response:%n %s", response.toString(4));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyLog.e("Error: ", error.getMessage());
                    VolleyLog.e("Error: ", error.getLocalizedMessage());
                    VolleyLog.e("Error: ", error.networkResponse.toString());
                }
            });
            req.setTag(TAG);
            mRequestQueue.add(req);
            return null; //I don't like it. I don't liek it
        }
    }

    private class ChooserTask extends AsyncTask<Void, Void, Intent> {

        @Override
        protected Intent doInBackground(Void... params) {
            return generateIntent();
        }

        @Override
        protected void onPostExecute(Intent intent) {
            setOpenChooser(intent);
        }
    }
}
