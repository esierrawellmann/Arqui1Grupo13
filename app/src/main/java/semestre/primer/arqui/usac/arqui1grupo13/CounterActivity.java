package semestre.primer.arqui.usac.arqui1grupo13;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Erik on 2/24/2018.
 */

public class CounterActivity extends AppCompatActivity {

    String address = null;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ProgressDialog progress;
    TextView txt = null;

    SoundPool sp = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
    final int soundIds[] = new int[10];

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sp.release();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_number);
        Intent intent = getIntent();
        address = intent.getStringExtra("EXTRA_ADDRESS");


        final ImageButton btn_1 =(ImageButton)findViewById(R.id.btn_key_1);
        ImageButton btn_2 =(ImageButton)findViewById(R.id.btn_key_2);
        ImageButton btn_3 =(ImageButton)findViewById(R.id.btn_key_3);
        ImageButton btn_4 =(ImageButton)findViewById(R.id.btn_key_4);



        setVolumeControlStream(AudioManager.STREAM_MUSIC);


        soundIds[0] = sp.load(CounterActivity.this, R.raw.haha, 1);


        txt = findViewById(R.id.txt_number);

        final ConnectBT bt_interface = new ConnectBT();


        View.OnTouchListener listener = ((new View.OnTouchListener()  {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                int action = motionEvent.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        view.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                        bt_interface.writeKeyPressed(view.getTag().toString());
                        break;
                    case MotionEvent.ACTION_MOVE:

                        break;
                    case MotionEvent.ACTION_UP:
                        view.setBackgroundColor(getResources().getColor(R.color.white));

                        break;
                    case MotionEvent.ACTION_CANCEL:
                        break;
                    default:
                        break;
                }
                return true;
            }
        }));

        btn_1.setOnTouchListener(listener);
        btn_2.setOnTouchListener(listener);
        btn_3.setOnTouchListener(listener);
        btn_4.setOnTouchListener(listener);




        bt_interface.execute();



    }


    private class ConnectBT extends AsyncTask<String, String, String>  // UI thread
    {



        private boolean ConnectSuccess = true; //if it's here, it's almost connected


        private void writeKeyPressed(String key){
            if(btSocket == null || isBtConnected){

                try {
                    OutputStream btSocketOutputStream =  btSocket.getOutputStream();
                    char charToWrite='0';
                    switch (key){
                        case "txt_1":
                            charToWrite='A';
                            break;
                        case "txt_2":
                            charToWrite='B';
                            break;
                        case "txt_3":
                            charToWrite='C';
                            break;
                        case "txt_4":
                            charToWrite='D';
                            break;

                    }
                    btSocketOutputStream.write((byte)charToWrite);
                    btSocketOutputStream.flush();
                    Log.e("Output",key);

                } catch (IOException e) {
                    Log.e("ERROR",e.getMessage(),e);
                }

            }

        }
        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(CounterActivity.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected String doInBackground(String... devices) //while the progress dialog is shown, the connection is done in background
        {

            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(String result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;

            }
            progress.dismiss();
            new SerialWritter().execute();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }



        private class SerialWritter extends AsyncTask<Void, String, Void> {
            protected Void doInBackground(Void... urls) {
                if(isBtConnected){
                    try {
                        InputStream socketInputStream =  btSocket.getInputStream();
                        byte[] buffer = new byte[256];
                        int bytes;

                        // Keep looping to listen for received messages
                       while (true){

                            bytes = socketInputStream.read(buffer);            //read bytes from input buffer
                            final String readMessage = new String(buffer, 0, bytes);
                            // Send the obtained bytes to the UI Activity via handler

                            publishProgress(readMessage);
                            Log.d("Message",readMessage);

                        }
                    } catch (IOException e) {
                        Log.e("ERROR",e.getMessage(),e);
                    }

                }
                return null;
            }

            protected void onProgressUpdate(String... progress) {

                String receivedMessage  = progress[0];
                String textToDisplay = receivedMessage.split("|")[0];
                String playSound = receivedMessage.split("|")[1];

                if(playSound.equals("Y")){
                    sp.play(soundIds[0], 1, 1, 1, 0, 1.0f);

                }
                txt.setText(textToDisplay);
            }

            protected void onPostExecute(Void result) {

            }
        }
    }


    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }

}
