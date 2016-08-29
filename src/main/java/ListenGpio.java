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

    final GpioPinDigitalInput station1 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_DOWN);
    final GpioPinDigitalInput station2 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, PinPullResistance.PULL_DOWN);
    final GpioPinDigitalInput station3 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);
    final GpioPinDigitalInput station4 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_03, PinPullResistance.PULL_DOWN);

    private int requestId = 1;

    private JSONObject stopClearBody, stopBody, clearBody, addBody, playBody;
    private JSONArray params;

    public String[] stationsUri;

    public void setup() {

        station1.setShutdownOptions(true);
        station2.setShutdownOptions(true);
        station3.setShutdownOptions(true);
        station4.setShutdownOptions(true);

        station1.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    changeStation(stationsUri[0]);
                }
            }
        });

        station2.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    changeStation(stationsUri[1]);
                }
            }
        });

        station3.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    changeStation(stationsUri[2]);
                }
            }
        });

        station4.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    changeStation(stationsUri[3]);
                }
            }
        });

        buildRequestBody();
        checkLastStation();
    }

    private void checkLastStation() {
        if(station1.isHigh())
            changeStation(stationsUri[0]);
        if(station2.isHigh())
            changeStation(stationsUri[1]);
        if(station3.isHigh())
            changeStation(stationsUri[2]);
        if(station4.isHigh())
            changeStation(stationsUri[3]);
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

        RequestBody body = RequestBody.create(JSON, content);
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
    }

    private void changeStation(String station) {

        JSONObject currentRequest;

        // Sequence is :
        // stopClear, stop, clear, add, play

        currentRequest = (JSONObject) stopClearBody.clone();
        currentRequest.put("id", requestId);

        this.sendRequest(currentRequest.toJSONString());

        currentRequest = (JSONObject) stopBody.clone();
        currentRequest.put("id", requestId);

        this.sendRequest(currentRequest.toJSONString());

        currentRequest = (JSONObject) clearBody.clone();
        currentRequest.put("id", requestId);

        this.sendRequest(currentRequest.toJSONString());

        currentRequest = (JSONObject) addBody.clone();
        currentRequest.put("id", requestId);

        params = new JSONArray();
        params.add(null);
        params.add(null);
        params.add(station);

        currentRequest.put("params", params);

        this.sendRequest(currentRequest.toJSONString());

        currentRequest = (JSONObject) playBody.clone();
        currentRequest.put("id", requestId);

        this.sendRequest(currentRequest.toJSONString());
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

        while (true) {
            Thread.sleep(500);
        }
    }
}
