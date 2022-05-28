package com.example.vitalsignsterminal;

import android.app.Activity;
//import android.app.AsyncNotedAppOp;
import android.bluetooth.BluetoothAdapter;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
//import android.os.AsyncTask;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.util.Log;
//import android.util.TypedValue;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.ColorTemplate;
//import com.github.mikephil.charting.utils.Utils;

import java.nio.charset.StandardCharsets;
//import java.util.stream.IntStream;

import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
//import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.Arrays;
import java.util.ArrayList;
import java.util.*;
import java.lang.Integer;
//import java.util.concurrent.atomic.AtomicBoolean;


public class MainActivity extends Activity {
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    //Thread thread;
    //int bufferPosition;

    Button startButton, sendButton, stopButton;
    TextView action_bar, xlabel1, ylabel1, xlabel2, ylabel2;
    TextView headtemp, headSpO2, headbpm, headRR, headBP, disptemp, dispSpO2, dispbpm, dispBP, dispBP2, dispRR;
    ImageView imageView3, imageView2, imageView, imageView4, imageView5;
    private LineChart mChart1, mChart2;
    private float time1 = 0, time2 = 0;
    ConcurrentLinkedQueue<Float> concPPG = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<Float> concHRV = new ConcurrentLinkedQueue<>();
    int bp_number = 5000;
    int numbersToSend = 8500;
    double[] obj = new double[numbersToSend];
    double[] obj1 = new double[bp_number];
    double str;
    int arrCount = 0;
    int countii = 0;
    long countiii = 0;
    int wait1 = 0, wait2 = 0;
    //flags
    public boolean StartStopflag = true;
    boolean deviceConnected = false;
    boolean asyncflag1 = false, asyncflag2 = false, start = false, connect = false;
    boolean stopThread;
    LinkedList<String[]> queue = new LinkedList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = findViewById(R.id.buttonStart);
        sendButton = findViewById(R.id.buttonSend);
        stopButton = findViewById(R.id.buttonStop);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        action_bar = findViewById(R.id.action_bar);
        disptemp = findViewById(R.id.Texttempvalue);
        dispSpO2 = findViewById(R.id.TextSpO2value);
        dispbpm = findViewById(R.id.Textbpmvalue);
        dispBP = findViewById(R.id.textBPressvalue);
        dispBP2 = findViewById(R.id.textBPressvalue2);
        dispRR = findViewById(R.id.textResRvalue);

        headRR = findViewById(R.id.textViewResR);
        headBP = findViewById(R.id.textViewBPress);
        headbpm = findViewById(R.id.textViewbpm);
        headSpO2 = findViewById(R.id.textViewSpO2);
        headtemp = findViewById(R.id.textViewtemp);

        imageView5 = findViewById(R.id.imageView5);
        imageView4 = findViewById(R.id.imageView4);
        imageView3 = findViewById(R.id.imageView3);
        imageView2 = findViewById(R.id.imageView2);
        imageView = findViewById(R.id.imageView1);

        setUiEnabled(false);
        Graphing();

    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        dispbpm.setEnabled(bool);
        dispSpO2.setEnabled(bool);
        disptemp.setEnabled(bool);
        dispRR.setEnabled(bool);
        dispBP.setEnabled(bool);
        dispBP2.setEnabled(bool);
    }

    public boolean BTinit() {
        boolean found = false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device doesn't Support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        assert bluetoothAdapter != null;
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please Pair the Device first", Toast.LENGTH_SHORT).show();
        } else {
            for (BluetoothDevice iterator : bondedDevices) {
                //    private final String DEVICE_NAME="VitalSignsBT";
                //HC05: 00:20:12:08:CC:38, ESP: 7C:9E:BD:07:29:6A
                //String DEVICE_ADDRESS = "3C:61:05:14:B8:9A";
                //String DEVICE_ADDRESS = "58:BF:25:82:7B:2E";
                String DEVICE_NAME = "VitalSigns BTT";
                if (iterator.getName().equals(DEVICE_NAME)) {
                    device = iterator;
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    public boolean BTconnect() {
        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }
        if (connected) {
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return connected;
    }

    public void onClickStart(View view) {
        Toast tst = Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT);
        tst.show();
        if (BTinit()) {
            if (BTconnect()) {
                setUiEnabled(true);
                deviceConnected = true;
                Toast tst2 = Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT);
                tst2.show();
            } else {
                Toast tst4 = Toast.makeText(getApplicationContext(), "Could not be connected", Toast.LENGTH_SHORT);
                tst4.show();
            }
        }
    }

    boolean flaggg = true;
    void beginListenForData() {
        final Handler handler = new Handler();
        stopThread = false;
        dispRR.setText(getString(R.string.Wait));
        dispBP.setText(getString(R.string.Wait));
        dispBP2.setText(getString(R.string.Wait));
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && !stopThread) {
                try {
                    start = false;
                    int byteCount = inputStream.available();
                    if (byteCount > 0) {
                        connect = true;
                        byte[] rawBytes = new byte[byteCount];
                        inputStream.read(rawBytes);
                        final String msg = new String(rawBytes, StandardCharsets.UTF_8);
//                        ms2 = msg;
                       // handler.post(() -> {
////                            if(!asyncflag3) {
//                            Log.d("Hello", "World");
//                            if(flaggg) {
//                                new MyAsyncTask2().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
//                            }
//                           // start = true;
////                            }
                            break_it(msg);
                        //});
                    }
                        handler.post(() -> {
                            if(connect && queue.size() > 1) {
                                draw_it(queue.getFirst());
                                queue.remove();
                            }
                        });
                    try {
                        Thread.sleep(6);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (IOException ex) {
                    stopThread = true;
                }
            }
        });
        thread.start();
    }

    public void onClickSend(View view) {
        String command;
        if (StartStopflag) {
            StartStopflag = false;
            command = "T";
            Toast tst_hold = Toast.makeText(getApplicationContext(), "Keep holding your finger over the sensor", Toast.LENGTH_SHORT);
            tst_hold.show();
            Toast tst_wait = Toast.makeText(getApplicationContext(), "Wait for 1 min for Respiratory Rate", Toast.LENGTH_SHORT);
            tst_wait.show();
            beginListenForData();
        } else {
            StartStopflag = true;
            command = "T";
            Toast tst6 = Toast.makeText(getApplicationContext(), "Stopping", Toast.LENGTH_SHORT);
            tst6.show();
            stopThread = true;
        }
        try {
            outputStream.write(command.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickStop(View view) throws IOException {
        stopThread = true;
        StartStopflag = true;
        outputStream.close();
        inputStream.close();
        socket.close();
        setUiEnabled(false);
        deviceConnected = false;
        Toast tst3 = Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT);
        tst3.show();
    }

    private static int RespirationRate(double[] signalPPG) {
        int interval = 4;
        int start = 500;
        int nearby = 50;
        double thred_ratio = 0.2;
        int elem_max;
        int elem_min;
        int summit;
        List<Integer> s_min = new ArrayList<>();
        List<Integer> s_max = new ArrayList<>();

        float env_peak;
        double env_threenv_s_peak = 0.3;
        int env_flag_thres = 0;
        float env_cur_peak = 1;
        int env_count_breath = 0;
        List<Integer> s_peak_deriv = new ArrayList<>();
        List<Integer> intPPG = new ArrayList<>();
        for (double v : signalPPG) intPPG.add((int) (1000 * v + 0.5));
        float[] m_deriv = sectionppg(signalPPG, interval, start);
        float peak = m_deriv[m_deriv.length - 1];
        int i = 0;
        while (i < m_deriv.length) {
            if (m_deriv[i] > thred_ratio * peak) {
                List<Integer> currList = intPPG.subList(Math.max(1, interval * i - nearby), Math.min(interval * i + nearby, signalPPG.length));
                elem_max = currList.indexOf(Collections.max(currList));
                elem_min = currList.indexOf(Collections.min(currList));
                summit = Collections.max(currList) - Collections.min(currList);
                if (summit > 3 * Math.abs(currList.get(elem_min) - currList.get(Math.max(1, elem_min - 1)))) {
                    peak = (float) (thred_ratio * peak + (1 - thred_ratio) * m_deriv[i]);
                    i = (int) (Math.floor((float) (interval * i - nearby + elem_max + nearby * 2) / interval) + 1);
                    s_min.add(currList.get(elem_min));
                    s_max.add(currList.get(elem_max));
                    if (s_min.size() > 1) {
                        s_peak_deriv.add(currList.get(elem_max) - s_max.get(s_max.size() - 2));
                    }
                } else
                    i += 1;
            } else
                i += 1;
        }
        int env_start = 15;
        float prev_peak = 0;
        try {
            env_peak = Collections.max(s_peak_deriv.subList(0, env_start-1));
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            env_peak = prev_peak;
        }
        i = env_start;
        while (i < s_peak_deriv.size() - 1) {
            float env_peak1 = Collections.max(s_peak_deriv.subList(i - env_start, i));
            if (env_peak1 < env_peak)
                env_peak = env_peak1;
            if (s_peak_deriv.get(i) > env_threenv_s_peak * env_peak & env_flag_thres == 0) {
                env_flag_thres = 1;
                env_cur_peak = s_peak_deriv.get(i);
            }
            i += 1;
            if (env_flag_thres == 1) {
                if (s_peak_deriv.get(i) > env_cur_peak)
                    env_cur_peak = s_peak_deriv.get(i);
                if (s_peak_deriv.get(i) < 0) {
                    env_flag_thres = 0;
                    env_count_breath = env_count_breath + 2;
                }
            }
        }
        return env_count_breath;
    }

    private static float[] sectionppg(double[] PPGarray, int interval, int start) {
        int cols = (int) Math.floor((float) PPGarray.length / interval);
        int[][] deriv = new int[interval][cols - 1];
        int[][] PPGsig = new int[interval][cols];
        int count;
        for (int i = 0; i < interval; i++) {
            count = i;
            for (int j = 0; j < cols; j++) {
                PPGsig[i][j] = (int) (1000 * PPGarray[count] + 0.5);
                count += interval;
            }
        }
        for (int i = 0; i < interval; i++) {
            for (int j = 0; j < cols - 1; j++) {
                deriv[i][j] = PPGsig[i][j + 1] - PPGsig[i][j]; //taking derivative of spaced elements
            }
        }
        float[] m_deriv = new float[cols];
        m_deriv[cols - 1] = 0; //reserved for peak value
        for (int j = 0; j < cols - 1; j++) {
            m_deriv[j] = (float) (deriv[0][j] + deriv[1][j]) / 2; //median of two rows,,, add rows if more than 2
            if (m_deriv[j] > m_deriv[cols - 1] & j < start)
                m_deriv[cols - 1] = m_deriv[j]; //peak value saved in last element of array
        }
        return m_deriv;
    }

    // add a point to a graph
    private float addEntry(LineChart chart, float sht, float time) {
        LineData data = chart.getData();
        if (data != null) {
            LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
            if (set == null) {
                set = createSet();
                if (chart == mChart2) {
                    set.setColor(Color.RED);
                } else {
                    set.setColor(Color.DKGRAY);
                }
                data.addDataSet(set);
            }
            time = time + (float) 0.025;
            data.addEntry(new Entry(time, sht), 0);
            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.setVisibleXRange(0f, 6f);
            chart.moveViewToX(data.getEntryCount());
            chart.invalidate();
        }
        return time;
    }

    // graph line settings
    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Real Time");
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(2.5f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setValueTextColor(Color.GRAY);
        set.setValueTextSize(10f);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        return set;
    }
    // graphs layout settings, sequence chart1, chart2, chart3

    private void Graphing() {
        {
            ylabel1 = findViewById(R.id.ylabel);

            xlabel1 = findViewById(R.id.xlabel);

            mChart1 = findViewById(R.id.graph1);

            ylabel2 = findViewById(R.id.ylabel2);

            xlabel2 = findViewById(R.id.xlabel2);

            mChart2 = findViewById(R.id.graph2);

            mChart1.setDescription(null);
//            mChart1.setOnChartGestureListener(new
//
//                                                      OnChartGestureListener() {
//                                                          @Override
//                                                          public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture
//                                                                  lastPerformedGesture) {
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture
//                                                                  lastPerformedGesture) {
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartLongPressed(MotionEvent me) {
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartDoubleTapped(MotionEvent me) {
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartSingleTapped(MotionEvent me) {
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX,
//                                                                                   float velocityY) {
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartScale(MotionEvent me, float scaleX, float scaleY) {        // allow zoom from chart 1
////                    float[] srcvals = new float[9];
////                    float[] dstvals = new float[9];
////
////                    mChart1.getViewPortHandler().getMatrixTouch().getValues(srcvals);
////                    dstvals[Matrix.MSCALE_X] = srcvals[Matrix.MSCALE_X];
////                    dstvals[Matrix.MTRANS_X] = srcvals[Matrix.MTRANS_X];
////                    dstvals[Matrix.MSCALE_Y] = srcvals[Matrix.MSCALE_Y];
////                    dstvals[Matrix.MTRANS_Y] = srcvals[Matrix.MTRANS_Y];
////                    mChart1.getViewPortHandler().getMatrixTouch().getValues(srcvals);
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartTranslate(MotionEvent me, float dX, float dY) {        // allow drag from chart 1
////                    float[] srcvals = new float[9];
////                    float[] dstvals = new float[9];
////
////                    mChart1.getViewPortHandler().getMatrixTouch().getValues(srcvals);
////                    dstvals[Matrix.MSCALE_Y] = srcvals[Matrix.MSCALE_Y];
////                    dstvals[Matrix.MTRANS_Y] = srcvals[Matrix.MTRANS_Y];
////                    dstvals[Matrix.MSCALE_X] = srcvals[Matrix.MSCALE_X];
////                    dstvals[Matrix.MTRANS_X] = srcvals[Matrix.MTRANS_X];
//
//                                                          }
//                                                      });

            //mChart1.setTouchEnabled(true);
            //mChart1.setDragEnabled(true);
            mChart1.setScaleEnabled(false);
            mChart1.setDrawGridBackground(false);
            //mChart1.setPinchZoom(true);
            mChart1.setBackgroundColor(Color.WHITE);
            mChart1.setExtraLeftOffset(10);
            mChart1.setAutoScaleMinMaxEnabled(true);
            LineData data1 = new LineData();
            data1.setValueTextColor(Color.BLUE);

            mChart1.setData(data1);
            //mChart1.setScaleMinima(6.5f,1f);
            Legend l1 = mChart1.getLegend();
            l1.setForm(Legend.LegendForm.LINE);
            l1.setTextColor(Color.BLUE);
            l1.setEnabled(false);

            XAxis x1 = mChart1.getXAxis();
            x1.setDrawGridLines(true);
            //x1.setAvoidFirstLastClipping(false);
            x1.setDrawAxisLine(true);
            //    x1.setAxisMinValue(0);
            x1.setPosition(XAxis.XAxisPosition.BOTTOM);
            mChart1.setExtraBottomOffset(2);
            x1.setTextColor(Color.BLUE);
            x1.setTextSize(12);
            x1.setDrawLabels(true);

            YAxis y1 = mChart1.getAxisLeft();
            y1.setTextColor(Color.BLUE);
            y1.setDrawGridLines(true);
            y1.setDrawAxisLine(true);
            y1.setDrawLabels(false);

            mChart2.setDescription(null);
//            mChart2.setOnChartGestureListener(new
//
//                                                      OnChartGestureListener() {
//                                                          @Override
//                                                          public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture
//                                                                  lastPerformedGesture) {
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture
//                                                                  lastPerformedGesture) {
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartLongPressed(MotionEvent me) {
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartDoubleTapped(MotionEvent me) {
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartSingleTapped(MotionEvent me) {
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX,
//                                                                                   float velocityY) {
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartScale(MotionEvent me, float scaleX, float scaleY) {        // allow zoom from chart 1
////                    float[] srcvals = new float[9];
////                    float[] dstvals = new float[9];
////
////                    mChart2.getViewPortHandler().getMatrixTouch().getValues(srcvals);
////                    dstvals[Matrix.MSCALE_X] = srcvals[Matrix.MSCALE_X];
////                    dstvals[Matrix.MTRANS_X] = srcvals[Matrix.MTRANS_X];
////                    dstvals[Matrix.MSCALE_Y] = srcvals[Matrix.MSCALE_Y];
////                    dstvals[Matrix.MTRANS_Y] = srcvals[Matrix.MTRANS_Y];
//
//                                                          }
//
//                                                          @Override
//                                                          public void onChartTranslate(MotionEvent me, float dX, float dY) {        // allow drag from chart 1
////                    float[] srcvals = new float[9];
////                    float[] dstvals = new float[9];
////
////                    mChart2.getViewPortHandler().getMatrixTouch().getValues(srcvals);
////                    dstvals[Matrix.MSCALE_X] = srcvals[Matrix.MSCALE_X];
////                    dstvals[Matrix.MTRANS_X] = srcvals[Matrix.MTRANS_X];
////                    dstvals[Matrix.MSCALE_Y] = srcvals[Matrix.MSCALE_Y];
////                    dstvals[Matrix.MTRANS_Y] = srcvals[Matrix.MTRANS_Y];
//                                                          }
//                                                      });
            //mChart2.setTouchEnabled(true);
            // mChart2.setDragEnabled(true);
            mChart2.setScaleEnabled(false);
            mChart2.setDrawGridBackground(false);
            // mChart2.setPinchZoom(true);
            mChart2.setBackgroundColor(Color.WHITE);
            mChart2.setExtraLeftOffset(10);


            //mChart2.setAutoScaleMinMaxEnabled(true);
            LineData data2 = new LineData();
            data2.setValueTextColor(Color.BLUE);
            mChart2.setData(data2);
            //mChart2.setScaleMinima(9.5f, 1f);
            Legend l2 = mChart2.getLegend();
            l2.setForm(Legend.LegendForm.LINE);
            l2.setTextColor(Color.BLUE);
            l2.setEnabled(false);

            XAxis x2 = mChart2.getXAxis();
            x2.setDrawGridLines(true);
            //x2.setAvoidFirstLastClipping(false);
            x2.setDrawAxisLine(true);
            //    x1.setAxisMinValue(0);
            x2.setPosition(XAxis.XAxisPosition.BOTTOM);
            mChart2.setExtraBottomOffset(2);
            x2.setTextColor(Color.BLUE);
            x2.setTextSize(12);
            x2.setDrawLabels(true);

            YAxis y2 = mChart2.getAxisLeft();
            y2.setTextColor(Color.BLUE);
            y2.setDrawGridLines(true);
            y2.setDrawAxisLine(true);
            y2.setDrawLabels(false);
            y2.setAxisMinimum(0);
            mChart2.setOnChartGestureListener(new CoupleChartGestureListener(mChart2, new Chart[]{mChart1}));
            mChart1.setOnChartGestureListener(new CoupleChartGestureListener(mChart1, new Chart[]{mChart2}));
        }
    }


    public static class CoupleChartGestureListener implements OnChartGestureListener {

        private final Chart srcChart;
        private final Chart[] dstCharts;

        public CoupleChartGestureListener(Chart srcChart, Chart[] dstCharts) {
            this.srcChart = srcChart;
            this.dstCharts = dstCharts;
        }

        @Override
        public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

        }

        @Override
        public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

        }

        @Override
        public void onChartLongPressed(MotionEvent me) {

        }

        @Override
        public void onChartDoubleTapped(MotionEvent me) {

        }

        @Override
        public void onChartSingleTapped(MotionEvent me) {

        }

        @Override
        public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

        }

        @Override
        public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
            //Log.d(TAG, "onChartScale " + scaleX + "/" + scaleY + " X=" + me.getX() + "Y=" + me.getY());
            syncCharts();
        }

        @Override
        public void onChartTranslate(MotionEvent me, float dX, float dY) {
            //Log.d(TAG, "onChartTranslate " + dX + "/" + dY + " X=" + me.getX() + "Y=" + me.getY());
            syncCharts();
        }

        public void syncCharts() {
            Matrix srcMatrix;
            float[] srcVals = new float[9];
            Matrix dstMatrix;
            float[] dstVals = new float[9];

            // get src chart translation matrix:
            srcMatrix = srcChart.getViewPortHandler().getMatrixTouch();
            srcMatrix.getValues(srcVals);

            // apply X axis scaling and position to dst charts:
            for (Chart dstChart : dstCharts) {
                if (dstChart.getVisibility() == View.VISIBLE) {
                    dstMatrix = dstChart.getViewPortHandler().getMatrixTouch();
                    dstMatrix.getValues(dstVals);
                    dstVals[Matrix.MSCALE_X] = srcVals[Matrix.MSCALE_X];
                    dstVals[Matrix.MTRANS_X] = srcVals[Matrix.MTRANS_X];
                    dstMatrix.setValues(dstVals);
                    dstChart.getViewPortHandler().refresh(dstMatrix, dstChart, true);
                }
            }
        }
    }

    private static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    private static double[][] Feature_Extraction(double[] signalPPG) {
//        Log.d("Feature Extraction Entered", "Now");
        int fs = 125;
        List<Double> buffer_base_sublist;
        List<Double> int1PPG = new ArrayList<>();
        for (double v : signalPPG) int1PPG.add(v);
        List<Double> buffer_long = new ArrayList<>();
        List<Double> buffer_base = new ArrayList<>();
        List<Double> buffer_plot = new ArrayList<>();
        List<Double> RAWPPG;
        double mean_online;
        double sum;
        double average;
        for (int i = 0; i < signalPPG.length; i++) {
            buffer_long.add(int1PPG.get(i));
            buffer_base.add(int1PPG.get(i));
            // Renew the mean and adapt it to the signal after 1 second of processing
            if (buffer_base.size() >= fs) {
                buffer_base_sublist = buffer_base.subList(1, fs);
                sum = 0;
                for (int j = 0; j < buffer_base_sublist.size(); j++) {
                    sum += buffer_base_sublist.get(j);
                }
                average = sum / buffer_base_sublist.size();
                int finalAverage = (int) average;
                buffer_base_sublist.replaceAll(x -> x - finalAverage);
                for (int j = 0; j < buffer_base_sublist.size(); j++) {
                    if (buffer_base_sublist.get(j) < 0) {
                        buffer_base_sublist.set(j, buffer_base_sublist.get(j) * -1);
                    } else {
                        buffer_base_sublist.set(j, buffer_base_sublist.get(j));
                    }
                }
                for (int j = 0; j < buffer_base_sublist.size(); j++) {
                    sum += buffer_base_sublist.get(j);
                }
                buffer_base.clear();
            }
            // smooth the signal by taking the average of 4 samples and add the new upcoming samples
            if (buffer_long.size() >= 4) {
                sum = 0;
                for (int j = 0; j < buffer_long.size(); j++) {
                    sum += buffer_long.get(j);
                }
                mean_online = sum / buffer_long.size();
                buffer_long.remove(0);
                buffer_plot.add(mean_online);
            }
        }
        RAWPPG = buffer_plot;
        double[] RawPPG = new double[RAWPPG.size()];
        for (int i = 0; i < RAWPPG.size(); i++) {
            RawPPG[i] = RAWPPG.get(i);
        }
        double[][] temp = BP_features(RawPPG);
        double[] footIndex2 = temp[0];
//        Log.d("footIndex2", Arrays.toString(footIndex2));
        double[] systolicIndex2 = temp[1];
//        Log.d("systolicIndex2", Arrays.toString(systolicIndex2));
        // double[] notchIndex2 = temp[2];
//        Log.d("notchIndex2", Arrays.toString(notchIndex2));
        double[] acp = new double[systolicIndex2.length - 1];
        for (int i = 1; i < systolicIndex2.length; i++) {
            acp[i - 1] = systolicIndex2[i] - systolicIndex2[i - 1];
        }
        double[] asut = new double[systolicIndex2.length];
        for (int i = 0; i < systolicIndex2.length; i++) {
            asut[i] = systolicIndex2[i] - footIndex2[i];
        }
        double[] arptt = new double[footIndex2.length - 1];
        for (int i = 0; i < footIndex2.length - 1; i++) {
            arptt[i] = footIndex2[i + 1] - systolicIndex2[i];
        }
//        Log.d("acp", Arrays.toString(acp));
//        Log.d("asut", Arrays.toString(asut));
//        Log.d("arptt", Arrays.toString(arptt));
        int win = 1;
        int win1 = 0;
        int win2 = win * acp.length;
        int size = 0;
        for (int i = win; i <= win2; i += win) {
            size++;
        }
//        Log.d("size", String.valueOf(size));
        double[] f_mcp = new double[size];
        int l = 0;
        double[] f_mcp_scaled = new double[size];
        for (int i = win; i <= win2; i += win) {
            sum = 0;
            int count = 0;
            for (int j = i - win1 - 1; j < i; j++) {
                sum += acp[j];
                count++;
            }
            f_mcp[l] = (sum / count);
            f_mcp_scaled[l] = f_mcp[l] * 0.8;
            l++;
        }
        l = 0;
        double[] f_msut = new double[size];
        double[] f_mrptt_scaled1 = new double[size];
        for (int i = win; i <= win2; i += win) {
            sum = 0;
            int count = 0;
            for (int j = i - win1 - 1; j < i; j++) {
                sum += asut[j];
                count++;
            }
            f_msut[l] = sum / count;
            l++;
        }
        double[] f_mrptt = new double[size];
        l = 0;
        double[] f_mrptt_scaled = new double[size];
        for (int i = win; i <= win2; i += win) {
            sum = 0;
            int count = 0;
            for (int j = i - win1 - 1; j < i; j++) {
                sum += arptt[j];
                count++;
            }
            f_mrptt[l] = sum / count;
            f_mrptt_scaled[l] = f_mrptt[l] * 0.5;
            f_mrptt_scaled1[l] = f_mrptt[l] * 0.6;
            l++;
        }
//        Log.d("Feature Extraction Exited", "Now");
        return new double[][]{f_mcp, f_msut, f_mrptt, f_mrptt_scaled, f_mrptt_scaled1, f_mcp_scaled};
    }

    private static double[][] BP_resample(double[] signalPPG) {
//        Log.d("BP_resample Entered", "Now");
        double fs = 125;
        double duration = signalPPG.length / fs;
        double[] oldx = linespace_i(duration, signalPPG.length);
        double[] newx = linespace_i(duration, signalPPG.length);
        double[] newwaveform = interpolation_i(oldx, signalPPG, newx);
//        Log.d("BP_resample Exited", "Now");
        return new double[][]{newwaveform, newx, oldx};
    }

    private static double[] linespace_i(double duration, int end_i) {
//        Log.d("Linspace Entered", "Now");
        double[] newx = new double[end_i];
        double spacing1 = (duration - 0) / ((duration * 125) - 1);
        for (int i = 0; i < end_i; i++) {
            if (i == 0) {
                newx[0] = 0;
            } else {
                newx[i] = newx[i - 1] + spacing1;
            }
        }
//        Log.d("Linspace Exited", "Now");
        return newx;
    }

    private static double[] interpolation_i(double[] newx, double[] waveform, double[] oldx) {
//        Log.d("Interpolation Entered", "Now");
        double[] newwaveform = new double[waveform.length];
        for (int i = 0; i < waveform.length; i++) {
            if (i < waveform.length - 1) {
                newwaveform[i] = waveform[i] + (waveform[i + 1] - waveform[i]) * (newx[i] - oldx[i]) / (oldx[i + 1] - oldx[i]);
            }
            if (i == newwaveform.length - 1) {
                newwaveform[i] = waveform[i];
            }
        }
//        Log.d("Interpolation Exited", "Now");
        return newwaveform;
    }

    private static double[] convolution_i(double[] u, double[] v) {
//        Log.d("Convolution Entered", "Now");
        int m = u.length;
        int n = v.length;
        double[] w = new double[m + n - 1];
        double s;
        for (int k = 1; k <= w.length; k++) {
            s = 0;
            for (int j = Math.max(1, k + 1 - n); j <= Math.min(k, m); j++) {
                s = s + u[j - 1] * v[k - j];
            }
            w[k - 1] = s;
        }
//        Log.d("Convolution Exited", "Now");
        return w;
    }

    private static double[] filter_i(double[] b, double[] a, double[] x) {
//        Log.d("Filter Entered", "Now");
        double as = 0;
        double bs, ys;
        int h = x.length, n, m;
        double[] y = new double[h];
        for (int k = 1; k <= y.length; k++) {
            bs = 0;
            n = k;
            for (int i = 1; i <= k; i++) {
                if (n > 0) {
                    bs = bs + b[i - 1] * x[n - 1];
                }
                n = n - 1;
            }
            m = k;
            for (int j = 1; j <= a.length - 1; j++) {
                if (m > 1) {
                    as = as + a[j] * y[m - 2];
                }
                m = m - 1;
            }
            ys = bs - as;
            y[k - 1] = ys / a[0];
            as = 0;
        }
//        Log.d("Filter Exited", "Now");
        return y;
    }

    private static double[][] rollingWindow(double[] vector, double winsize) {
//        Log.d("Rolling Window Entered", "Now");
        int vecsize = vector.length;
        double[][] rwin = new double[(int) winsize][vecsize];
        for (int i = 0; i < winsize; i++) {
            for (int j = 0; j < vecsize; j++) {
                rwin[i][j] = Double.NaN;
            }
        }
        double[] tmp = new double[vector.length];
        for (int i = 1; i <= winsize; i++) {
            if (vector.length - i + 1 >= 0)
                System.arraycopy(vector, 0, tmp, 0, vector.length - i + 1);
            int l = 0;
            for (int k = i - 1; k < rwin[0].length; k++) {
                rwin[i - 1][k] = tmp[l];
                l += 1;
            }
            tmp = new double[vector.length - i];
        }
//        Log.d("Rolling Window Exited", "Now");
        return rwin;
    }

    private static double[] winmean(double[][] rwin) {
//        Log.d("Winmean Entered", "Now");
        double sum;
        double[] res = new double[rwin[0].length];
        for (int i = 0; i < rwin[0].length; i++) {
            sum = 0;
            for (double[] doubles : rwin) {
                if (Double.isNaN(doubles[i])) {
                    sum = Double.NaN;
                    break;
                } else {
                    sum += doubles[i];
                }
            }
            res[i] = 1.5 * (sum / rwin.length);
        }
//        Log.d("Winmean Exited", "Now");
        return res;
    }

    private static double[] winsum(double[][] rwin) {
//        Log.d("winsum Entered", "Now");
        double sum;
        double[] res = new double[rwin[0].length];
        for (int i = 0; i < rwin[0].length; i++) {
            sum = 0;
            for (double[] doubles : rwin) {
                if (Double.isNaN(doubles[i])) {
                    sum = Double.NaN;
                    break;
                } else {
                    sum += doubles[i];
                }
            }
            res[i] = sum;
        }
//        Log.d("winsum Exited", "Now");
        return res;
    }

    private static double[] BP_lowpass(double[] waveform) {
//        Log.d("BP_Lowpass Entered", "Now");
        double[] b = {1, 0, 0, 0, 0, 0, -2, 0, 0, 0, 0, 0, 1};
        double[] a = {36, -2 * 36, 36};
        double[] unit = {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        double[] h_1 = filter_i(b, a, unit);
        double[] filtwaveform = convolution_i(waveform, h_1);
        List<Double> int1PPG = new ArrayList<>();
        for (double v : filtwaveform) int1PPG.add(v);
        Collections.rotate(int1PPG, -5);
        for (int i = 0; i < int1PPG.size(); i++) {
            filtwaveform[i] = int1PPG.get(i);
        }
        for (int i = 0; i < 5; i++) {
            filtwaveform[i] = Double.NaN;
        }
        double[] temp = new double[waveform.length];
        System.arraycopy(filtwaveform, 0, temp, 0, waveform.length);
//        Log.d("BP_Lowpass Exited", "Now");
        return temp;
    }

    private static double[][] doubleDerive(double[] waveform) {
//        Log.d("Double Derive Entered", "Now");
        double[] waveformD = new double[waveform.length];
        for (int i = 0; i < waveform.length - 1; i++) {
            waveformD[i] = waveform[i + 1] - waveform[i];
        }
        waveformD[waveform.length - 1] = Double.NaN;
        double[] waveformDD = new double[waveformD.length];
        for (int i = 0; i < waveformDD.length - 1; i++) {
            waveformDD[i] = waveformD[i + 1] - waveformD[i];
        }
        waveformDD[waveformD.length - 1] = Double.NaN;
        waveformDD = BP_lowpass(waveformDD);
        double[] waveformDDPlus = new double[waveformDD.length];
        double[] temp = new double[waveformDDPlus.length];
        for (int i = 0; i < waveformDD.length - 1; i++) {
            if (waveformD[i] > 0 && waveformDD[i] > 0) {
                temp[i] = 1;
            } else {
                temp[i] = 0;
            }
        }
        for (int i = 0; i < waveformDD.length; i++) {
            waveformDDPlus[i] = waveformDD[i] * temp[i];
        }
        for (int i = 0; i < waveformDD.length; i++) {
            waveformDDPlus[i] = Math.pow(waveformDDPlus[i], 2);
        }
//        Log.d("Double Derive Exited", "Now");
        return new double[][]{waveformD, waveformDD, waveformDDPlus};
    }

    private static double[] FixIndex(double[] BrokeIndex, double[] Signal, boolean Down, double minWavelength) {
//        Log.d("FixIndex Entered", "Now");
        double[] fixedIndex = BrokeIndex;
        double NewIndex;
        double Radius = round(minWavelength / 4, 0);
        int change = 10;
        double OldIndex;
        for (int N = 0; N < BrokeIndex.length; N++) {
            if (BrokeIndex[N] + round(minWavelength, 0) + 2 < Signal.length) {
                OldIndex = BrokeIndex[N];
                int count = 0;
                if (Down) {
                    for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                        count++;
                    }
                    double[] TempSignal = new double[count];
                    int l = 0;
                    for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                        if (l >= TempSignal.length) {
                            break;
                        }
                        TempSignal[l] = Signal[i];
                        l++;
                    }
                    List<Double> LIST = new ArrayList<>();
                    for (double v : TempSignal) {
                        LIST.add(v);
                    }
                    NewIndex = LIST.indexOf(Collections.min(LIST));
                    NewIndex = NewIndex + OldIndex - Radius;
                    while (OldIndex != NewIndex) {
                        OldIndex = NewIndex;
                        l = 0;
                        for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                            l++;
                        }
                        TempSignal = new double[l];
                        l = 0;
                        for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                            if (l >= TempSignal.length) {
                                break;
                            }
                            TempSignal[l] = Signal[i];
                            l++;
                        }
                        LIST = new ArrayList<>();
                        for (double v : TempSignal) {
                            LIST.add(v);
                        }
                        NewIndex = LIST.indexOf(Collections.min(LIST));
                        NewIndex = NewIndex + Math.max(OldIndex - Radius, 1) - 1;
                    }
                } else {
                    for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                        count++;
                    }
                    double[] TempSignal = new double[count];
                    int l = 0;
                    for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                        if (l >= TempSignal.length) {
                            break;
                        }
                        TempSignal[l] = Signal[i];
                        l++;
                    }
                    List<Double> LIST = new ArrayList<>();
                    for (double v : TempSignal) {
                        LIST.add(v);
                    }
                    NewIndex = LIST.indexOf(Collections.max(LIST));
                    NewIndex = NewIndex + OldIndex - Radius - 1;
                    while (OldIndex != NewIndex) {
                        OldIndex = NewIndex;
                        l = 0;
                        for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                            if (l >= TempSignal.length) {
                                break;
                            }
                            TempSignal[l] = Signal[i];
                            l++;
                        }
                        LIST = new ArrayList<>();
                        for (double v : TempSignal) {
                            LIST.add(v);
                        }
                        NewIndex = LIST.indexOf(Collections.max(LIST));
                        NewIndex = NewIndex + Math.max(OldIndex - Radius, 1) - 1;
                    }
                }
                double Index = NewIndex;
                fixedIndex[N] = Index;
            }
        }
        for (int i = 0; i < fixedIndex.length; i++) {
            if (fixedIndex[i] >= Signal.length || fixedIndex[i] < 0) {
                fixedIndex = ArrayUtils.remove(fixedIndex, i);
            }
        }
//        Log.d("FixIndex Exited", "Now");
        return fixedIndex;
    }

    private static double[] getFootIndex(double[] bpwaveform, double[] waveformDDPlus, double[] zoneOfInterest) {
//        Log.d("GetFootIndex Entered", "Now");
        double[] zoneWall = new double[bpwaveform.length - 1];
        for (int i = 0; i < zoneOfInterest.length - 1; i++) {
            zoneWall[i] = zoneOfInterest[i + 1] - zoneOfInterest[i];
        }
        int count_start = 0, count_stop = 0;
        for (double v : zoneWall) {
            if (v == 1) {
                count_start++;
            }
            if (v == -1) {
                count_stop++;
            }
        }
        double[] BP_start = new double[count_start];
        double[] BP_stop = new double[count_stop];
        int l1 = 0, l2 = 0;
        for (int i = 0; i < zoneWall.length; i++) {
            if (zoneWall[i] == 1) {
                BP_start[l1] = i;
                l1++;
            }
            if (zoneWall[i] == -1) {
                BP_stop[l2] = i;
                l2++;
            }
        }
        while (BP_stop[0] < BP_start[0]) {
            System.arraycopy(BP_stop, 1, BP_stop, 0, BP_stop.length - 1);
        }
        int nfeet = Math.min(BP_start.length, BP_stop.length);
        double[] footIndex = new double[nfeet];
        int largest = 0;
        int count = 0;
        for (int i = 0; i < nfeet; i++) {
            for (int j = (int) BP_start[i]; j <= BP_stop[i]; j++) {
                if (j >= waveformDDPlus.length || (int) (largest + BP_start[i]) >= waveformDDPlus.length) {
                    //Log.d("HELOOOOOOOOOOOOOOO", "LNFIHREIHGGGGGGGGG");
                    break;
                }
                // Log.d("J: ", String.valueOf(j));
                //  Log.d("WaveformDD", String.valueOf(waveformDDPlus[j]));
                if (waveformDDPlus[j] >= waveformDDPlus[(int) (largest + BP_start[i])]) {
                    largest = count;
                }
                count++;
            }
            count = 0;
            footIndex[i] = largest;
            footIndex[i] = footIndex[i] + BP_start[i] - 1;
        }
        for (int i = 0; i < zoneOfInterest.length - 1; i++) {
            zoneWall[i] = zoneOfInterest[i + 1] - zoneOfInterest[i];
        }
        l1 = 0;
        l2 = 0;
        for (int i = 0; i < zoneWall.length; i++) {
            if (zoneWall[i] == 1) {
                BP_start[l1] = i;
                l1++;
            }
            if (zoneWall[i] == -1) {
                BP_stop[l2] = i;
                l2++;
            }
        }
        while (BP_stop[0] < BP_start[0]) {
            System.arraycopy(BP_stop, 1, BP_stop, 0, BP_stop.length - 1);
        }
        nfeet = Math.min(BP_start.length, BP_stop.length);
        footIndex = new double[nfeet];
        largest = 0;
        count = 0;
        for (int i = 0; i < nfeet; i++) {
            for (int j = (int) BP_start[i]; j <= BP_stop[i]; j++) {
                if (j >= waveformDDPlus.length || (int) (largest + BP_start[i]) >= waveformDDPlus.length) {
                    //Log.d("HELOOOOOOOOOOOOOOO", "LNFIHREIHGGGGGGGGG");
                    break;
                }
                if (waveformDDPlus[j] >= waveformDDPlus[(int) (largest + BP_start[i])]) {
                    largest = count;
                }
                count++;
            }
            count = 0;
            footIndex[i] = largest;
            footIndex[i] = footIndex[i] + BP_start[i] - 1;
        }
        footIndex = FixIndex(footIndex, bpwaveform, true, 10);
//        Log.d("GetFootIndex Exit", "Now");
        return footIndex;
    }

    private static double[] getDicroticIndex(double[] waveformDD, double[] waveformD, double[] bpwaveform, double[] footIndex, double[] systolicIndex) {
        int fs = 125;
//        Log.d("get Dicrotic Index Entered", "Now");
        double[] temp1 = new double[footIndex.length - 1];
        double[] temp2 = new double[footIndex.length - 1];
        System.arraycopy(footIndex, 1, temp1, 0, footIndex.length - 1);
        System.arraycopy(footIndex, 0, temp2, 0, footIndex.length - 1);
        for (int i = 0; i < temp2.length; i++) {
            temp1[i] = temp1[i] - temp2[i];
        }
        Arrays.sort(temp1);
        double median = 0.67;
        try {
            if (temp1.length % 2 == 0) {
                median = ((temp1[temp1.length / 2] + temp1[temp1.length / 2 - 1]) / 2) / fs;
            } else {
                median = temp1[temp1.length / 2] / fs;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        double minWavelength = round((median / 5) * fs, 0);
        double[] straightLines = new double[bpwaveform.length];
        double[] notQuiteSystolicIndex = FixIndex(systolicIndex, bpwaveform, false, minWavelength);
        double slope, intercept;
        for (int i = 0; i < footIndex.length - 1; i++) {
            try {
                slope = (bpwaveform[(int) footIndex[i + 1] + 1] - bpwaveform[(int) notQuiteSystolicIndex[i] + 1]) / (footIndex[i + 1] - notQuiteSystolicIndex[i]);
                intercept = bpwaveform[(int) footIndex[i + 1] + 1] - (slope * (footIndex[i + 1] + 2));
                if (notQuiteSystolicIndex[i] + 1 - (int) (footIndex[i] + 1) >= 0)
                    System.arraycopy(bpwaveform, (int) (footIndex[i] + 1), straightLines, (int) (footIndex[i] + 1), (int) (notQuiteSystolicIndex[i] + 1 - (int) (footIndex[i] + 1)));
                for (int j = (int) notQuiteSystolicIndex[i] + 2; j <= footIndex[i + 1] + 2; j++) {
                    straightLines[j - 1] = (slope * (j)) + intercept;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
        double[] eyeballsignal = new double[bpwaveform.length];
        for (int i = 0; i < bpwaveform.length; i++) {
            eyeballsignal[i] = bpwaveform[i] - straightLines[i];
        }
        int count;
        int l = 0;
        for (int i = (int) footIndex[footIndex.length - 1] + 1; i < waveformDD.length; i++) {
            eyeballsignal[i] = waveformDD[i];
            l++;
        }
        double[] systolicIndexTemp = new double[systolicIndex.length];
        for (int i = 0; i < systolicIndex.length; i++) {
            systolicIndexTemp[i] = systolicIndex[i] + round(minWavelength, 0);
        }
        double[] notchIndex = FixIndex(systolicIndexTemp, eyeballsignal, true, minWavelength / 4);
        double[] notchIndexTemp = new double[notchIndex.length];
        for (int i = 0; i < notchIndex.length; i++) {
            notchIndexTemp[i] = notchIndex[i] + round(minWavelength, 0) + 1;
        }
        double[] dicroticIndex = FixIndex(notchIndexTemp, waveformDD, true, round(0.25 * minWavelength, 0));
        double[] temp = new double[dicroticIndex.length];
        System.arraycopy(systolicIndex, 0, temp, 0, dicroticIndex.length);
        systolicIndex = new double[temp.length];
        System.arraycopy(temp, 0, systolicIndex, 0, temp.length);
        double Start, End;
        for (int i = 0; i < systolicIndex.length; i++) {
            Start = systolicIndex[i] + round(minWavelength / 2, 0);
            End = Math.min(dicroticIndex[i] + round(minWavelength / 4, 0), waveformD.length);
            count = 0;
            for (int j = (int) Start; j <= End; j++) {
                count++;
            }
            double[] ZOI = new double[count];
            l = 0;
            for (int j = (int) Start; j <= End; j++) {
                if (j + 1 >= waveformD.length || j >= ZOI.length) {
                    break;
                }
                ZOI[l] = waveformD[j + 1];
                l++;
            }
            if (ZOI.length <= 0) {
                break;
            }
            double[] ZOITEMP1 = new double[ZOI.length - 1], ZOITEMP2 = new double[ZOI.length - 1];
            System.arraycopy(ZOI, 1, ZOITEMP1, 0, ZOI.length - 1);
            System.arraycopy(ZOI, 0, ZOITEMP2, 0, ZOI.length - 1);
            ZOI = new double[ZOI.length - 1];
            for (int j = 0; j < ZOI.length - 1; j++) {
                ZOI[j] = ZOITEMP1[j] * ZOITEMP2[j];
            }
            l = 0;
            for (double v : ZOI) {
                if (v < 0) {
                    l++;
                }
            }
            notchIndexTemp = new double[notchIndex.length];
            for (int j = 0; j < notchIndex.length; j++) {
                notchIndexTemp[i] = notchIndex[i] + round(0.25 * minWavelength, 0);
            }
            if (l >= 2) {
                notchIndex[i] = FixIndex(notchIndex[i], bpwaveform, true);
                if (notchIndex[i] >= bpwaveform.length || notchIndex[i] < 0) {
                    notchIndex = ArrayUtils.remove(notchIndex, i);
                }
                dicroticIndex[i] = FixIndex(Math.min(notchIndexTemp[i], bpwaveform.length), bpwaveform, false);
                if (dicroticIndex[i] >= bpwaveform.length || dicroticIndex[i] < 0) {
                    dicroticIndex = ArrayUtils.remove(dicroticIndex, i);
                }
            }
        }
//        Log.d("get Dicrotic Index Ended", "Now");
        return notchIndex;
    }

    private static double FixIndex(double BrokeIndex, double[] Signal, boolean Down) {
//        Log.d("Fix Index Single element Entered", "Now");
        double fixedIndex = BrokeIndex;
        double NewIndex;
        double Radius = round((double) 4 / 4, 0);
        int change = 10;
        double OldIndex;
        if (BrokeIndex + round(4, 0) + 2 < Signal.length) {
            OldIndex = BrokeIndex;
            int count = 0;
            if (Down) {
                for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                    count++;
                }
                double[] TempSignal = new double[count];
                int l = 0;
                for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                    TempSignal[l] = Signal[i];
                    l++;
                }
                List<Double> LIST = new ArrayList<>();
                for (double v : TempSignal) {
                    LIST.add(v);
                }
                NewIndex = LIST.indexOf(Collections.min(LIST));
                NewIndex = NewIndex + OldIndex - Radius;
                while (OldIndex != NewIndex) {
                    OldIndex = NewIndex;
                    l = 0;
                    for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                        l++;
                    }
                    TempSignal = new double[l];
                    l = 0;
                    for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                        TempSignal[l] = Signal[i];
                        l++;
                    }
                    LIST = new ArrayList<>();
                    for (double v : TempSignal) {
                        LIST.add(v);
                    }
                    NewIndex = LIST.indexOf(Collections.min(LIST));
                    NewIndex = NewIndex + Math.max(OldIndex - Radius, 1) - 1;
                }
            } else {
                for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                    count++;
                }
                double[] TempSignal = new double[count];
                int l = 0;
                for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                    TempSignal[l] = Signal[i];
                    l++;
                }
                List<Double> LIST = new ArrayList<>();
                for (double v : TempSignal) {
                    LIST.add(v);
                }
                NewIndex = LIST.indexOf(Collections.max(LIST));
                NewIndex = NewIndex + OldIndex - Radius - 1;
                while (OldIndex != NewIndex) {
                    OldIndex = NewIndex;
                    l = 0;
                    for (int i = (int) Math.max(OldIndex - Radius, 1); i <= Math.min(OldIndex + Radius + change, Signal.length - 1); i++) {
                        TempSignal[l] = Signal[i];
                        l++;
                    }
                    LIST = new ArrayList<>();
                    for (double v : TempSignal) {
                        LIST.add(v);
                    }
                    NewIndex = LIST.indexOf(Collections.max(LIST));
                    NewIndex = NewIndex + Math.max(OldIndex - Radius, 1) - 1;
                }
            }
            fixedIndex = NewIndex;
        }
//        Log.d("Fix Index Single element Exited", "Now");
        return fixedIndex;
    }

    private static double[][] BP_features(double[] inwaveform) {
//        Log.d("BP_features Entered", "Now");
        int Fs = 125;
        int integwinsize = (int) Math.floor(Fs / 4.0);
        int threswinsize = (int) Math.floor(Fs * 3);
        double[][] temp = BP_resample(inwaveform);
        double[] bpwaveform = temp[0];
        double[] BP_integral = new double[bpwaveform.length];
        double[] threshold = new double[bpwaveform.length];
        bpwaveform = BP_lowpass(bpwaveform);
        temp = doubleDerive(bpwaveform);
        double[] waveformD = temp[0];
        double[] waveformDD = temp[1];
        double[] waveformDDPlus = temp[2];
        float sizelimit = (float) (3 * Math.pow(10, 5));
        if (bpwaveform.length > sizelimit) {
            int numSubParts = (int) Math.ceil(bpwaveform.length / sizelimit);
            int overlap = (int) round((numSubParts * sizelimit - bpwaveform.length) / (numSubParts - 1.0), 1);
            for (int i = 0; i < numSubParts; i++) {
                double Start = ((i - 1) * sizelimit) - (i - 1) * overlap + 1;
                double End = Math.min(Start + sizelimit - 1, bpwaveform.length);
                double[] temp1 = new double[(int) (End - Start)];
                if (End - (int) Start >= 0)
                    System.arraycopy(waveformDDPlus, (int) Start, temp1, (int) Start, (int) (End - (int) Start));
                double[][] subIntegralWindow = rollingWindow(temp1, integwinsize);
                double[] subIntegral = winsum(subIntegralWindow);
                List<Double> doubleSub = new ArrayList<>();
                for (double v : subIntegral) doubleSub.add(v);
                Collections.rotate(doubleSub, (int) -Math.floor(integwinsize / 2.0));
                for (int j = 0; j < doubleSub.size(); j++) {
                    subIntegral[j] = doubleSub.get(j);
                }
                double[][] subThresholdWindow = rollingWindow(subIntegral, Math.floor(threswinsize));
                double[] subThreshold = winmean(subThresholdWindow);
                temp1 = new double[subIntegral.length - 1 + overlap];
                if (subIntegral.length - (1 + overlap) >= 0)
                    System.arraycopy(subIntegral, 1 + overlap, temp1, 1 + overlap, subIntegral.length - (1 + overlap));
                if (End - ((int) Start + overlap) >= 0)
                    System.arraycopy(temp1, (int) Start + overlap, BP_integral, (int) Start + overlap, (int) (End - ((int) Start + overlap)));
                temp1 = new double[subThreshold.length - 1 + overlap];
                if (subThreshold.length - (1 + overlap) >= 0)
                    System.arraycopy(subThreshold, 1 + overlap, temp1, 1 + overlap, subThreshold.length - (1 + overlap));
                if (End - ((int) Start + overlap) >= 0)
                    System.arraycopy(temp1, (int) Start + overlap, threshold, (int) Start + overlap, (int) (End - ((int) Start + overlap)));
                if (i > 0) {
                    double sum = 0;
                    temp1 = new double[overlap];
                    if (Start + overlap - (int) Start >= 0)
                        System.arraycopy(BP_integral, (int) Start, temp1, (int) Start, (int) (Start + overlap - (int) Start));
                    double[] temp2 = new double[overlap];
                    System.arraycopy(subIntegral, 0, temp2, 0, overlap);
                    temp = new double[][]{temp1, temp2};
                    for (int j = (int) Start; j < Start + overlap; j++) {
                        for (double[] doubles : temp) {
                            sum += doubles[j];
                        }
                        BP_integral[j] = sum / temp.length;
                    }
                    temp1 = new double[overlap];
                    if (Start + overlap - (int) Start >= 0)
                        System.arraycopy(threshold, (int) Start, temp1, (int) Start, (int) (Start + overlap - (int) Start));
                    temp2 = new double[overlap];
                    System.arraycopy(subThreshold, 0, temp2, 0, overlap);
                    temp = new double[][]{temp1, temp2};
                    for (int j = (int) Start; j < Start + overlap; j++) {
                        for (double[] doubles : temp) {
                            sum += doubles[j];
                        }
                        threshold[j] = sum / temp.length;
                    }
                } else {
                    temp1 = new double[overlap];
                    System.arraycopy(subIntegral, 0, temp1, 0, overlap);
                    if (Start + overlap - (int) Start >= 0)
                        System.arraycopy(temp1, (int) Start, BP_integral, (int) Start, (int) (Start + overlap - (int) Start));
                    temp1 = new double[overlap];
                    System.arraycopy(subThreshold, 0, temp1, 0, overlap);
                    if (Start + overlap - (int) Start >= 0)
                        System.arraycopy(temp1, (int) Start, threshold, (int) Start, (int) (Start + overlap - (int) Start));
                }
            }
        } else {
            double[][] integralWindow = rollingWindow(waveformDDPlus, integwinsize);
            BP_integral = winsum(integralWindow);
            List<Double> BP = new ArrayList<>();
            for (double v : BP_integral) BP.add(v);
            Collections.rotate(BP, (int) -Math.floor(integwinsize / 2.0));
            for (int j = 0; j < BP.size(); j++) {
                BP_integral[j] = BP.get(j);
            }
            double[][] thresholdWindow = rollingWindow(BP_integral, threswinsize);
            threshold = winmean(thresholdWindow);
        }
        int count = 0;
        for (double v : threshold) {
            if (!Double.isNaN(v)) {
                count++;
            }
        }
        double[] firstNotNan = new double[count];
        int l = 0;
        for (int i = 0; i < threshold.length; i++) {
            if (!Double.isNaN(threshold[i])) {
                firstNotNan[l] = i;
                l++;
            }
        }
        double sum = 0;
        count = 0;
        for (int i = (int) firstNotNan[0]; i < firstNotNan[0] + integwinsize; i++) {
            sum += threshold[i];
            count++;
        }
        double firstNotNanInt = sum / count;
        for (int i = 0; i < threshold.length; i++) {
            if (Double.isNaN(threshold[i])) {
                threshold[i] = firstNotNanInt;
            }
        }
        for (int i = 0; i < BP_integral.length; i++) {
            if (Double.isNaN(BP_integral[i])) {
                BP_integral[i] = 0;
            }
        }
        double[] zoneOfInterest = new double[BP_integral.length];
        for (int i = 0; i < BP_integral.length; i++) {
            if (BP_integral[i] > threshold[i]) {
                zoneOfInterest[i] = 1;
            } else {
                zoneOfInterest[i] = 0;
            }
        }
        double[] footIndex = getFootIndex(bpwaveform, waveformDDPlus, zoneOfInterest);
        double[] footIndexIntegwinsize = new double[footIndex.length];
        for (int i = 0; i < footIndex.length; i++) {
            footIndexIntegwinsize[i] = footIndex[i] + Math.floor(integwinsize / 2.0);
        }
        double[] systolicIndex = FixIndex(footIndexIntegwinsize, bpwaveform, false, Math.floor(integwinsize / 2.0));
        double[] notchIndex = getDicroticIndex(waveformDD, waveformD, bpwaveform, footIndex, systolicIndex);
        //double[] dicroticIndex = temp[0];
        //double[] notchIndex = temp[1];
        double[] footTemp = new double[notchIndex.length];
        double[] systTemp = new double[notchIndex.length];
        System.arraycopy(footIndex, 0, footTemp, 0, footTemp.length);
        System.arraycopy(systolicIndex, 0, systTemp, 0, systTemp.length);
        footIndex = new double[notchIndex.length];
        System.arraycopy(footTemp, 0, footIndex, 0, footIndex.length);
        systolicIndex = new double[notchIndex.length];
        System.arraycopy(systTemp, 0, systolicIndex, 0, systolicIndex.length);
//        Log.d("BP_features Exited", "Now");
        return new double[][]{footIndex, systolicIndex};
    }

    private static double[] ML(double[] signal) {
//        Log.d("ML Entered", "Now");
        double[][] x = Feature_Extraction(signal);
//        Log.d("Feature Extraction", "Done");
        double[] first_t = {92.25, 93.875, 91.375, 89.625, 89.375, 88.5, 87.5, 86.75, 86.125, 85.375, 85.25, 85};
        double[] second_t = {48.75, 50.375, 47.875, 47, 46.75, 46.125, 45.25, 44.75, 44.125, 44, 44, 44};
        double[][] t = {first_t, second_t};
        double[][] Y = new double[t.length][x[0].length];
        double[] x_xoffset = {53.0714285714286, 15.9166666666667, 42.4571428571429, 13.8461538461539, 18.2769230769231, 27.6923076923077};
        double[] x_gain = {0.0219263899765074, 0.0958357102110667, 0.0274079874706343, 0.0723226703755216, 0.0547899017996376, 0.0361613351877608};
        double x_ymin = -1;
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[0].length; j++) {
                x[i][j] = x[i][j] - x_xoffset[i];
                x[i][j] = x[i][j] * x_gain[i];
                x[i][j] = x[i][j] + x_ymin;
            }
        }
        double[][] IW = {{-0.0200991826775102, -0.833050866398596, 0.935752439648157, 0.472120595047717, 1.07388654447504, -0.938743701349115}, {1.02489403668311, -1.19468110327629, -0.397775039104560, 0.573456941078942, -1.17861516047607, -0.207299248886127},
                {0.506906738791016, 0.518767567231249, -1.12863044839652, 0.679133863521602, -0.695564355540819, -1.22061085762415}, {0.402185511993089, -0.462501637635624, 1.10276201307748, -0.891842380851132, -1.10768818308775, 0.660709735027869},
                {0.506921339317609, 0.293596194544210, 0.941535979881278, 0.288080528697618, 1.18627894521803, -1.37653551006879}, {0.376955032178594, -0.461156264806345, 1.21043270851746, -1.36080236292460, -0.535521025926567, 0.159702449040687},
                {-0.792264430901765, -1.09686882158300, -0.909860276456376, -0.908305819330013, -0.443865521264159, 0.643766793712124}, {0.107249444344322, 0.115462544791344, -0.203834692995068, 1.46526301031679, -1.14448630997732, -0.740888158418231},
                {-0.891242260256945, -1.16254186388646, 0.357116440889153, -1.28705385184043, -0.666801957700382, -0.334662802512497}, {-1.26762145270812, 1.37296351004476, 0.595039922830672, 0.307733027855519, -0.382616556161138, 0.228130325310044}};
        double[][] LW1 = {{-0.904927919940651, -0.631182027053450, 0.352519511200031, 0.417774330449566, -0.713958773083743, -0.435370922782086, -0.214145760098128, -0.642639522096996, -0.373991183634285, -0.490675416359647}, {0.0323343085005032, 0.828798947055050, -0.813359799295548, -0.000283683207181956, -0.329377017143181, -0.793938306123368, -0.381902291023016, 0.153165220960889, -0.119138003753089, -0.819186075836119},
                {0.362134412042868, 0.178270777584863, 0.212649378323875, -0.0242651670936495, 0.0969172360749056, 0.757347052867615, 0.735275845360664, 1.02308265863563, 0.511502457093614, 0.615724987340545}, {-0.342506912904627, 0.0884369455470178, 0.640274599991766, 0.440091799632239, 0.980484814316629, 0.0759354942997365, 0.310457148497114, 0.153945302892031, 0.747659235021213, 0.942655179834930},
                {0.658512662904941, -0.627504754239145, -0.0816823042567715, 0.488788264654231, 0.373193638114053, 0.689088025124483, -0.494666136689863, -0.774377828709914, -0.242596615162104, -0.456126157619557}, {0.705138442939135, -0.647085256431998, 0.769193046382807, 0.452683283757272, -0.328013993984889, -0.682740782604954, -0.270199592992567, -0.685292398605803, 0.232000869372160, 0.425566872791049},
                {0.609258213971663, -0.445779727781371, -0.678079143006150, -0.324158272129983, -0.420169253273382, 0.149238437865068, -0.578912933495382, 0.561116193588682, 0.864341442073603, -0.564725176718940}, {0.329810409274718, 0.575125973365740, -0.449000485699684, -0.268897064452637, 0.986900146887980, -0.348070515298431, 0.220270964437076, 0.0148152166283292, -0.876708121694692, 0.900096298660046},
                {-0.515477925417588, 0.902548988730477, 0.0169954697915711, 0.572622071122629, 0.839352196357010, 0.722420097719553, 0.185827013446554, -0.249320287778836, 0.171823659595213, 0.624177841425985}, {0.433318451124271, 0.500876463447503, -0.952797326014139, -0.659344151010197, 0.229495455287008, -0.513419093919314, -0.615298328272070, 0.552249514150368, -0.233323023541068, -0.215782640790156}};
        double[][] LW2 = {{0.421670172882538, -0.890773432484724, 0.346991430397433, 0.886099909425850, -0.131670092211930, -0.625032869555704, -0.182034553175678, -0.299237308141922, -0.801661495681554, -0.0895368283468213}, {-0.00363792710125677, 0.992126571158183, -0.215255593896675, 0.0774177142622538, 0.0939622420188111, -0.491741648946865, 0.766867186594531, 0.214762707334625, -0.707387015204081, -0.827848277915831},
                {0.489010296320660, 0.348260006154147, 0.535547112603122, 0.575093659769487, 0.594068675766321, -0.534408842049855, 0.433655021276182, 0.851822681217342, -0.767889622836880, -0.569154788085137}, {-0.273849197763390, 0.184705140372969, -0.0850585192629534, -0.898149259742989, -1.04830095614532, -0.730039920747593, -0.256811975515711, 0.413316277097884, -0.120623758517953, 0.541979559882067},
                {-0.694089489080220, -0.500081057870596, -0.503198365038160, -0.807657030980109, 0.483174791215576, -0.329842756694239, 0.681442752932356, -0.167343208705604, -0.850281252251180, -0.196300597391359}, {0.300463588968631, 0.0799525819006866, -0.314994774576128, -0.500585771049915, -0.634619000718613, 0.682578404937278, 0.687427303916025, 0.457517935924581, 0.737927902194040, 0.680620246338634},
                {1.11563613374727, 0.0770368909670601, 0.491158570691079, 0.165056291644472, -0.0908966807745011, -0.110492331976639, -0.0471934755898935, 1.17533378063698, 0.323643120426750, -0.119785167912667}, {-0.513145595396169, 1.08353333293065, 0.0434400009062933, -0.000385440136040256, -0.329401263628290, -0.305008405663242, 0.660529480967982, 0.0719534797006586, -1.10567285393543, -0.0814901811786529},
                {-0.181548294164904, 0.527768123880439, 0.503505345222459, 0.748584648506999, -0.0858546098459398, -0.486983448810154, 0.642290940237582, 0.0585564302304708, -0.762013985598331, -0.829180192792593}, {0.451327280276053, 0.782522930648841, 0.774680697806980, 0.0129000409572925, 0.425457433240979, 0.733034375864324, -0.0469723579243973, -0.289127320002413, -0.553623180312217, -0.696113342094660}};
        double[][] LW3 = {{-0.759923217351771, -0.561339551399418, 0.961027901051898, -0.0642569285774393, -0.551183756762926, 0.252522562999444, -0.0884594126264075, 0.630947429230293, 0.417437550928703, -0.137079112702816}, {0.155153213695468, 0.715024382827070, 0.792307389026170, -0.478945649467747, 0.228417764423633, -0.177412504176897, 0.188952169898914, -0.654033863744905, 0.0656385631644541, 0.754948568392513}};
        double[] b1 = {-2.17315689320286, -1.47531638787391, -1.18210566037997, -0.633156814990143, -0.438722967999051, 0.0532050261053854, -0.757306249290975, 1.21295381688562, -1.53581335342885, -2.09895137578510};
        double[] b2 = {1.86835536353871, 1.63779178862928, -0.973456855064988, 0.542843461734170, -0.275027672915230, 0.210896261704376, 0.607100463509418, 1.03426220120071, -1.34708039139892, 1.82316548848533};
        double[] b3 = {-1.78254372251441, 1.37088631717161, -0.883124221709888, 0.629080052016918, 0.218187254573360, 0.328147016090574, 0.634634127154178, -0.856021974499694, -1.41846786120425, 1.82438609667646};
        double[] b4 = {-0.256954073145964, -0.609364033648089};
        double[][] Ly1;
        double[][] Ly11;
        double[][] Ly2;
        double[][] Ly22;
        double[][] Ly3;
        double[][] Ly33;
        double[][] out;
        double[] y_xoffset = {80.25, 37};
        double[] y_gain = {0.0160804020100503, 0.0272572402044293};
        int y_ymin = -1;
        double[][] temp;
        for (int i = 0; i < x[0].length; i++) {
            temp = new double[x.length][1];
            for (int j = 0; j < x.length; j++) {
                temp[j][0] = x[j][i];
            }
            Ly1 = multiplyMatrix(IW, temp);
            for (int j = 0; j < b1.length; j++) {
                Ly1[j][0] = Ly1[j][0] + b1[j];
            }
            Ly11 = tansig_apply(Ly1);
            Ly2 = multiplyMatrix(LW1, Ly11);
            for (int j = 0; j < b2.length; j++) {
                Ly2[j][0] = Ly2[j][0] + b2[j];
            }
            Ly22 = tansig_apply(Ly2);
            Ly3 = multiplyMatrix(LW2, Ly22);
            for (int j = 0; j < b3.length; j++) {
                Ly3[j][0] = Ly3[j][0] + b3[j];
            }
            Ly33 = tansig_apply(Ly3);
            out = multiplyMatrix(LW3, Ly33);
            for (int j = 0; j < t.length; j++) {
                out[j][0] = out[j][0] + b4[j];
                out[j][0] = out[j][0] - y_ymin;
                out[j][0] = out[j][0] / y_gain[j];
                out[j][0] = out[j][0] + y_xoffset[j];
            }
            for (int j = 0; j < t.length; j++) {
                Y[j][i] = out[j][0];
            }
        }
        double sum;
        double[] bp = new double[Y.length];
        for (int i = 0; i < Y.length; i++) {
            sum = 0;
            for (int j = 0; j < Y[0].length; j++) {
                sum += Y[i][j];
            }
            bp[i] = sum / Y[0].length;
        }
//        Log.d("ML Done", "Now");
        return bp;
    }

    private static double[][] tansig_apply(double[][] arr) {
        double[][] a = new double[arr.length][1];
        for (int i = 0; i < arr.length; i++) {
            a[i][0] = 2 / (1 + (Math.pow(2.783, -2 * arr[i][0]))) - 1;
        }
        return a;
    }

    private static double[][] multiplyMatrix(double[][] A, double[][] B) {
        int i, j, k;
        int row1 = A.length, col1 = A[0].length, row2 = B.length, col2 = B[0].length;
        double[][] C = new double[row1][col2];
        if (row2 != col1) {
            return C;
        }
        for (i = 0; i < row1; i++) {
            for (j = 0; j < col2; j++) {
                for (k = 0; k < row2; k++)
                    C[i][j] += A[i][k] * B[k][j];
            }
        }
        return C;
    }

    public void draw_it(String[] strings) {
        //Log.d("Strings", strings[0]);
        String text = strings[0];
        concPPG.add(Float.parseFloat(strings[0]));
        concHRV.add(Float.parseFloat(strings[2]));
        str = Double.parseDouble(strings[0]);
        if (concPPG.peek() != null & mChart1 != null) {
            float dataPPG = concPPG.poll();
            time1 = addEntry(mChart1, dataPPG, time1);
            float dataHRV = concHRV.poll();
            time2 = addEntry(mChart2, dataHRV, time2);
        }
        dispSpO2.setText(strings[1]);
//        dispSpO2.setText("98");
        dispbpm.setText(strings[2]);
//        dispbpm.setText("78.47");
//        disptemp.setText("98.4");
        try {
            disptemp.setText(String.format(Locale.getDefault(), "%.1f", Double.parseDouble(strings[3])));
        } catch (NumberFormatException ignored) {
        }
        if (Double.parseDouble(strings[2]) > 20) {
            obj[countii] = str * 0.1;
            obj1[arrCount] = str;
            countii++;
            arrCount++;
            countiii++;
        }
        if (countii == numbersToSend - 1) {
            System.arraycopy(obj, 1, obj, 0, obj.length - 1);
            countii = countii - 1;
        }
        if (arrCount == bp_number - 1) {
            System.arraycopy(obj1, 1, obj1, 0, obj1.length - 1);
            arrCount = arrCount - 1;
        }
        if (countiii >= bp_number && !asyncflag2) {
            new MyAsyncTask1().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, obj1);
        }
        if (countiii >= numbersToSend && !asyncflag1) {
            new MyAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, obj);
        }
    }

    public class MyAsyncTask extends AsyncTask<double[], Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(double[]... params) {
            //Log.d("Entered: ", "Respiration Rate");
            asyncflag1 = true;
            wait2 += 1;
            if (wait2 >= 50) {
                wait2 = 0;
                dispRR.setText(String.format(Locale.getDefault(), "%d", (RespirationRate(params[0]))));
//                dispRR.setText("19");
            }
            asyncflag1 = false;
            return null;
        }
    }

    private class MyAsyncTask1 extends AsyncTask<double[], Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(double[]... params) {
            asyncflag2 = true;
            //Log.d("Entered: ", "Blood Pressure");

            double[] bp = ML(params[0]);
            //int RR = RespirationRate(params[0]);
            wait1 += 1;
            if (wait1 >= 50) {
//                dispRR.setText(Integer.toString(w.RR));
                wait1 = 0;
                dispBP.setText(String.format(Locale.getDefault(), "%d", ((int) ((int) bp[0] + 15))));  //+10*1.25
//                dispBP.setText("124");
                dispBP2.setText(String.format(Locale.getDefault(), "%d", ((int) ((int) bp[1] + 25))));  //+10*1.2
//                dispBP2.setText("86");
            }
            asyncflag2 = false;
            //w.RR = RR;
            return null;
        }
    }

    private class MyAsyncTask2 extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String[] strings) {
            //Log.d("Tokens", Arrays.toString(strings));
             //asyncflag3 = true;
            //draw_it(strings);
            flaggg = false;
            break_it(strings[0]);
            flaggg = true;
            //asyncflag3 = false;
            return null;
        }
    }

    private void break_it(String msg) {
        String[] tokens = msg.split(":");
        if (tokens.length > 5) {
            int count = 0;
            for (int i = 0; i < msg.length(); i++) {
                if (msg.charAt(i) == '#') {
                    count++;
                }
            }
            String[] complete_msg = new String[count];
            int k = 0;
            for (int i = 0; i < complete_msg.length; i++) {
                while (k < msg.length()) {
                    if (msg.charAt(k) == '#') {
                        complete_msg[i] = complete_msg[i] + msg.charAt(k);
                        k++;
                        break;
                    }
                    if (complete_msg[i] == null) {
                        complete_msg[i] = String.valueOf(msg.charAt(k));
                    } else {
                        complete_msg[i] = complete_msg[i] + msg.charAt(k);
                    }
                    k++;
                }
                String[] tokens1 = complete_msg[i].split(":");
                if (tokens1.length == 5 && !tokens1[0].isEmpty() && tokens1[0].charAt(0) != '#') {
                    if (tokens1[0].charAt(tokens1[0].indexOf(tokens1[0].trim())) == '$') {
                        tokens1[0] = tokens1[0].substring(tokens1[0].indexOf(tokens1[0].trim()) + 1);
                        //new MyAsyncTask2().execute( tokens1);
//                        runOnUiThread(() -> draw_it(tokens1));
                        queue.add(tokens1);

                    }
                }
            }
        }
        if (tokens.length == 5 && !tokens[0].isEmpty() && tokens[0].charAt(0) != '#') {
            if (tokens[0].charAt(tokens[0].indexOf(tokens[0].trim())) == '$') {
                tokens[0] = tokens[0].substring(tokens[0].indexOf(tokens[0].trim()) + 1);
//                //new MyAsyncTask2().execute(tokens);
//                runOnUiThread(() -> draw_it(tokens));
                queue.add(tokens);
            }
        }
    }
}