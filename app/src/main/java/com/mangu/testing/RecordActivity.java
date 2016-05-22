package com.mangu.testing;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class RecordActivity extends AppCompatActivity {
    private static final int MY_MSG = 1;
    private static final int ERROR_MSG = -1;
    private SplEngine mEngine;
    private Button start, stop, enviar;
    private boolean stopped = false;
    private double amplitude = -1; //Umbral
    public Handler mHandle = new Handler() {
      @Override
        public void handleMessage(Message msg) {
          switch (msg.what) {
              case MY_MSG:
                amplitude = (double) msg.obj;
                break;
              case ERROR_MSG:
                  Toast.makeText(
                          RecordActivity.this,
                          "Error " + msg.obj, Toast.LENGTH_LONG).show();
                  mEngine.stop_engine();
                  break;
              default :
                  super.handleMessage(msg);
                  break;
          }
      }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        start = (Button) findViewById(R.id.btn_grabar);
        stop = (Button) findViewById(R.id.btn_prueba);
        enviar = (Button) findViewById(R.id.btn_enviar);
        stop.setEnabled(false);
        stop.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        enviar.setEnabled(false);
        enviar.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        mEngine = new SplEngine(this.mHandle, RecordActivity.this);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("value", amplitude);
        setResult(1, intent);
        finish();
    }

    public void start(View view) {
        if(!stopped) {
            mEngine.start_engine();
            start.setEnabled(false);
            start.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            stop.setEnabled(true);
            Toast.makeText(getApplicationContext(), "Recording started",
                    Toast.LENGTH_LONG).show();
            Log.i(this.toString(), "Recording started");
        }else{
            Toast.makeText(RecordActivity.this, "Para grabar otro audio salga y vuelva a entrar en la pantalla", Toast.LENGTH_SHORT).show();
        }
    }

    public void stop(View view) {
        stopped=true;
        amplitude = mEngine.getMedian();
        mEngine.stop_engine();
        stop.setEnabled(false);
        stop.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        start.setEnabled(true);
        enviar.setEnabled(true);
        Toast.makeText(getApplicationContext(), "Value:"+amplitude,
                Toast.LENGTH_LONG).show();
        Log.i(this.toString(), "Value: "+amplitude);

    }


    public void enviar(View view) {
        Intent intent = new Intent();
        intent.putExtra("value", amplitude);
        setResult(1, intent);
        finish();
    }
}
