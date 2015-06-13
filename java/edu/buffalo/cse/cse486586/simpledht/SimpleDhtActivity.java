package edu.buffalo.cse.cse486586.simpledht;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SimpleDhtActivity extends Activity {
    SimpleDhtProvider dhtProvider= new SimpleDhtProvider();
    static String port;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);
        
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver()));
        final EditText editText = (EditText) findViewById(R.id.editText1);



        findViewById(R.id.button4).setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){

                        String msg = editText.getText().toString() + "\n";
                        editText.setText(""); // This is one way to reset the input box.
                        /*TextView localTextView = (TextView) findViewById(R.id.local_text_display);
                        localTextView.append("\t" + msg); // This is one way to display a string.
                        TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
                        remoteTextView.append("\n");*/
                        // mContentResolver= getContentResolver();
                       dhtProvider.new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, null);

                    }});

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }

}
