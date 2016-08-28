import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import okhttp3.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

public class ListenGpio {

    // create gpio controller
    final GpioController gpio = GpioFactory.getInstance();

    final GpioPinDigitalInput playButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_DOWN);
    final GpioPinDigitalInput pauseButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, PinPullResistance.PULL_DOWN);
    final GpioPinDigitalInput nextButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);
    final GpioPinDigitalInput previousButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_03, PinPullResistance.PULL_DOWN);

    private int requestId = 1;

    private JSONObject stopClearBody, stopBody, clearBody, addBody, playBody;
    private JSONArray params;

    public String[] stationsUri;

    public void setup() {

        playButton.setShutdownOptions(true);
        pauseButton.setShutdownOptions(true);
        nextButton.setShutdownOptions(true);
        previousButton.setShutdownOptions(true);

        playButton.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    sendRequest(stationsUri[0]);
                }
            }
        });

        pauseButton.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    sendRequest(stationsUri[1]);
                }
            }
        });

        nextButton.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    sendRequest(stationsUri[2]);
                }
            }
        });

        previousButton.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    sendRequest(stationsUri[3]);
                }
            }
        });

        buildRequestBody();
    }

    private void buildRequestBody() {

        stopBody = new JSONObject();
        stopBody.put("jsonrpc", "2.0");

        stopClearBody = (JSONObject) stopBody.clone();
        addBody = (JSONObject) stopBody.clone();
        clearBody = (JSONObject) stopBody.clone();
        playBody = (JSONObject) stopBody.clone();

        params = new JSONArray();
        params.add(true);


        stopBody.put("method", "core.playback.stop");

        stopClearBody.put("method", "core.playback.stop");
        stopClearBody.put("params", params);

        addBody.put("method", "core.tracklist.add");

        clearBody.put("method", "core.tracklist.clear");

        playBody.put("method", "core.playback.play");

    }

    private void sendRequest(String content) {


        OkHttpClient client = new OkHttpClient();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JSONObject currentRequest;

        // Sequence is :
        // stopClear, stop, clear, add, play

        currentRequest = (JSONObject) stopClearBody.clone();
        currentRequest.put("id", requestId);

        System.out.println(currentRequest.toJSONString());

        RequestBody body = RequestBody.create(JSON, currentRequest.toJSONString());
        Request request = new Request.Builder()
                .url("http://localhost:6680/mopidy/rpc")
                .post(body)
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.requestId++;

        currentRequest = (JSONObject) stopBody.clone();
        currentRequest.put("id", requestId);

        body = RequestBody.create(JSON, currentRequest.toJSONString());
        request = new Request.Builder()
                .url("http://localhost:6680/mopidy/rpc")
                .post(body)
                .build();
        try {
            response = client.newCall(request).execute();
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.requestId++;

        currentRequest = (JSONObject) clearBody.clone();
        currentRequest.put("id", requestId);

        body = RequestBody.create(JSON, currentRequest.toJSONString());
        request = new Request.Builder()
                .url("http://localhost:6680/mopidy/rpc")
                .post(body)
                .build();
        try {
            response = client.newCall(request).execute();
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.requestId++;

        currentRequest = (JSONObject) addBody.clone();
        currentRequest.put("id", requestId);

        params = new JSONArray();
        params.add(null);
        params.add(null);
        params.add(content);

        currentRequest.put("params", params);

        System.out.println(currentRequest.toJSONString());

        body = RequestBody.create(JSON, currentRequest.toJSONString());
        request = new Request.Builder()
                .url("http://localhost:6680/mopidy/rpc")
                .post(body)
                .build();
        try {
            response = client.newCall(request).execute();
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.requestId++;

        currentRequest = (JSONObject) playBody.clone();
        currentRequest.put("id", requestId);

        body = RequestBody.create(JSON, currentRequest.toJSONString());
        request = new Request.Builder()
                .url("http://localhost:6680/mopidy/rpc")
                .post(body)
                .build();
        try {
            response = client.newCall(request).execute();
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readFile() {
        Properties prop = new Properties();
        try {
            InputStream input = new FileInputStream("/home/mopidy/radios.properties");
            prop.load(input);
            this.stationsUri = (String[]) Arrays.asList(
                    prop.getProperty("station1"),
                    prop.getProperty("station2"),
                    prop.getProperty("station3"),
                    prop.getProperty("station4")
            ).toArray();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws InterruptedException {

        ListenGpio gpio = new ListenGpio();

        gpio.readFile();
        gpio.setup();

        while(true) {
            Thread.sleep(500);
        }
    }
}
