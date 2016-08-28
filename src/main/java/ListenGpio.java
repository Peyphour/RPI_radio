import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import okhttp3.*;

import java.io.IOException;

public class ListenGpio {

    private static void executeCommand(String command) {


        OkHttpClient client = new OkHttpClient();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        RequestBody body = RequestBody.create(JSON, command);
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

    }

    public static void main(String args[]) throws InterruptedException {

        // create gpio controller
        final GpioController gpio = GpioFactory.getInstance();

        final GpioPinDigitalInput playButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_DOWN);
        final GpioPinDigitalInput pauseButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, PinPullResistance.PULL_DOWN);
        final GpioPinDigitalInput nextButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);
        final GpioPinDigitalInput previousButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_03, PinPullResistance.PULL_DOWN);

        final String PLAY = "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"core.playback.play\"}";
        final String PAUSE = "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"core.playback.pause\"}";
        final String NEXT = "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"core.playback.next\"}";
        final String PREVIOUS = "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"core.playback.previous\"}";

        // set shutdown state for this input pin
        playButton.setShutdownOptions(true);
        pauseButton.setShutdownOptions(true);
        nextButton.setShutdownOptions(true);
        previousButton.setShutdownOptions(true);

        playButton.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    executeCommand(PLAY);
                }
            }
        });

        pauseButton.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    executeCommand(PAUSE);
                }
            }
        });

        nextButton.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    executeCommand(NEXT);
                }
            }
        });

        previousButton.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    executeCommand(PREVIOUS);
                }
            }
        });


        while (true) {
            Thread.sleep(500);
        }

    }
}
