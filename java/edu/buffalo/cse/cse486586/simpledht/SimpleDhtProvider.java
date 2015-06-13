package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SimpleDhtProvider extends ContentProvider {
    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    static TreeMap<String, String> REMOTE_PORTS= new TreeMap<String, String>();
    static String myPort;
    static String pred_port;
    static String succ_port;
    static String my_hash;
    static final int SERVER_PORT = 10000;
    private final Uri mUri= buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
    boolean value_for_star= true;
    boolean value_for_string= true;
    HashMap<String, String> key_values= new HashMap<String, String>();



    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        if(selection.equals("_&_")){
            String parts[]= selection.split("_&_");
            if(parts[0].equals("\"*\"")) {
                if (parts[2].equals(myPort)) {
                    return 0;
                }
                else{
                    File directory= getContext().getFilesDir();
                    String files[]= directory.list();
                    for(String f:files) {
                        getContext().deleteFile(f);
                    }
                    String send= succ_port+"%&"+selection;
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, send, null);
                    return 0;
                }
            }
        }
        else{
            if(selection.equals("*")){
                File directory= getContext().getFilesDir();
                String files[]= directory.list();
                for(String f:files) {
                    getContext().deleteFile(f);
                }
                if(succ_port != null){
                    String send= succ_port+"%&"+"*" +"_&_ALL_&_"+ myPort;
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, send, null);
                }
            }
            else if(selection.equals("@")){
                File directory= getContext().getFilesDir();
                String files[]= directory.list();
                for(String f:files) {
                    getContext().deleteFile(f);
                }
            }
            else{
                getContext().deleteFile(selection);
            }
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        FileOutputStream outputStream;
        String fileName= values.get("key").toString();
        String value= values.get("value").toString();

        try {

            if(succ_port==null){
                outputStream = getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
                outputStream.write(value.getBytes());
                outputStream.close();
            }else if(succ_port.equals(myPort)){
                outputStream = getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
                outputStream.write(value.getBytes());
                outputStream.close();
            }
            else{
                String pred_hash= genHash(Integer.toString(Integer.parseInt(pred_port)/2));
                String keyToInsert= genHash(fileName);
                String msgToSend= "";
                if(my_hash.compareTo(keyToInsert)<0 && pred_hash.compareTo(keyToInsert)<0 && pred_hash.compareTo(my_hash)>0){
                    Log.v("Putting into myself", fileName+" "+value);
                    outputStream = getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
                    outputStream.write(value.getBytes());
                    outputStream.close();


                }
                else if(my_hash.compareTo(keyToInsert)>= 0 && pred_hash.compareTo(keyToInsert)<0){
                    Log.v("Putting into myself", fileName+" "+value);
                    outputStream = getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
                    outputStream.write(value.getBytes());
                    outputStream.close();

                }
                else if(my_hash.compareTo(keyToInsert)>0 && pred_hash.compareTo(keyToInsert)>0 && pred_hash.compareTo(my_hash)>0){
                    Log.v("Putting into myself", fileName+" "+value);
                    outputStream = getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
                    outputStream.write(value.getBytes());
                    outputStream.close();

                    }
                    else{
                        Log.v("Sending to successor after checking predecessor", "");
                        msgToSend=fileName+"_&_"+value+"_&_"+myPort;
                        //keys.add(fileName);
                        String send= succ_port+"%&"+msgToSend;
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, send, null);

                    }
                }




        }
        catch (Exception e) {
            Log.e(TAG, "File write failed");
        }

       // Log.v(myPort, values.toString());
        return uri;
    }
    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub


        try {
            TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
            String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            myPort = String.valueOf((Integer.parseInt(portStr) * 2));
            if(!myPort.equals("11108")){
                String msgToSend = "11108%&I-am-new-node_&_"+myPort;
                my_hash= genHash(portStr);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, null);
             }
            else{
                my_hash= genHash(portStr);
                REMOTE_PORTS.put(my_hash, myPort);
                succ_port= myPort;
                pred_port= myPort;
            }
            //getSuccPredPort();
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
            return false;
        }catch (NoSuchAlgorithmException i){
            Log.e(TAG, "Hash Failed");
        }

        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        String message="";

        File directory= getContext().getFilesDir();
        String files[]= directory.list();

        String query_string= selection;
        try {
            if(query_string.contains("_&_")){
                String parts[]= query_string.split("_&_");
                if(parts[0].equals("\"*\"")){
                    if(parts[2].equals(myPort)){
                        for(String f:files){
                            FileInputStream inputStream = getContext().openFileInput(f);
                            if(inputStream!= null){
                                InputStreamReader inputStreamReader= new InputStreamReader(inputStream);
                                BufferedReader reader= new BufferedReader(inputStreamReader);
                                String line;
                                while((line= reader.readLine())!= null){
                                    message= ""+ line;
                                }
                            }
                           key_values.put(f, message);
                        }
                        for(int i=0; i< parts.length;i++){
                            if(parts[i].contains("&&")){
                                String key_value[]= parts[i].split("&&");
                                key_values.put(key_value[0], key_value[1]);
                            }
                        }
                        Log.v("Values in map", key_values.size()+"");
                        value_for_star= false;
                    }
                    else{
                        String toSend="";
                        Log.v("Values with *", selection);
                        for(String f:files) {
                            FileInputStream inputStream = getContext().openFileInput(f);
                            if (inputStream != null) {
                                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                                BufferedReader reader = new BufferedReader(inputStreamReader);
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    message = "" + line;
                                }
                                Log.v("Inside Loop", f+" "+message);
                               toSend= toSend+"_&_"+f+"&&"+message;
                            }
                        }

                        if(!toSend.equals("")) {
                            toSend = succ_port + "%&" + query_string + toSend;
                            Log.v("Values after", toSend);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, toSend, null);
                            return null;
                        }
                        else{
                            toSend = succ_port + "%&" + query_string;
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, toSend, null);
                            return null;
                        }
                    }
                }
                else{
                    if(parts[2].equals(myPort)){
                        String key_value[]= parts[3].split("&&");
                        Log.v("It came here", key_value[0]+key_value[1]);
                        key_values.put(key_value[0], key_value[1]);
                        Log.v("Values in map", key_values.toString());
                        value_for_string= false;
                        //return matrixCursor;
                    }else{
                        String pred_hash= genHash(Integer.toString(Integer.parseInt(pred_port)/2));
                        String key_hash= genHash(parts[0]);
                        if((my_hash.compareTo(key_hash)<0 && pred_hash.compareTo(key_hash)<0 && pred_hash.compareTo(my_hash)>0)
                                ||(my_hash.compareTo(key_hash)>= 0 && pred_hash.compareTo(key_hash)<0)
                                ||(my_hash.compareTo(key_hash)>0 && pred_hash.compareTo(key_hash)>0 && pred_hash.compareTo(my_hash)>0)){
                            FileInputStream inputStream = getContext().openFileInput(parts[0]);
                            if(inputStream!= null){
                                InputStreamReader inputStreamReader= new InputStreamReader(inputStream);
                                BufferedReader reader= new BufferedReader(inputStreamReader);
                                String line;
                                while((line= reader.readLine())!= null){
                                    message= ""+ line;
                                }
                            }
                            String toSend= parts[2]+"%&"+query_string+"_&_"+ parts[0]+"&&"+message;
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, toSend, null);
                            return null;
                        }
                        else{
                            String msgToSend= succ_port+"%&"+query_string;
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, null);
                            return null;
                        }
                    }

                }
            }
            else{
                if(query_string.equals("\"*\"")){
                    if(succ_port != null && !succ_port.equals(myPort)){
                        MatrixCursor matrixCursor= new MatrixCursor(new String[] {"key", "value"});
                        String msgToSend= succ_port+"%&"+"\"*\"" +"_&_ALL_&_"+ myPort;
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, null);
                        Log.v("Strating of *", "");
                        while(value_for_star){

                        }
                       for(Map.Entry<String, String> entry: key_values.entrySet()) {
                           matrixCursor.addRow(new String[]{entry.getKey(), entry.getValue()});

                       }
                        key_values.clear();
                        value_for_star=true;
                        Log.v("hashmap size", key_values.size()+"");
                        return matrixCursor;
                    }
                    else{
                        MatrixCursor matrixCursor= new MatrixCursor(new String[] {"key", "value"});
                        for(String f:files){
                            FileInputStream inputStream = getContext().openFileInput(f);
                            if(inputStream!= null){
                                InputStreamReader inputStreamReader= new InputStreamReader(inputStream);
                                BufferedReader reader= new BufferedReader(inputStreamReader);
                                String line;
                                while((line= reader.readLine())!= null){
                                    message= ""+ line;
                                }
                            }
                            matrixCursor.addRow(new String[] {f, message});
                        }
                        return matrixCursor;
                    }
                }
                else if(query_string.equals("\"@\"")){
                    MatrixCursor matrixCursor= new MatrixCursor(new String[] {"key", "value"});
                    for(String f:files){
                        FileInputStream inputStream = getContext().openFileInput(f);
                        if(inputStream!= null){
                            InputStreamReader inputStreamReader= new InputStreamReader(inputStream);
                            BufferedReader reader= new BufferedReader(inputStreamReader);
                            String line;
                            while((line= reader.readLine())!= null){
                                message= ""+ line;
                            }
                        }
                        matrixCursor.addRow(new String[] {f, message});
                    }
                    return matrixCursor;
                }
                else{
                    if(succ_port != null && !succ_port.equals(myPort)){
                        String key_hash= genHash(query_string);
                        String pred_hash= genHash(Integer.toString(Integer.parseInt(pred_port)/2));
                        if((my_hash.compareTo(key_hash)<0 && pred_hash.compareTo(key_hash)<0 && pred_hash.compareTo(my_hash)>0)
                                ||(my_hash.compareTo(key_hash)>= 0 && pred_hash.compareTo(key_hash)<0)
                                ||(my_hash.compareTo(key_hash)>0 && pred_hash.compareTo(key_hash)>0 && pred_hash.compareTo(my_hash)>0)) {
                            MatrixCursor matrixCursor = new MatrixCursor(new String[]{"key", "value"});
                            FileInputStream inputStream = getContext().openFileInput(query_string);
                            if (inputStream != null) {
                                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                                BufferedReader reader = new BufferedReader(inputStreamReader);
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    message = "" + line;
                                }
                            }
                            matrixCursor.addRow(new String[]{query_string, message});
                            return matrixCursor;
                        }
                        else{
                            MatrixCursor matrixCursor= new MatrixCursor(new String[] {"key", "value"});
                            String msgToSend= succ_port+"%&"+query_string +"_&_ALL_&_"+ myPort;
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, null);
                            Log.v("It came here before while", "");
                            while(value_for_string){

                            }
                            Log.v("It came here after while", "");
                            for(Map.Entry<String, String> entry: key_values.entrySet()){
                                Log.v("values in hashmpa", entry.getKey()+" "+entry.getValue());
                                matrixCursor.addRow(new String[] {entry.getKey(), entry.getValue()});

                            }
                            value_for_string= true;
                            key_values.clear();
                            Log.v("hashmap size", key_values.size()+"");
                            return matrixCursor;
                        }

                    }
                    else{
                        MatrixCursor matrixCursor= new MatrixCursor(new String[] {"key", "value"});
                        FileInputStream inputStream = getContext().openFileInput(query_string);
                        if(inputStream!= null){
                            InputStreamReader inputStreamReader= new InputStreamReader(inputStream);
                            BufferedReader reader= new BufferedReader(inputStreamReader);
                            String line;
                            while((line= reader.readLine())!= null){
                                message= ""+ line;
                            }
                        }
                        matrixCursor.addRow(new String[] {query_string, message});
                        return matrixCursor;
                    }


                }
            }

        } catch(NoSuchAlgorithmException ni){
            ni.printStackTrace();
        }catch (java.io.IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub

        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            /* Below logic is taken from http://docs.oracle.com/javase/tutorial/networking/sockets/index.html and
            http://developer.android.com/reference/android/os/AsyncTask.html */
            try {


                while (true) {
                    Socket socketS = serverSocket.accept();
                    BufferedReader brReader = new BufferedReader(new InputStreamReader(socketS.getInputStream()));
                    String message;

                    while ((message = brReader.readLine()) != null) {
                        if (message != null) {
                            String parts[] = message.split("_&_");
                            if (myPort.equals("11108")) {
                                if (parts[0].equals("I-am-new-node")) {
                                    int port_to_hash = Integer.parseInt(parts[1]);
                                    String hash_port = genHash(Integer.toString(port_to_hash / 2));
                                    String message_to_join = "";
                                    if (REMOTE_PORTS.size() > 1) {
                                        Set<String> key = REMOTE_PORTS.keySet();
                                        List<String> port_list = new ArrayList<String>(key);
                                        for (int i = 0; i < port_list.size(); i++) {
                                            if (i == (REMOTE_PORTS.size() - 1)) {
                                                //pred_port= parts[1];
                                                message_to_join = "join_&_" + REMOTE_PORTS.get(port_list.get(i)) + "_&_" + REMOTE_PORTS.get(port_list.get(0));
                                                String send = parts[1] + "%&" + message_to_join;
                                                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, send, null);
                                                String message_to_pred_new = REMOTE_PORTS.get(port_list.get(i)) + "%&join_&_succ_&_" + parts[1];
                                                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message_to_pred_new, null);
                                                String message_to_succ_new = REMOTE_PORTS.get(port_list.get(0)) + "%&join_&_pred_&_" + parts[1];
                                                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message_to_succ_new, null);
                                            } else {
                                                if (hash_port.compareTo(port_list.get(i)) > 0 && hash_port.compareTo(port_list.get(i + 1)) < 0) {
                                                    message_to_join = "join_&_" + REMOTE_PORTS.get(port_list.get(i)) + "_&_" + REMOTE_PORTS.get(port_list.get(i + 1));
                                                    String send = parts[1] + "%&" + message_to_join;
                                                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, send, null);
                                                    String message_to_pred_new = REMOTE_PORTS.get(port_list.get(i)) + "%&join_&_succ_&_" + parts[1];
                                                    String message_to_succ_new = REMOTE_PORTS.get(port_list.get(i + 1)) + "%&join_&_pred_&_" + parts[1];
                                                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message_to_pred_new, null);
                                                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message_to_succ_new, null);
                                                    break;
                                                } else {
                                                    continue;
                                                }
                                            }

                                        }
                                    } else {
                                        message_to_join = "join_&_both_&_" + myPort;
                                        succ_port = parts[1];
                                        pred_port = parts[1];
                                        String send = parts[1] + "%&" + message_to_join;
                                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, send, null);
                                    }

                                    REMOTE_PORTS.put(hash_port, parts[1]);
                                    //setSuccPredPort();


                                } else if (parts[1].equals("ALL")) {
                                    if (!parts[0].equals("*")) {
                                        getContext().getContentResolver().query(mUri, null,
                                                message, null, null);
                                    } else if (parts[0].equals("*")) {
                                        getContext().getContentResolver().delete(mUri, "*", null);
                                    }

                                }
                                else if (parts[0].equals("join")) {
                                    if (parts[1].equals("succ")) {
                                        succ_port = parts[2];
                                        Log.v(myPort, succ_port + pred_port + "succ");
                                    } else if (parts[1].equals("pred")) {
                                        pred_port = parts[2];
                                        Log.v(myPort, succ_port + pred_port + "pred");
                                    } else if (parts[1].equals("both")) {
                                        succ_port = parts[2];
                                        pred_port = parts[2];
                                        Log.v(myPort, succ_port + pred_port + "both");
                                    } else {
                                        succ_port = parts[2];
                                        pred_port = parts[1];
                                        Log.v(myPort, succ_port + pred_port + "different");
                                    }
                                } else {
                                    ContentValues[] cv = new ContentValues[1];
                                    cv[0] = new ContentValues();
                                    cv[0].put("key", parts[0]);
                                    cv[0].put("value", parts[1]);
                                    insert(mUri, cv[0]);
                                }
                            } else {
                                Log.v("It came here in server", myPort);
                                    if (parts[0].equals("join")) {
                                        if (parts[1].equals("succ")) {
                                            succ_port = parts[2];
                                            Log.v(myPort, succ_port + pred_port + "succ");
                                        } else if (parts[1].equals("pred")) {
                                            pred_port = parts[2];
                                            Log.v(myPort, succ_port + pred_port + "pred");
                                        } else if (parts[1].equals("both")) {
                                            succ_port = parts[2];
                                            pred_port = parts[2];
                                            Log.v(myPort, succ_port + pred_port + "both");
                                        } else {
                                            succ_port = parts[2];
                                            pred_port = parts[1];
                                            Log.v(myPort, succ_port + pred_port + "different");
                                        }
                                    } else if (parts[1].equals("ALL")) {
                                        if (!parts[0].equals("*")) {
                                            Log.v("It came for All", message);
                                            getContext().getContentResolver().query(mUri, null,
                                                    message, null, null);
                                        } else if (parts[0].equals("*")) {
                                            getContext().getContentResolver().delete(mUri, "*", null);
                                        }

                                    }
                                    else {
                                        ContentValues[] cv = new ContentValues[1];
                                        cv[0] = new ContentValues();
                                        cv[0].put("key", parts[0]);
                                        cv[0].put("value", parts[1]);
                                        insert(mUri, cv[0]);

                                    }


                                }


                        }

                    }
                }

            }catch(NoSuchAlgorithmException ne){
                Log.e(TAG, "Hashing Failed");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    public class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                if(msgs[0].contains("%&")){
                    String parts[]= msgs[0].split("%&");
                    if(parts.length==2){
                        Log.v("Inside client", parts[0]+" "+parts[1]);
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(parts[0]));
                        String msgToSend = parts[1];
                        PrintWriter printOnServer = new PrintWriter(socket.getOutputStream());
                        printOnServer.write(msgToSend);
                        printOnServer.close();
                        socket.close();
                    }

                }
                else {
                    String parts[] = msgs[0].split(" ");
                    String keyToInsert = genHash(parts[0]);
                    String msgToSend = "";
                    if (my_hash.compareTo(keyToInsert) < 0) {

                    }
                    else if(my_hash.compareTo(keyToInsert)== 0){

                    }
                    else if(my_hash.compareTo(keyToInsert)>0){

                    }
                }

            }catch(NoSuchAlgorithmException ne){
                    Log.e(TAG, "Hashing Failed");
                }
            catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            }catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }
}
