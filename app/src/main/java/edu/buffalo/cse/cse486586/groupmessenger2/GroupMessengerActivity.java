package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.net.Uri;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.io.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final int SERVER_PORT = 10000;
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    String[] portsArr={"11108","11112","11116","11120","11124"};
    String myPort;
    static int countVar=0;
    private  Uri mUri;

    HashMap<String,Integer> acknowledgeMap;
    HashMap<String,Integer> highestProposedMap;
    static int proposed_number=0;
    static PriorityBlockingQueue<String> pQueue;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        acknowledgeMap=new HashMap<String,Integer>();
        highestProposedMap=new HashMap<String,Integer>();
        pQueue=new PriorityBlockingQueue<String>(10,new comp());

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

           /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket"+e.getMessage());
            return;
        }
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        final EditText editText = (EditText) findViewById(R.id.editText1);


        final Button sendButton = (Button) findViewById(R.id.button4);




        sendButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                String msg = editText.getText().toString().trim();
                editText.setText("");

                String uniqueMsgId=System.currentTimeMillis()+"#"+myPort;
                acknowledgeMap.put(uniqueMsgId,5);
                String msgStr=uniqueMsgId+"#"+msg;
                Log.d("send button message",msgStr);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgStr, myPort);

            }
        });



    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];

            try {
                while (true) {

                    Socket socket = serverSocket.accept();

                    BufferedReader brReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String strReceived = null;
                    while((strReceived = brReader.readLine())!=null) {
                        Log.d("serverTask string received ", strReceived);
                        String[] strArr = strReceived.split("#");
                        String uniqueMsgId = strArr[0] + "#" + strArr[1];
                        Log.d("myPort",myPort);
                        if (strArr.length == 3) {
                            String strNew = strReceived + "#" + proposed_number;
                            proposed_number++;
                            Log.d("serverTask proposed number ", strNew);
                            pQueue.add(strNew);
                            printqueue();
                            Log.d("queuepublishing","                                    ");
                            new MessageClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, strNew, myPort);
                        } else if (strArr.length == 4) {
                            int returnProposedNumber = Integer.parseInt(strArr[3]);

                            int count = acknowledgeMap.get(uniqueMsgId) - 1;
                            acknowledgeMap.put(uniqueMsgId, count);
                            if (!highestProposedMap.containsKey(uniqueMsgId)) {
                                highestProposedMap.put(uniqueMsgId, returnProposedNumber);
                            } else {
                                int no = highestProposedMap.get(uniqueMsgId);
                                highestProposedMap.put(uniqueMsgId, Math.max(no, returnProposedNumber));
                                if (count == 0) {
                                    int agreedNo = highestProposedMap.get(uniqueMsgId);
                                   if(agreedNo>proposed_number)
                                       proposed_number=agreedNo+1;
                                    String strNew = uniqueMsgId + "#" + strArr[2] + "#" + agreedNo + "#" + "true";
                                    Log.d("agreed proposed number ", strNew);
                                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, strNew, myPort);
                                }
                            }
                        } else if (strArr.length == 5) {
                            Iterator<String> it=pQueue.iterator();
                            String str="";
                            while(it.hasNext()) {
                                str = it.next();
                                if (str.contains(uniqueMsgId)) {
                                   Log.d("priority queue removing message ", str);
                                    break;
                                }
                            }
                            pQueue.remove(str);
                            pQueue.add(strReceived);

                            Log.d("Count Zero Received", strReceived);

                            while(!pQueue.isEmpty()) {
                             String st=pQueue.peek();
                              if (st.contains("true")) {
                                    printqueue();
                                    pQueue.poll();
                                  Log.d("databasepublishing ", st);
                                  Log.d("databasepublishing", Long.toString(System.currentTimeMillis()));
                                    publishProgress(st.split("#")[2]);
                                } else
                                    break;//                                   it.remove();

                            }

                        }
                    }
                    socket.close();
                }

            }
            catch (IOException e) {
                Log.d("exception ",e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
        protected void printqueue(){
            PriorityQueue<String> copyQueue = new PriorityQueue<>(pQueue);
            while(!copyQueue.isEmpty())
                Log.d("queuepublishing", copyQueue.poll());
            Log.d("queuepublishing", "                         ");
        }
        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            final TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append("\t" + strReceived+"\n");

            ContentValues contVal=new ContentValues();
            contVal.put("key",Integer.toString(countVar));
            contVal.put("value",strReceived);
            countVar=countVar+1;
            getContentResolver().insert(mUri,contVal);

            return;
        }

    }



    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            for(String port:portsArr) {
                try {


                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));
                    Log.d("client message",msgs[0]);

                    PrintWriter pw =
                            new PrintWriter(socket.getOutputStream(), true);
                    pw.write(msgs[0]);
                    pw.flush();
                    pw.close();
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException"+e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException" +e.getMessage());
                }
            }
            return null;
        }
    }

    private class MessageClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

                try {

                    Log.d("MessageClientTask message",msgs[0]);
                    String[] arr=msgs[0].split("#");
                    String port=arr[1];



                    Log.d("MessageClientTask port ",port);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));

                    PrintWriter pw =
                            new PrintWriter(socket.getOutputStream(), true);
                    pw.write(msgs[0]);
                    pw.flush();
                    pw.close();


                    socket.close();
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException"+e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException" + e.getMessage());
                }
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

     static class comp implements Comparator<String>{
        @Override
        public int compare(String s1, String s2) {
            Log.d("s1 ",s1);
            Log.d("st2 ",s2);

            long str1= Long.parseLong(s1.split("#")[3]+s1.split("#")[1]);
            long str2= Long.parseLong(s2.split("#")[3]+s2.split("#")[1]);

            Log.d("str1 ",str1+"");
            Log.d("str2 ",str2+"");

            if (str1 < str2)
                return -1;
            else if(str1 > str2)
                return 1;
            else
                return 0;
        }
    }
}
